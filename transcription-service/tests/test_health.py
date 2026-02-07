"""
Unit tests for health module.
Tests health check functionality.
"""
import pytest
from unittest.mock import Mock, patch
import sys
import os

# Add parent to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from health import get_gpu_memory_info, build_health_response


class TestGpuMemoryInfo:
    """Test GPU memory info function."""
    
    @patch('health.torch')
    def test_returns_none_when_cuda_unavailable(self, mock_torch):
        """Test returns None when CUDA is not available."""
        mock_torch.cuda.is_available.return_value = False
        
        result = get_gpu_memory_info()
        
        assert result is None
    
    @patch('health.torch')
    def test_returns_memory_info_when_cuda_available(self, mock_torch):
        """Test returns memory info when CUDA is available."""
        mock_torch.cuda.is_available.return_value = True
        mock_torch.cuda.current_device.return_value = 0
        
        # Mock device properties
        mock_props = Mock()
        mock_props.total_memory = 8 * 1024 * 1024 * 1024  # 8GB
        mock_torch.cuda.get_device_properties.return_value = mock_props
        mock_torch.cuda.memory_allocated.return_value = 2 * 1024 * 1024 * 1024  # 2GB
        mock_torch.cuda.memory_reserved.return_value = 3 * 1024 * 1024 * 1024  # 3GB
        
        result = get_gpu_memory_info()
        
        assert result is not None
        assert "total_mb" in result
        assert "allocated_mb" in result
        assert "reserved_mb" in result
        assert "free_mb" in result


class TestBuildHealthResponse:
    """Test health response builder."""
    
    def test_returns_healthy_when_all_loaded(self):
        """Test returns 'healthy' when all models loaded."""
        mock_transcriber = Mock()
        mock_transcriber.is_loaded.return_value = True
        mock_transcriber.get_model_info.return_value = {"model_name": "medium"}
        
        mock_diarizer = Mock()
        mock_diarizer.is_loaded.return_value = True
        mock_diarizer.get_pipeline_info.return_value = {"device": "cpu"}
        
        mock_job_manager = Mock()
        mock_job_manager.get_stats.return_value = {"active": 0, "completed": 5}
        
        with patch('health.torch') as mock_torch, \
             patch('health.get_gpu_memory_info') as mock_gpu:
            mock_torch.cuda.is_available.return_value = False
            mock_gpu.return_value = None
            
            result = build_health_response(mock_transcriber, mock_diarizer, mock_job_manager)
        
        assert result["status"] == "healthy"
        assert "components" in result
        assert "gpu" in result
        assert "jobs" in result
    
    def test_returns_degraded_when_one_loaded(self):
        """Test returns 'degraded' when only one model loaded."""
        mock_transcriber = Mock()
        mock_transcriber.is_loaded.return_value = True
        mock_transcriber.get_model_info.return_value = {}
        
        mock_diarizer = Mock()
        mock_diarizer.is_loaded.return_value = False
        mock_diarizer.get_pipeline_info.return_value = {}
        
        mock_job_manager = Mock()
        mock_job_manager.get_stats.return_value = {}
        
        with patch('health.torch') as mock_torch, \
             patch('health.get_gpu_memory_info') as mock_gpu:
            mock_torch.cuda.is_available.return_value = False
            mock_gpu.return_value = None
            
            result = build_health_response(mock_transcriber, mock_diarizer, mock_job_manager)
        
        assert result["status"] == "degraded"
    
    def test_returns_unhealthy_when_none_loaded(self):
        """Test returns 'unhealthy' when no models loaded."""
        mock_transcriber = Mock()
        mock_transcriber.is_loaded.return_value = False
        mock_transcriber.get_model_info.return_value = {}
        
        mock_diarizer = Mock()
        mock_diarizer.is_loaded.return_value = False
        mock_diarizer.get_pipeline_info.return_value = {}
        
        mock_job_manager = Mock()
        mock_job_manager.get_stats.return_value = {}
        
        with patch('health.torch') as mock_torch, \
             patch('health.get_gpu_memory_info') as mock_gpu:
            mock_torch.cuda.is_available.return_value = False
            mock_gpu.return_value = None
            
            result = build_health_response(mock_transcriber, mock_diarizer, mock_job_manager)
        
        assert result["status"] == "unhealthy"
    
    def test_includes_gpu_info(self):
        """Test includes GPU information in response."""
        mock_transcriber = Mock()
        mock_transcriber.is_loaded.return_value = True
        mock_transcriber.get_model_info.return_value = {}
        
        mock_diarizer = Mock()
        mock_diarizer.is_loaded.return_value = True
        mock_diarizer.get_pipeline_info.return_value = {}
        
        mock_job_manager = Mock()
        mock_job_manager.get_stats.return_value = {}
        
        with patch('health.torch') as mock_torch, \
             patch('health.get_gpu_memory_info') as mock_gpu:
            mock_torch.cuda.is_available.return_value = True
            mock_torch.cuda.get_device_name.return_value = "Test GPU"
            mock_gpu.return_value = {"total_mb": 8192}
            
            result = build_health_response(mock_transcriber, mock_diarizer, mock_job_manager)
        
        assert result["gpu"]["available"] is True
        assert result["gpu"]["device_name"] == "Test GPU"
