"""
Job management for concurrent transcription processing.
Uses semaphores and async queuing for GPU-aware processing.
"""
import asyncio
import logging
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Optional
from uuid import uuid4

from config import MAX_CONCURRENT_JOBS, JOB_TIMEOUT_SECONDS

logger = logging.getLogger(__name__)


class JobStatus(str, Enum):
    """Status of a transcription job."""
    QUEUED = "queued"
    PROCESSING = "processing"
    COMPLETED = "completed"
    FAILED = "failed"
    TIMEOUT = "timeout"


@dataclass
class TranscriptionJob:
    """Represents a transcription job."""
    job_id: str
    correlation_id: str
    filename: str
    file_size: int
    status: JobStatus = JobStatus.QUEUED
    progress: int = 0
    stage: str = "queued"
    created_at: datetime = field(default_factory=datetime.utcnow)
    started_at: Optional[datetime] = None
    completed_at: Optional[datetime] = None
    result: Optional[dict] = None
    error: Optional[str] = None
    
    def to_dict(self) -> dict:
        """Convert job to dictionary for API response."""
        return {
            "job_id": self.job_id,
            "correlation_id": self.correlation_id,
            "filename": self.filename,
            "file_size": self.file_size,
            "status": self.status.value,
            "progress": self.progress,
            "stage": self.stage,
            "created_at": self.created_at.isoformat(),
            "started_at": self.started_at.isoformat() if self.started_at else None,
            "completed_at": self.completed_at.isoformat() if self.completed_at else None,
            "error": self.error,
            "duration_seconds": self._get_duration()
        }
    
    def _get_duration(self) -> Optional[float]:
        """Calculate processing duration."""
        if self.started_at:
            end = self.completed_at or datetime.utcnow()
            return (end - self.started_at).total_seconds()
        return None


class JobManager:
    """Manages transcription jobs with semaphore-based concurrency control."""
    
    def __init__(self, max_concurrent: int = MAX_CONCURRENT_JOBS):
        self.max_concurrent = max_concurrent
        self.semaphore = asyncio.Semaphore(max_concurrent)
        self.jobs: dict[str, TranscriptionJob] = {}
        self._lock = asyncio.Lock()
        
    async def create_job(
        self, 
        filename: str, 
        file_size: int,
        correlation_id: str
    ) -> TranscriptionJob:
        """Create a new transcription job."""
        job_id = str(uuid4())
        job = TranscriptionJob(
            job_id=job_id,
            correlation_id=correlation_id,
            filename=filename,
            file_size=file_size
        )
        
        async with self._lock:
            self.jobs[job_id] = job
            
        logger.info(f"Created job {job_id} for {filename} (correlation: {correlation_id})")
        return job
    
    async def get_job(self, job_id: str) -> Optional[TranscriptionJob]:
        """Get a job by ID."""
        return self.jobs.get(job_id)
    
    async def update_job_status(
        self,
        job_id: str,
        status: JobStatus,
        result: Optional[dict] = None,
        error: Optional[str] = None
    ) -> None:
        """Update job status."""
        job = self.jobs.get(job_id)
        if not job:
            return
            
        async with self._lock:
            job.status = status
            
            if status == JobStatus.PROCESSING:
                job.started_at = datetime.utcnow()
            elif status in (JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.TIMEOUT):
                job.completed_at = datetime.utcnow()
                
            if result:
                job.result = result
            if error:
                job.error = error
                
        logger.info(f"Job {job_id} status updated to {status.value}")
    
    async def update_progress(
        self,
        job_id: str,
        progress: int,
        stage: str
    ) -> None:
        """Update job progress and stage."""
        job = self.jobs.get(job_id)
        if not job:
            return
            
        async with self._lock:
            job.progress = min(max(progress, 0), 100)
            job.stage = stage
                
        logger.debug(f"Job {job_id} progress: {progress}% ({stage})")
    
    async def acquire_slot(self) -> bool:
        """Acquire a processing slot (blocks until available)."""
        await self.semaphore.acquire()
        return True
    
    def release_slot(self) -> None:
        """Release a processing slot."""
        self.semaphore.release()
    
    def get_stats(self) -> dict:
        """Get job manager statistics."""
        statuses = {}
        for job in self.jobs.values():
            statuses[job.status.value] = statuses.get(job.status.value, 0) + 1
            
        return {
            "max_concurrent": self.max_concurrent,
            "available_slots": self.semaphore._value,
            "total_jobs": len(self.jobs),
            "jobs_by_status": statuses
        }
    
    async def cleanup_old_jobs(self, max_age_hours: int = 24) -> int:
        """Remove jobs older than max_age_hours."""
        cutoff = datetime.utcnow()
        removed = 0
        
        async with self._lock:
            to_remove = [
                job_id for job_id, job in self.jobs.items()
                if (cutoff - job.created_at).total_seconds() > max_age_hours * 3600
                and job.status in (JobStatus.COMPLETED, JobStatus.FAILED, JobStatus.TIMEOUT)
            ]
            
            for job_id in to_remove:
                del self.jobs[job_id]
                removed += 1
                
        if removed:
            logger.info(f"Cleaned up {removed} old jobs")
            
        return removed


# Global instance
job_manager = JobManager()
