"""
Unit tests for job_manager module.
Tests job queue, semaphore, and status tracking.
"""
import pytest
from unittest.mock import Mock, patch
import asyncio
import sys
import os

# Add parent to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from job_manager import JobManager, JobStatus, TranscriptionJob


class TestJobStatus:
    """Test JobStatus enum."""
    
    def test_queued_status(self):
        """Test QUEUED status exists."""
        assert JobStatus.QUEUED == "queued"
    
    def test_processing_status(self):
        """Test PROCESSING status exists."""
        assert JobStatus.PROCESSING == "processing"
    
    def test_completed_status(self):
        """Test COMPLETED status exists."""
        assert JobStatus.COMPLETED == "completed"
    
    def test_failed_status(self):
        """Test FAILED status exists."""
        assert JobStatus.FAILED == "failed"
    
    def test_timeout_status(self):
        """Test TIMEOUT status exists."""
        assert JobStatus.TIMEOUT == "timeout"


class TestTranscriptionJob:
    """Test TranscriptionJob dataclass."""
    
    def test_creation(self):
        """Test creating a job."""
        job = TranscriptionJob(
            job_id="test-123",
            correlation_id="corr-456",
            filename="test.mp3",
            file_size=1024000
        )
        assert job.job_id == "test-123"
        assert job.filename == "test.mp3"
        assert job.status == JobStatus.QUEUED
    
    def test_to_dict(self):
        """Test converting job to dictionary."""
        job = TranscriptionJob(
            job_id="test-123",
            correlation_id="corr-456",
            filename="test.mp3",
            file_size=1024000
        )
        d = job.to_dict()
        
        assert d["job_id"] == "test-123"
        assert d["filename"] == "test.mp3"
        assert d["status"] == "queued"
        assert "created_at" in d


class TestJobManager:
    """Test JobManager class."""
    
    def test_initialization(self):
        """Test job manager initializes correctly."""
        manager = JobManager()
        assert manager is not None
        assert manager.max_concurrent > 0
    
    def test_initialization_with_custom_concurrency(self):
        """Test job manager with custom concurrency setting."""
        manager = JobManager(max_concurrent=5)
        assert manager.max_concurrent == 5
    
    def test_get_stats_structure(self):
        """Test get_stats returns expected structure."""
        manager = JobManager()
        stats = manager.get_stats()
        
        assert "max_concurrent" in stats
        assert "available_slots" in stats
        assert "total_jobs" in stats
        assert "jobs_by_status" in stats
    
    def test_semaphore_exists(self):
        """Test semaphore is created."""
        manager = JobManager()
        assert hasattr(manager, 'semaphore')


@pytest.mark.asyncio
class TestJobManagerAsync:
    """Async tests for JobManager."""
    
    async def test_create_job(self):
        """Test creating a job asynchronously."""
        manager = JobManager()
        job = await manager.create_job(
            filename="test.mp3",
            file_size=1024000,
            correlation_id="corr-123"
        )
        
        assert job is not None
        assert job.filename == "test.mp3"
        assert job.status == JobStatus.QUEUED
    
    async def test_get_job(self):
        """Test getting a job by ID."""
        manager = JobManager()
        created_job = await manager.create_job(
            filename="test.mp3",
            file_size=1024000,
            correlation_id="corr-123"
        )
        
        retrieved_job = await manager.get_job(created_job.job_id)
        
        assert retrieved_job is not None
        assert retrieved_job.job_id == created_job.job_id
    
    async def test_get_nonexistent_job(self):
        """Test getting a job that doesn't exist."""
        manager = JobManager()
        job = await manager.get_job("nonexistent-id")
        
        assert job is None
    
    async def test_update_job_status(self):
        """Test updating job status."""
        manager = JobManager()
        job = await manager.create_job(
            filename="test.mp3",
            file_size=1024000,
            correlation_id="corr-123"
        )
        
        await manager.update_job_status(job.job_id, JobStatus.PROCESSING)
        
        updated_job = await manager.get_job(job.job_id)
        assert updated_job.status == JobStatus.PROCESSING
        assert updated_job.started_at is not None
    
    async def test_complete_job(self):
        """Test completing a job with result."""
        manager = JobManager()
        job = await manager.create_job(
            filename="test.mp3",
            file_size=1024000,
            correlation_id="corr-123"
        )
        
        result = {"text": "Hello world", "duration": 5.0}
        await manager.update_job_status(
            job.job_id,
            JobStatus.COMPLETED,
            result=result
        )
        
        completed_job = await manager.get_job(job.job_id)
        assert completed_job.status == JobStatus.COMPLETED
        assert completed_job.result == result
        assert completed_job.completed_at is not None
    
    async def test_fail_job(self):
        """Test failing a job with error."""
        manager = JobManager()
        job = await manager.create_job(
            filename="test.mp3",
            file_size=1024000,
            correlation_id="corr-123"
        )
        
        await manager.update_job_status(
            job.job_id,
            JobStatus.FAILED,
            error="Transcription failed"
        )
        
        failed_job = await manager.get_job(job.job_id)
        assert failed_job.status == JobStatus.FAILED
        assert failed_job.error == "Transcription failed"
    
    async def test_acquire_and_release_slot(self):
        """Test acquiring and releasing processing slots."""
        manager = JobManager(max_concurrent=2)
        
        initial_slots = manager.get_stats()["available_slots"]
        
        await manager.acquire_slot()
        after_acquire = manager.get_stats()["available_slots"]
        
        manager.release_slot()
        after_release = manager.get_stats()["available_slots"]
        
        assert after_acquire == initial_slots - 1
        assert after_release == initial_slots
