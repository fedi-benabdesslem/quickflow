"""
Prometheus metrics for the transcription service.
"""
from prometheus_client import Counter, Histogram, Gauge, Info

# Service info
SERVICE_INFO = Info(
    'transcription_service',
    'Transcription service information'
)

# Request metrics
TRANSCRIPTION_REQUESTS = Counter(
    'transcription_requests_total',
    'Total number of transcription requests',
    ['status']  # success, failure, timeout
)

# Duration metrics
TRANSCRIPTION_DURATION = Histogram(
    'transcription_duration_seconds',
    'Time spent on transcription',
    buckets=[30, 60, 120, 300, 600, 900, 1200, 1800]  # 30s to 30min
)

DIARIZATION_DURATION = Histogram(
    'diarization_duration_seconds',
    'Time spent on speaker diarization',
    buckets=[10, 30, 60, 120, 300, 600]
)

# File metrics  
FILE_SIZE_BYTES = Histogram(
    'transcription_file_size_bytes',
    'Size of uploaded audio files in bytes',
    buckets=[1e6, 5e6, 10e6, 50e6, 100e6, 200e6, 350e6]  # 1MB to 350MB
)

AUDIO_DURATION_SECONDS = Histogram(
    'transcription_audio_duration_seconds',
    'Duration of processed audio files',
    buckets=[60, 300, 600, 1800, 3600, 5400, 7200]  # 1min to 2hours
)

# Speaker metrics
SPEAKERS_DETECTED = Histogram(
    'transcription_speakers_detected',
    'Number of speakers detected in audio',
    buckets=[1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
)

# Active job gauge
ACTIVE_JOBS = Gauge(
    'transcription_active_jobs',
    'Number of currently active transcription jobs'
)

# Queue metrics
QUEUED_JOBS = Gauge(
    'transcription_queued_jobs',
    'Number of jobs waiting in queue'
)


def init_service_info(model_name: str, device: str):
    """Initialize service info metric."""
    SERVICE_INFO.info({
        'whisper_model': model_name,
        'device': device
    })
