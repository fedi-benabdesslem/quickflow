# Transcription Service Configuration
import os
from pathlib import Path
from dotenv import load_dotenv

# Load .env file
load_dotenv()

# Service settings
SERVICE_HOST = os.getenv("SERVICE_HOST", "0.0.0.0")
SERVICE_PORT = int(os.getenv("SERVICE_PORT", "8001"))

# Whisper settings
WHISPER_MODEL = os.getenv("WHISPER_MODEL", "medium")
WHISPER_DEVICE = os.getenv("WHISPER_DEVICE", "cuda")  # or "cpu"
WHISPER_LANGUAGE = os.getenv("WHISPER_LANGUAGE") or "en"  # None/Empty for default "en", set to "None" string for auto-detect? Wait. Whisper takes None for auto-detect.
# If user wants auto-detect, they might set it to empty?
# But Whisper raises error on empty string.
# If user wants auto-detect, they should unset it or set "None"?
# Better: if empty, default to "en". If "auto", set to None.
WHISPER_LANGUAGE = os.getenv("WHISPER_LANGUAGE") or "en"

# Diarization settings  
HUGGINGFACE_TOKEN = os.getenv("HUGGINGFACE_TOKEN", "")  # Required for pyannote
MAX_SPEAKERS = int(os.getenv("MAX_SPEAKERS", "10"))
MIN_SPEAKERS = int(os.getenv("MIN_SPEAKERS", "1"))

# Processing settings
MAX_CONCURRENT_JOBS = int(os.getenv("MAX_CONCURRENT_JOBS", "2"))
MAX_FILE_SIZE_MB = int(os.getenv("MAX_FILE_SIZE_MB", "350"))
MAX_DURATION_HOURS = float(os.getenv("MAX_DURATION_HOURS", "2.0"))
JOB_TIMEOUT_SECONDS = int(os.getenv("JOB_TIMEOUT_SECONDS", "1800"))  # 30 minutes

# Storage settings
TEMP_DIR = Path(os.getenv("TEMP_DIR", "./temp"))
TEMP_DIR.mkdir(exist_ok=True)

# Logging
LOG_LEVEL = os.getenv("LOG_LEVEL", "INFO")
