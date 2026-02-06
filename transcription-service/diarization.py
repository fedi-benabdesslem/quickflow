"""
Speaker diarization module using pyannote.audio.
Identifies who spoke when in an audio recording.
"""
import logging
from pathlib import Path
from typing import Optional
from dataclasses import dataclass
import torch

from config import HUGGINGFACE_TOKEN, MAX_SPEAKERS, MIN_SPEAKERS, WHISPER_DEVICE

logger = logging.getLogger(__name__)


@dataclass
class SpeakerSegment:
    """A segment of audio attributed to a specific speaker."""
    speaker: str
    start: float
    end: float


@dataclass
class DiarizationResult:
    """Complete diarization result."""
    segments: list[SpeakerSegment]
    speakers: list[str]
    duration: float


class SpeakerDiarizer:
    """Pyannote-based speaker diarization service."""
    
    def __init__(self):
        self.pipeline = None
        
        # Determine device based on platform
        import platform
        system = platform.system()
        
        self.device = WHISPER_DEVICE
        
        # On Windows, force CPU for Diarization to verify stability (unless user really tweaked their environment)
        # We rely on the logs seen during debugging: Windows GPU = Deadlock.
        if system == "Windows" and self.device == "cuda":
            logger.warning("Windows detected: Forcing Diarization to CPU to prevent known deadlocks.")
            self.device = "cpu"
        elif self.device == "cuda" and not torch.cuda.is_available():
             logger.warning("CUDA requested but not available, falling back to CPU")
             self.device = "cpu"
        
        logger.info(f"Diarization initialized on device: {self.device}")
        
    def load_model(self) -> None:
        """Load the pyannote diarization pipeline."""
        import sys
        print("DEBUG: Checking if diarization pipeline is loaded...", file=sys.stderr)
        
        if self.pipeline is not None:
            logger.info("Diarization pipeline already loaded")
            print("DEBUG: Pipeline already loaded.", file=sys.stderr)
            return
            
        print(f"DEBUG: HUGGINGFACE_TOKEN present? {bool(HUGGINGFACE_TOKEN)}", file=sys.stderr)
        if HUGGINGFACE_TOKEN:
             print(f"DEBUG: Token starts with: {HUGGINGFACE_TOKEN[:4]}...", file=sys.stderr)

        if not HUGGINGFACE_TOKEN:
            raise ValueError(
                "HUGGINGFACE_TOKEN environment variable required for pyannote. "
                "Get your token at https://huggingface.co/settings/tokens"
            )
        
        logger.info("Loading pyannote speaker diarization pipeline...")
        print("DEBUG: Attempting to load pyannote pipeline from pretrained...", file=sys.stderr)
        
        try:
            from pyannote.audio import Pipeline
            print("DEBUG: Imported pyannote.audio.Pipeline successfully", file=sys.stderr)
            
            self.pipeline = Pipeline.from_pretrained(
                "pyannote/speaker-diarization-3.1",
                use_auth_token=HUGGINGFACE_TOKEN
            )
            print(f"DEBUG: Pipeline.from_pretrained returned: {self.pipeline}", file=sys.stderr)
            
            if self.pipeline is None:
                 print("ERROR: Pipeline.from_pretrained returned None! Check token permissions and model acceptance.", file=sys.stderr)
                 raise ValueError("Pipeline failed to load (returned None). Check HF token permissions.")

            # Move to GPU if available
            if self.device == "cuda":
                print("DEBUG: Moving pipeline to CUDA...", file=sys.stderr)
                self.pipeline.to(torch.device("cuda"))
                logger.info("Diarization pipeline loaded on CUDA")
            else:
                logger.info("Diarization pipeline loaded on CPU")
            
            print("DEBUG: Diarization pipeline loaded successfully!", file=sys.stderr)
                
        except Exception as e:
            logger.error(f"Failed to load diarization pipeline: {e}")
            print(f"ERROR: Exception in load_model: {e}", file=sys.stderr)
            import traceback
            traceback.print_exc()
            raise
    
    def is_loaded(self) -> bool:
        """Check if the pipeline is loaded."""
        return self.pipeline is not None
    
    def diarize(
        self,
        audio_path: Path | str,
        min_speakers: Optional[int] = MIN_SPEAKERS,
        max_speakers: Optional[int] = MAX_SPEAKERS
    ) -> DiarizationResult:
        """
        Perform speaker diarization on an audio file.
        
        Args:
            audio_path: Path to the audio file
            min_speakers: Minimum expected number of speakers
            max_speakers: Maximum expected number of speakers
            
        Returns:
            DiarizationResult with speaker segments
        """
        if self.pipeline is None:
            raise RuntimeError("Diarization pipeline not loaded. Call load_model() first.")
        
        audio_path = Path(audio_path)
        if not audio_path.exists():
            raise FileNotFoundError(f"Audio file not found: {audio_path}")
        
        logger.info(f"Diarizing audio: {audio_path}")
        
        # Run diarization
        diarization = self.pipeline(
            str(audio_path),
            min_speakers=min_speakers,
            max_speakers=max_speakers
        )
        
        # Extract segments
        segments = []
        speakers_set = set()
        duration = 0.0
        
        for turn, _, speaker in diarization.itertracks(yield_label=True):
            segments.append(SpeakerSegment(
                speaker=speaker,
                start=turn.start,
                end=turn.end
            ))
            speakers_set.add(speaker)
            duration = max(duration, turn.end)
        
        # Sort segments by start time
        segments.sort(key=lambda s: s.start)
        speakers = sorted(list(speakers_set))
        
        logger.info(f"Diarization complete: {len(speakers)} speakers, {len(segments)} segments")
        
        return DiarizationResult(
            segments=segments,
            speakers=speakers,
            duration=duration
        )
    
    def get_pipeline_info(self) -> dict:
        """Get information about the loaded pipeline."""
        return {
            "is_loaded": self.is_loaded(),
            "device": self.device,
            "cuda_available": torch.cuda.is_available(),
            "max_speakers": MAX_SPEAKERS
        }


# Global instance
diarizer = SpeakerDiarizer()
