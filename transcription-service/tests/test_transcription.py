"""
Unit tests for transcription module.
Tests WhisperTranscriber class without loading actual models.
"""
import pytest
from unittest.mock import Mock, patch, MagicMock
from pathlib import Path
import sys
import os

# Add parent to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from transcription import TranscriptionSegment, TranscriptionResult, WhisperTranscriber


class TestTranscriptionSegment:
    """Test TranscriptionSegment dataclass."""
    
    def test_creation(self):
        """Test creating a segment."""
        seg = TranscriptionSegment(start=0.0, end=2.5, text="Hello world")
        assert seg.start == 0.0
        assert seg.end == 2.5
        assert seg.text == "Hello world"
    
    def test_segment_duration(self):
        """Test calculating segment duration."""
        seg = TranscriptionSegment(start=1.0, end=3.5, text="Test")
        duration = seg.end - seg.start
        assert duration == 2.5


class TestTranscriptionResult:
    """Test TranscriptionResult dataclass."""
    
    def test_creation(self):
        """Test creating a result."""
        segments = [
            TranscriptionSegment(start=0.0, end=2.0, text="Hello"),
            TranscriptionSegment(start=2.0, end=4.0, text="World"),
        ]
        result = TranscriptionResult(
            segments=segments,
            language="en",
            duration=4.0,
            text="Hello World"
        )
        assert len(result.segments) == 2
        assert result.language == "en"
        assert result.duration == 4.0
        assert result.text == "Hello World"


class TestWhisperTranscriber:
    """Test WhisperTranscriber class."""
    
    def test_initialization(self):
        """Test transcriber initializes without model loaded."""
        transcriber = WhisperTranscriber()
        assert transcriber.model is None
        assert transcriber.is_loaded() is False
    
    def test_is_loaded_false_initially(self):
        """Test is_loaded returns False before loading."""
        transcriber = WhisperTranscriber()
        assert transcriber.is_loaded() is False
    
    @patch('transcription.whisper')
    @patch('transcription.torch')
    def test_load_model_calls_whisper(self, mock_torch, mock_whisper):
        """Test load_model calls whisper.load_model."""
        mock_torch.cuda.is_available.return_value = True
        mock_whisper.load_model.return_value = Mock()
        
        transcriber = WhisperTranscriber()
        transcriber.load_model()
        
        mock_whisper.load_model.assert_called_once()
        assert transcriber.is_loaded() is True
    
    @patch('transcription.whisper')
    @patch('transcription.torch')
    def test_load_model_fallback_to_cpu(self, mock_torch, mock_whisper):
        """Test model falls back to CPU if CUDA unavailable."""
        mock_torch.cuda.is_available.return_value = False
        mock_whisper.load_model.return_value = Mock()
        
        transcriber = WhisperTranscriber()
        transcriber.device = "cuda"
        transcriber.load_model()
        
        assert transcriber.device == "cpu"
    
    def test_transcribe_raises_if_model_not_loaded(self):
        """Test transcribe raises RuntimeError if model not loaded."""
        transcriber = WhisperTranscriber()
        
        with pytest.raises(RuntimeError, match="model not loaded"):
            transcriber.transcribe("nonexistent.wav")
    
    @patch('transcription.whisper')
    @patch('transcription.torch')
    def test_transcribe_raises_if_file_not_found(self, mock_torch, mock_whisper):
        """Test transcribe raises FileNotFoundError for missing file."""
        mock_torch.cuda.is_available.return_value = False
        mock_whisper.load_model.return_value = Mock()
        
        transcriber = WhisperTranscriber()
        transcriber.load_model()
        
        with pytest.raises(FileNotFoundError):
            transcriber.transcribe("/nonexistent/path/audio.wav")
    
    def test_get_model_info_structure(self):
        """Test get_model_info returns expected structure."""
        transcriber = WhisperTranscriber()
        info = transcriber.get_model_info()
        
        assert "model_name" in info
        assert "device" in info
        assert "is_loaded" in info
        assert "cuda_available" in info


class TestTranscriberIntegration:
    """Integration-style tests (mock heavy components)."""
    
    @patch('transcription.whisper')
    @patch('transcription.torch')
    def test_full_transcription_flow(self, mock_torch, mock_whisper, tmp_path):
        """Test full transcription flow with mocked Whisper."""
        # Setup mocks
        mock_torch.cuda.is_available.return_value = False
        mock_model = Mock()
        mock_model.transcribe.return_value = {
            "segments": [
                {"start": 0.0, "end": 2.0, "text": " Hello world "}
            ],
            "language": "en",
            "text": " Hello world "
        }
        mock_whisper.load_model.return_value = mock_model
        mock_whisper.load_audio.return_value = [0.0] * 32000  # 2 seconds at 16kHz
        mock_whisper.audio.SAMPLE_RATE = 16000
        
        # Create dummy audio file
        audio_file = tmp_path / "test.wav"
        audio_file.write_bytes(b"fake audio content")
        
        # Run transcription
        transcriber = WhisperTranscriber()
        transcriber.load_model()
        result = transcriber.transcribe(str(audio_file))
        
        # Verify result
        assert isinstance(result, TranscriptionResult)
        assert len(result.segments) == 1
        assert result.segments[0].text == "Hello world"
        assert result.language == "en"
