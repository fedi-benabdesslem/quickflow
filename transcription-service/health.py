"""
Health check utilities for the transcription service.
"""
import logging
from typing import Optional
import torch

logger = logging.getLogger(__name__)


def get_gpu_memory_info() -> Optional[dict]:
    """Get GPU memory information if CUDA is available."""
    if not torch.cuda.is_available():
        return None
        
    try:
        device = torch.cuda.current_device()
        total = torch.cuda.get_device_properties(device).total_memory
        allocated = torch.cuda.memory_allocated(device)
        reserved = torch.cuda.memory_reserved(device)
        
        return {
            "total_mb": round(total / 1024 / 1024, 1),
            "allocated_mb": round(allocated / 1024 / 1024, 1),
            "reserved_mb": round(reserved / 1024 / 1024, 1),
            "free_mb": round((total - reserved) / 1024 / 1024, 1)
        }
    except Exception as e:
        logger.warning(f"Failed to get GPU memory info: {e}")
        return None


def build_health_response(
    transcriber,
    diarizer,
    job_manager
) -> dict:
    """Build a comprehensive health check response."""
    gpu_info = get_gpu_memory_info()
    job_stats = job_manager.get_stats()
    
    # Determine overall health
    whisper_ok = transcriber.is_loaded()
    diarizer_ok = diarizer.is_loaded()
    
    if whisper_ok and diarizer_ok:
        status = "healthy"
    elif whisper_ok or diarizer_ok:
        status = "degraded"
    else:
        status = "unhealthy"
    
    return {
        "status": status,
        "components": {
            "whisper": {
                "loaded": whisper_ok,
                **transcriber.get_model_info()
            },
            "diarization": {
                "loaded": diarizer_ok,
                **diarizer.get_pipeline_info()
            }
        },
        "gpu": {
            "available": torch.cuda.is_available(),
            "device_name": torch.cuda.get_device_name(0) if torch.cuda.is_available() else None,
            "memory": gpu_info
        },
        "jobs": job_stats
    }
