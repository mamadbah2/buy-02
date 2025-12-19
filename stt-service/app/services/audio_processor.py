import librosa
import numpy as np
from fastapi import HTTPException, UploadFile
from app.core.config import settings
import io
import soundfile as sf
import logging

logger = logging.getLogger(__name__)

class AudioProcessor:
    @staticmethod
    def process_audio(file: UploadFile) -> tuple[np.ndarray, float]:
        """
        Reads an audio file, resamples it to 16kHz, and returns the audio array and duration.
        """
        try:
            # Read file into memory
            content = file.file.read()
            
            # Load audio with librosa
            # librosa.load can read from a file-like object in newer versions, or we might need to save to temp
            # Using soundfile or io.BytesIO is safer for in-memory
            
            # Note: librosa.load accepts a file path or a file-like object (since 0.8.0)
            audio_data, _ = librosa.load(io.BytesIO(content), sr=settings.SAMPLE_RATE)
            
            duration = librosa.get_duration(y=audio_data, sr=settings.SAMPLE_RATE)
            
            return audio_data, duration
        except Exception as e:
            logger.error(f"Error processing audio file: {e}")
            raise HTTPException(status_code=400, detail=f"Invalid audio file: {str(e)}")
