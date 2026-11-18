import os
from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI, HTTPException, Request
from pydantic import BaseModel, Field

from bde import BondStrengthPredictor, InvalidStructure

MODEL_DIR = Path(os.environ.get("MODEL_DIR", Path(__file__).parent / "model"))


@asynccontextmanager
async def lifespan(app: FastAPI):
    app.state.predictor = BondStrengthPredictor(MODEL_DIR)
    yield


app = FastAPI(title="Bond strength service", lifespan=lifespan)


class PredictRequest(BaseModel):
    molfile: str = Field(min_length=1, max_length=500_000)


@app.get("/health")
def health(request: Request):
    return {"status": "ok", "method": request.app.state.predictor.method()}


@app.post("/predict")
def predict(body: PredictRequest, request: Request):
    try:
        return request.app.state.predictor.analyse(body.molfile)
    except InvalidStructure as e:
        raise HTTPException(status_code=422, detail=str(e)) from e
