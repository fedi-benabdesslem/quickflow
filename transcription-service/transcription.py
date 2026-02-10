"""
Whisper-based audio transcription module.
Handles speech-to-text conversion with timestamp extraction.
"""
import logging
from pathlib import Path
from typing import Optional
import torch
import whisper
from dataclasses import dataclass

from config import WHISPER_MODEL, WHISPER_DEVICE, WHISPER_LANGUAGE

logger = logging.getLogger(__name__)


@dataclass
class TranscriptionSegment:
    """A segment of transcribed text with timing information."""
    start: float
    end: float
    text: str


@dataclass  
class TranscriptionResult:
    """Complete transcription result."""
    segments: list[TranscriptionSegment]
    language: str
    duration: float
    text: str  # Full concatenated text


class WhisperTranscriber:
    """Whisper-based transcription service."""
    
    def __init__(self):
        self.model: Optional[whisper.Whisper] = None
        self.device = WHISPER_DEVICE
        self.model_name = WHISPER_MODEL
        
    def load_model(self) -> None:
        """Load the Whisper model into memory."""
        if self.model is not None:
            logger.info("Whisper model already loaded")
            return
            
        logger.info(f"Loading Whisper model '{self.model_name}' on {self.device}")
        
        # Check CUDA availability
        if self.device == "cuda" and not torch.cuda.is_available():
            logger.warning("CUDA not available, falling back to CPU")
            self.device = "cpu"
        
        self.model = whisper.load_model(self.model_name, device=self.device)
        logger.info(f"Whisper model loaded successfully on {self.device}")
        
    def is_loaded(self) -> bool:
        """Check if the model is loaded."""
        return self.model is not None
    
    def transcribe(
        self, 
        audio_path: Path | str,
        language: Optional[str] = WHISPER_LANGUAGE
    ) -> TranscriptionResult:
        """
        Transcribe an audio file to text with timestamps.
        
        Args:
            audio_path: Path to the audio file
            language: Language code (e.g., 'en', 'fr') or None for auto-detect
            
        Returns:
            TranscriptionResult with segments and full text
        """
        if self.model is None:
            raise RuntimeError("Whisper model not loaded. Call load_model() first.")
        
        audio_path = Path(audio_path)
        if not audio_path.exists():
            raise FileNotFoundError(f"Audio file not found: {audio_path}")
        
        logger.info(f"Transcribing audio: {audio_path}")
        
        # Transcribe with word-level timestamps
        result = self.model.transcribe(
            str(audio_path),
            language=language,
            task="transcribe",
            verbose=False,
            word_timestamps=True
        )
        
        # Extract segments
        segments = [
            TranscriptionSegment(
                start=seg["start"],
                end=seg["end"],
                text=seg["text"].strip()
            )
            for seg in result["segments"]
        ]
        
        # Get audio duration
        audio = whisper.load_audio(str(audio_path))
        duration = len(audio) / whisper.audio.SAMPLE_RATE
        
        logger.info(f"Transcription complete: {len(segments)} segments, {duration:.1f}s duration")
        
        return TranscriptionResult(
            segments=segments,
            language=result["language"],
            duration=duration,
            text=result["text"].strip()
        )
    
    def get_model_info(self) -> dict:
        """Get information about the loaded model."""
        return {
            "model_name": self.model_name,
            "device": self.device,
            "is_loaded": self.is_loaded(),
            "cuda_available": torch.cuda.is_available(),
            "cuda_device_name": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None
        }


# Global instance
transcriber = WhisperTranscriber()
