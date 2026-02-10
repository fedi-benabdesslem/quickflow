# QuickFlow Transcription Service

A Python FastAPI microservice for audio transcription with speaker diarization using Whisper and pyannote.

## Features

- **Speech-to-Text**: OpenAI Whisper (medium model) for accurate transcription
- **Speaker Diarization**: pyannote.audio for identifying who spoke when
- **GPU Acceleration**: CUDA support for fast processing
- **Async Job Queue**: Semaphore-based concurrency control
- **Prometheus Metrics**: Full observability with metrics endpoint
- **Health Checks**: Detailed component status reporting

## Prerequisites

- Python 3.10+
- CUDA-capable GPU (recommended) with 8GB+ VRAM
- Hugging Face account with accepted pyannote model terms

## Setup

### 1. Create Virtual Environment

```bash
cd transcription-service
python -m venv venv
venv\Scripts\activate  # Windows
# source venv/bin/activate  # Linux/Mac
```

### 2. Install Dependencies

```bash
pip install -r requirements.txt
```

### 3. Install PyTorch with CUDA (if using GPU)

```bash
pip install torch torchaudio --index-url https://download.pytorch.org/whl/cu121
```

### 4. Configure Environment

Create a `.env` file or set environment variables:

```env
# Required for pyannote
HUGGINGFACE_TOKEN=hf_your_token_here

# Optional overrides
WHISPER_MODEL=medium
WHISPER_DEVICE=cuda
MAX_CONCURRENT_JOBS=2
SERVICE_PORT=8001
```

> **Note**: Get your Hugging Face token at https://huggingface.co/settings/tokens
> You must also accept the pyannote model terms at https://huggingface.co/pyannote/speaker-diarization-3.1

### 5. Run the Service

```bash
python main.py
```

Or with uvicorn:

```bash
uvicorn main:app --host 0.0.0.0 --port 8001 --reload
```

## API Endpoints

| Method | Endpoint | Description |
|:--|:--|:--|
| `POST` | `/transcribe` | Upload audio file for transcription |
| `GET` | `/status/{job_id}` | Get job status |
| `GET` | `/result/{job_id}` | Get transcription result |
| `GET` | `/health` | Health check |
| `GET` | `/metrics` | Prometheus metrics |

## Usage Example

```bash
# Upload audio file
curl -X POST "http://localhost:8001/transcribe" \
  -H "X-Correlation-ID: my-request-123" \
  -F "file=@meeting_recording.mp3"

# Response (immediate if slot available, or queued)
{
  "job_id": "abc-123",
  "status": "completed",
  "segments": [
    {"speaker": "SPEAKER_00", "start": 0.5, "end": 3.2, "text": "Hello everyone"},
    {"speaker": "SPEAKER_01", "start": 3.5, "end": 7.1, "text": "Thanks for joining"}
  ],
  "speakers": ["SPEAKER_00", "SPEAKER_01"],
  "duration": 120.5,
  "language": "en",
  "processing_time_seconds": 45.2
}
```

## Configuration Options

| Variable | Default | Description |
|:--|:--|:--|
| `WHISPER_MODEL` | `medium` | Whisper model: tiny, base, small, medium, large |
| `WHISPER_DEVICE` | `cuda` | Device: cuda or cpu |
| `MAX_CONCURRENT_JOBS` | `2` | Max simultaneous transcriptions |
| `MAX_FILE_SIZE_MB` | `350` | Maximum upload size |
| `MAX_DURATION_HOURS` | `2.0` | Maximum audio duration |
| `SERVICE_PORT` | `8001` | API port |

## Performance

| Audio Length | Processing Time (GPU) | Processing Time (CPU) |
|:--|:--|:--|
| 30 min | ~3-5 min | ~15-20 min |
| 1 hour | ~5-10 min | ~30-45 min |
| 2 hours | ~10-20 min | ~60-90 min |

*Times vary based on audio quality and number of speakers.*
