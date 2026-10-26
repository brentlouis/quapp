"""Warm start: trains the model on simulated days and saves it where the server loads it
(BACKEND.md step 9.3). Run once before going live, and again whenever simulate.py changes:

    .venv\\Scripts\\python -m ml.train

The saved file (ml/wait_model_warm.joblib) is committed, so the hosted server starts with it.
The server copies it to ml/wait_model.joblib (not committed) and keeps learning there from every
served person (app/services/estimator.py).
"""

from pathlib import Path

from ml.features import to_row
from ml.simulate import simulate
from ml.wait_model import WaitModel

WARM_PATH = Path(__file__).parent / "wait_model_warm.joblib"
WARM_START_DAYS = 60


def train(days: int = WARM_START_DAYS, seed: int = 7) -> WaitModel:
    events = simulate(days, seed=seed)
    model = WaitModel()
    model.warm_start([to_row(e.snapshot) for e in events], [e.minutes for e in events])
    return model


if __name__ == "__main__":
    trained = train()
    trained.save(WARM_PATH)
    print(f"Warm-started on {trained.samples} simulated served people; saved to {WARM_PATH}")
    print("Largest weights (minutes per standard deviation):")
    for name, weight in sorted(trained.weights().items(), key=lambda kv: -abs(kv[1]))[:8]:
        print(f"  {name:<22} {weight:+.2f}")
