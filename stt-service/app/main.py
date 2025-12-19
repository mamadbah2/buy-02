from fastapi import FastAPI
from py_eureka_client import eureka_client
from contextlib import asynccontextmanager
from app.api import endpoints
from app.core.model_loader import stt_model
from app.core.config import settings
import logging

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Load model on startup
    logger.info("Starting up STT Service...")
    # 1. Démarrage : On s'enregistre auprès d'Eureka (le serveur Java)
    await eureka_client.init_async(
        eureka_server="http://localhost:8761/eureka", # L'adresse de votre serveur Eureka Java
        app_name="STT-SERVICE",                       # Le nom que Spring utilisera pour l'appeler
        instance_port=8000                            # Le port de ce service Python
    )
    stt_model.load_model()
    yield
    # Clean up resources if needed
    logger.info("Shutting down STT Service...")
    await eureka_client.stop_async()

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    lifespan=lifespan
)

app.include_router(endpoints.router, prefix="/api/stt", tags=["transcription"])

@app.get("/health")
async def health_check():
    return {"status": "healthy", "model_loaded": stt_model.initialized}
