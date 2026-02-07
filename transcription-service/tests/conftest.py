# Pytest configuration for transcription service tests
import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

# Pytest fixtures
import pytest
from pathlib import Path

@pytest.fixture
def temp_audio_path(tmp_path):
    """Create a temporary audio file path for testing."""
    return tmp_path / "test_audio.wav"

@pytest.fixture
def mock_transcript_data():
    """Sample transcript data for testing."""
    return {
        "segments": [
            {"start": 0.0, "end": 2.5, "text": "Hello, this is speaker one."},
            {"start": 2.5, "end": 5.0, "text": "And this is speaker two."},
        ],
        "language": "en",
        "text": "Hello, this is speaker one. And this is speaker two."
    }
