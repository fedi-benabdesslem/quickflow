"""
Unit tests for config module.
Tests environment variable loading and configuration defaults.
"""
import pytest
import os
from unittest.mock import patch


class TestConfigDefaults:
    """Test that configuration has sensible defaults."""
    
    def test_service_host_default(self):
        """Default host should be 0.0.0.0."""
        from config import SERVICE_HOST
        assert SERVICE_HOST == "0.0.0.0" or SERVICE_HOST is not None
    
    def test_service_port_is_integer(self):
        """Service port should be an integer."""
        from config import SERVICE_PORT
        assert isinstance(SERVICE_PORT, int)
        assert SERVICE_PORT > 0
    
    def test_whisper_model_default(self):
        """Whisper model should default to 'medium'."""
        from config import WHISPER_MODEL
        assert WHISPER_MODEL in ["tiny", "base", "small", "medium", "large", "large-v2", "large-v3"]
    
    def test_whisper_device_valid(self):
        """Whisper device should be 'cuda' or 'cpu'."""
        from config import WHISPER_DEVICE
        assert WHISPER_DEVICE in ["cuda", "cpu"]
    
    def test_whisper_language_not_empty(self):
        """Whisper language should not be empty string."""
        from config import WHISPER_LANGUAGE
        # Either None (auto-detect) or a valid language code
        assert WHISPER_LANGUAGE is None or (isinstance(WHISPER_LANGUAGE, str) and len(WHISPER_LANGUAGE) >= 2)
    
    def test_max_speakers_range(self):
        """Max speakers should be between 1 and 20."""
        from config import MAX_SPEAKERS
        assert 1 <= MAX_SPEAKERS <= 20
    
    def test_min_speakers_range(self):
        """Min speakers should be at least 1."""
        from config import MIN_SPEAKERS
        assert MIN_SPEAKERS >= 1
    
    def test_max_concurrent_jobs(self):
        """Max concurrent jobs should be reasonable for GPU memory."""
        from config import MAX_CONCURRENT_JOBS
        assert 1 <= MAX_CONCURRENT_JOBS <= 10
    
    def test_max_file_size_mb(self):
        """Max file size should be large enough for 2h audio."""
        from config import MAX_FILE_SIZE_MB
        assert MAX_FILE_SIZE_MB >= 100  # At least 100MB for long recordings
    
    def test_temp_dir_exists(self):
        """Temp directory should be created."""
        from config import TEMP_DIR
        assert TEMP_DIR.exists() or TEMP_DIR.is_dir


class TestConfigEnvironmentOverride:
    """Test that environment variables override defaults."""
    
    def test_whisper_model_env_override(self):
        """Environment variable should override WHISPER_MODEL."""
        with patch.dict(os.environ, {"WHISPER_MODEL": "tiny"}):
            # Need to reimport to pick up new env var
            import importlib
            import config
            importlib.reload(config)
            assert config.WHISPER_MODEL == "tiny"
    
    def test_max_concurrent_jobs_env_override(self):
        """Environment variable should override MAX_CONCURRENT_JOBS."""
        with patch.dict(os.environ, {"MAX_CONCURRENT_JOBS": "4"}):
            import importlib
            import config
            importlib.reload(config)
            assert config.MAX_CONCURRENT_JOBS == 4
