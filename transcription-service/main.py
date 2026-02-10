"""
FastAPI Transcription Service
Provides audio transcription with speaker diarization using Whisper and pyannote.
"""
import asyncio
import logging
import os
import shutil
import sys
import time
from contextlib import asynccontextmanager
from datetime import datetime
from pathlib import Path
from typing import Optional
from uuid import uuid4

from fastapi import FastAPI, File, Header, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from prometheus_client import make_asgi_app
from pydantic import BaseModel

from config import (
    SERVICE_HOST, SERVICE_PORT, LOG_LEVEL, TEMP_DIR,
    MAX_FILE_SIZE_MB, MAX_DURATION_HOURS, WHISPER_MODEL, WHISPER_DEVICE
)
from transcription import transcriber, TranscriptionResult
from diarization import diarizer, DiarizationResult
from job_manager import job_manager, JobStatus
from health import build_health_response
from metrics import (
    TRANSCRIPTION_REQUESTS, TRANSCRIPTION_DURATION, DIARIZATION_DURATION,
    FILE_SIZE_BYTES, AUDIO_DURATION_SECONDS, SPEAKERS_DETECTED,
    ACTIVE_JOBS, init_service_info
)

# Configure logging
logging.basicConfig(
    level=getattr(logging, LOG_LEVEL),
    format='%(asctime)s [%(levelname)s] %(name)s (%(correlation_id)s): %(message)s',
    datefmt='%Y-%m-%d %H:%M:%S'
)
logger = logging.getLogger(__name__)

# STARTUP DEBUG
import sys
import numpy
print(f"DEBUG: Python executable: {sys.executable}", file=sys.stderr)
print(f"DEBUG: Numpy version: {numpy.__version__}", file=sys.stderr)
print(f"DEBUG: Pandas version: {str(getattr(sys.modules.get('pandas'), '__version__', 'not loaded'))}", file=sys.stderr)
try:
    print(f"DEBUG: np.NaN exists? {numpy.NaN}", file=sys.stderr)
except Exception as e:
    print(f"DEBUG: np.NaN check failed: {e}", file=sys.stderr)
# END STARTUP DEBUG

# Add correlation ID filter
class CorrelationIdFilter(logging.Filter):
    def filter(self, record):
        if not hasattr(record, 'correlation_id'):
            record.correlation_id = '-'
        return True

for handler in logging.root.handlers:
    handler.addFilter(CorrelationIdFilter())


# Supported audio formats
SUPPORTED_FORMATS = {'.mp3', '.wav', '.m4a', '.flac', '.ogg', '.wma', '.aac', '.webm'}


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan - load models on startup."""
    logger.info("Starting transcription service...")
    
    # Load models
    try:
        transcriber.load_model()
        init_service_info(WHISPER_MODEL, WHISPER_DEVICE)
    except Exception as e:
        logger.error(f"Failed to load Whisper model: {e}")
        
    try:
        diarizer.load_model()
    except Exception as e:
        logger.error(f"Failed to load diarization pipeline: {e}")
        import sys
        import traceback
        print(f"ERROR: Failed to load diarization pipeline: {e}", file=sys.stderr)
        traceback.print_exc()
    
    logger.info("Transcription service ready")
    yield
    
    # Cleanup on shutdown
    logger.info("Shutting down transcription service...")
    # Clean temp files
    for file in TEMP_DIR.glob("*"):
        try:
            file.unlink()
        except Exception:
            pass


# Create FastAPI app
app = FastAPI(
    title="QuickFlow Transcription Service",
    description="Audio transcription with speaker diarization",
    version="1.0.0",
    lifespan=lifespan
)

# CORS middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173", "http://localhost:8080"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount Prometheus metrics
metrics_app = make_asgi_app()
app.mount("/metrics", metrics_app)


# Response models
class TranscriptSegmentResponse(BaseModel):
    speaker: str
    start: float
    end: float
    text: str


class TranscriptionResponse(BaseModel):
    job_id: str
    status: str
    segments: Optional[list[TranscriptSegmentResponse]] = None
    speakers: Optional[list[str]] = None
    duration: Optional[float] = None
    language: Optional[str] = None
    full_text: Optional[str] = None
    processing_time_seconds: Optional[float] = None
    error: Optional[str] = None


class JobStatusResponse(BaseModel):
    job_id: str
    status: str
    filename: str
    created_at: str
    started_at: Optional[str] = None
    completed_at: Optional[str] = None
    duration_seconds: Optional[float] = None
    error: Optional[str] = None
    result: Optional[dict] = None


# Helper functions
def get_correlation_id(header: Optional[str]) -> str:
    """Get or generate correlation ID."""
    return header or str(uuid4())


def merge_transcription_and_diarization(
    transcription: TranscriptionResult,
    diarization: DiarizationResult
) -> list[dict]:
    """
    Merge transcription segments with speaker information.
    Assigns each transcription segment to the speaker with maximum temporal overlap.
    """
    merged_segments = []
    
    for trans_seg in transcription.segments:
        # Find the speaker with maximum overlap for this segment
        best_speaker = "UNKNOWN"
        best_overlap = 0
        
        for diar_seg in diarization.segments:
            # Calculate overlap
            overlap_start = max(trans_seg.start, diar_seg.start)
            overlap_end = min(trans_seg.end, diar_seg.end)
            overlap = max(0, overlap_end - overlap_start)
            
            if overlap > best_overlap:
                best_overlap = overlap
                best_speaker = diar_seg.speaker
        
        merged_segments.append({
            "speaker": best_speaker,
            "start": trans_seg.start,
            "end": trans_seg.end,
            "text": trans_seg.text
        })
    
    # Merge consecutive segments from same speaker
    if not merged_segments:
        return merged_segments
        
    final_segments = [merged_segments[0]]
    for seg in merged_segments[1:]:
        last = final_segments[-1]
        # Merge if same speaker and close in time (within 2 seconds)
        if seg["speaker"] == last["speaker"] and seg["start"] - last["end"] < 2.0:
            last["end"] = seg["end"]
            last["text"] = last["text"] + " " + seg["text"]
        else:
            final_segments.append(seg)
    
    return final_segments


async def process_transcription(
    job_id: str,
    audio_path: Path,
    correlation_id: str
) -> dict:
    """Process transcription and diarization for a job."""
    extra = {"correlation_id": correlation_id}
    
    try:
        # Update job status
        await job_manager.update_job_status(job_id, JobStatus.PROCESSING)
        ACTIVE_JOBS.inc()
        
        start_time = time.time()
        
        # Run transcription
        logger.info(f"Starting transcription for job {job_id}", extra=extra)
        trans_start = time.time()
        transcription = transcriber.transcribe(audio_path)
        trans_duration = time.time() - trans_start
        TRANSCRIPTION_DURATION.observe(trans_duration)
        
        # Record audio duration
        AUDIO_DURATION_SECONDS.observe(transcription.duration)
        
        # Check duration limit
        max_seconds = MAX_DURATION_HOURS * 3600
        if transcription.duration > max_seconds:
            raise ValueError(f"Audio duration ({transcription.duration/60:.1f} min) exceeds limit ({MAX_DURATION_HOURS} hours)")
        
        # Convert to WAV for safe Diarization (fixes torchaudio MP3 issues on Windows)
        # We use strict 16kHz mono PCM which is ideal for both Whisper and Pyannote
        wav_path = audio_path.with_suffix(".wav")
        logger.info(f"Converting audio to WAV: {wav_path}", extra=extra)
        print(f"DEBUG: Job {job_id} - Converting to WAV...", file=sys.stderr)
        
        try:
            import subprocess
            # ffmpeg -i input -ar 16000 -ac 1 -c:a pcm_s16le output.wav
            subprocess.run([
                "ffmpeg", "-y",
                "-i", str(audio_path),
                "-ar", "16000",
                "-ac", "1",
                "-c:a", "pcm_s16le",
                str(wav_path)
            ], check=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
            print(f"DEBUG: Job {job_id} - Conversion successful.", file=sys.stderr)
            
            # Use WAV for diarization (and future steps)
            # Note: Whisper already ran on MP3, which is fine as it uses ffmpeg internally.
            # But Diarization needs WAV to avoid torchaudio issues.
            diarization_audio_path = wav_path
            
        except Exception as e:
            logger.error(f"Failed to convert to WAV: {e}", extra=extra)
            print(f"DEBUG: Job {job_id} - WAV conversion failed: {e}", file=sys.stderr)
            # Do NOT fallback to MP3 on Windows, it causes infinite loops/hangs in torchaudio
            raise RuntimeError(f"Audio conversion failed: {e}. Please ensure ffmpeg is installed and working.")
            # diarization_audio_path = audio_path
        
        # Run diarization

        logger.info(f"Starting diarization for job {job_id}", extra=extra)
        print(f"DEBUG: Job {job_id} - Calling diarizer.diarize...", file=sys.stderr)
        diar_start = time.time()
        diarization = diarizer.diarize(diarization_audio_path)
        diar_duration = time.time() - diar_start
        print(f"DEBUG: Job {job_id} - Diarization finished in {diar_duration:.2f}s", file=sys.stderr)
        DIARIZATION_DURATION.observe(diar_duration)
        
        # Record speaker count
        print(f"DEBUG: Job {job_id} - Recording metrics...", file=sys.stderr)
        SPEAKERS_DETECTED.observe(len(diarization.speakers))
        
        # Merge results
        print(f"DEBUG: Job {job_id} - Merging results...", file=sys.stderr)
        segments = merge_transcription_and_diarization(transcription, diarization)
        print(f"DEBUG: Job {job_id} - Merge complete. {len(segments)} segments.", file=sys.stderr)
        
        total_time = time.time() - start_time
        
        result = {
            "segments": segments,
            "speakers": diarization.speakers,
            "duration": transcription.duration,
            "language": transcription.language,
            "full_text": transcription.text,
            "processing_time_seconds": round(total_time, 2)
        }
        
        # Update job with result
        print(f"DEBUG: Job {job_id} - Updating job status to COMPLETED...", file=sys.stderr)
        await job_manager.update_job_status(job_id, JobStatus.COMPLETED, result=result)
        print(f"DEBUG: Job {job_id} - Job status updated.", file=sys.stderr)
        TRANSCRIPTION_REQUESTS.labels(status="success").inc()
        
        logger.info(
            f"Job {job_id} completed in {total_time:.1f}s "
            f"({len(diarization.speakers)} speakers, {len(segments)} segments)",
            extra=extra
        )
        
        print(f"DEBUG: Job {job_id} - Returning result...", file=sys.stderr)
        return result
        
    except Exception as e:
        logger.error(f"Job {job_id} failed: {e}", extra=extra)
        await job_manager.update_job_status(job_id, JobStatus.FAILED, error=str(e))
        TRANSCRIPTION_REQUESTS.labels(status="failure").inc()
        raise
        
    finally:
        ACTIVE_JOBS.dec()
        job_manager.release_slot()
        
        # Cleanup temp files
        if audio_path.exists():
            try:
                audio_path.unlink()
            except Exception:
                pass
        
        # Cleanup WAV if different
        if 'diarization_audio_path' in locals() and diarization_audio_path != audio_path and diarization_audio_path.exists():
            try:
                diarization_audio_path.unlink()
            except Exception:
                pass


# API Endpoints
@app.get("/health")
async def health_check():
    """Health check endpoint."""
    return build_health_response(transcriber, diarizer, job_manager)


@app.post("/transcribe", response_model=TranscriptionResponse)
async def transcribe_audio(
    file: UploadFile = File(...),
    x_correlation_id: Optional[str] = Header(None)
):
    """
    Transcribe an audio file with speaker diarization.
    
    Returns immediately with job_id for polling, or waits if queue is empty.
    """
    correlation_id = get_correlation_id(x_correlation_id)
    extra = {"correlation_id": correlation_id}
    
    logger.info(f"Received transcription request: {file.filename}", extra=extra)
    
    # Validate file
    if not file.filename:
        raise HTTPException(400, "No filename provided")
    
    ext = Path(file.filename).suffix.lower()
    if ext not in SUPPORTED_FORMATS:
        raise HTTPException(
            400, 
            f"Unsupported format '{ext}'. Supported: {', '.join(SUPPORTED_FORMATS)}"
        )
    
    # Check models are loaded
    if not transcriber.is_loaded():
        raise HTTPException(503, "Whisper model not loaded")
    if not diarizer.is_loaded():
        raise HTTPException(503, "Diarization pipeline not loaded")
    
    # Save file temporarily
    temp_path = TEMP_DIR / f"{uuid4()}{ext}"
    try:
        # Read file and check size
        content = await file.read()
        file_size = len(content)
        
        if file_size > MAX_FILE_SIZE_MB * 1024 * 1024:
            raise HTTPException(
                400,
                f"File size ({file_size / 1024 / 1024:.1f}MB) exceeds limit ({MAX_FILE_SIZE_MB}MB)"
            )
        
        FILE_SIZE_BYTES.observe(file_size)
        
        with open(temp_path, "wb") as f:
            f.write(content)
            
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Failed to save uploaded file: {e}", extra=extra)
        raise HTTPException(500, "Failed to process upload")
    
    # Create job
    job = await job_manager.create_job(
        filename=file.filename,
        file_size=file_size,
        correlation_id=correlation_id
    )
    
    # Try to acquire processing slot
    try:
        # Wait up to 5 seconds for a slot, otherwise return queued status
        await asyncio.wait_for(job_manager.acquire_slot(), timeout=5.0)
    except asyncio.TimeoutError:
        # Job is queued, return immediately
        logger.info(f"Job {job.job_id} queued (no slots available)", extra=extra)
        return TranscriptionResponse(
            job_id=job.job_id,
            status="queued"
        )
    
    # Process synchronously since we got a slot
    try:
        result = await process_transcription(job.job_id, temp_path, correlation_id)
        
        return TranscriptionResponse(
            job_id=job.job_id,
            status="completed",
            segments=[TranscriptSegmentResponse(**s) for s in result["segments"]],
            speakers=result["speakers"],
            duration=result["duration"],
            language=result["language"],
            full_text=result["full_text"],
            processing_time_seconds=result["processing_time_seconds"]
        )
        
    except Exception as e:
        return TranscriptionResponse(
            job_id=job.job_id,
            status="failed",
            error=str(e)
        )


@app.get("/status/{job_id}", response_model=JobStatusResponse)
async def get_job_status(job_id: str):
    """Get the status of a transcription job."""
    job = await job_manager.get_job(job_id)
    
    if not job:
        raise HTTPException(404, f"Job {job_id} not found")
    
    return JobStatusResponse(**job.to_dict())


@app.get("/result/{job_id}", response_model=TranscriptionResponse)
async def get_job_result(job_id: str):
    """Get the result of a completed transcription job."""
    job = await job_manager.get_job(job_id)
    
    if not job:
        raise HTTPException(404, f"Job {job_id} not found")
    
    if job.status == JobStatus.QUEUED:
        return TranscriptionResponse(job_id=job_id, status="queued")
    
    if job.status == JobStatus.PROCESSING:
        return TranscriptionResponse(job_id=job_id, status="processing")
    
    if job.status == JobStatus.FAILED:
        return TranscriptionResponse(
            job_id=job_id,
            status="failed",
            error=job.error
        )
    
    if job.status == JobStatus.COMPLETED and job.result:
        return TranscriptionResponse(
            job_id=job_id,
            status="completed",
            segments=[TranscriptSegmentResponse(**s) for s in job.result["segments"]],
            speakers=job.result["speakers"],
            duration=job.result["duration"],
            language=job.result["language"],
            full_text=job.result["full_text"],
            processing_time_seconds=job.result["processing_time_seconds"]
        )
    
    raise HTTPException(500, "Unexpected job state")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host=SERVICE_HOST, port=SERVICE_PORT)
