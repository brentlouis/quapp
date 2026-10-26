"""The wait-time model: predicts minutes per person for a queue right now (BACKEND.md step 9.2).

Two scikit-learn parts, saved together:
- StandardScaler: rescales each feature to "how many standard deviations from its average",
  so "people in line" (0–80) and "grace period" (0 or 1) count on the same scale.
- SGDRegressor: a linear model, minutes = w·features + b, fitted by stochastic gradient descent:
  after each example it nudges the weights a little in the direction that would have made that
  prediction better. That's why it can learn one served person at a time (partial_fit) instead
  of retraining on everything.

Why linear: its weights can be read out at the defense ("each extra person in line takes x
minutes off", "lunch adds y"), and it learns online. DECISIONS.md "Wait-time estimation learns
online" has the alternatives that were weighed.
"""

from pathlib import Path

import joblib
import numpy as np
from sklearn.linear_model import SGDRegressor
from sklearn.preprocessing import StandardScaler

from ml.features import FEATURE_NAMES

# Predictions outside this range are clamped: a model that has drifted shouldn't tell anyone to
# wait 0 minutes or 3 hours a person.
MIN_MINUTES = 0.5
MAX_MINUTES = 30.0


class WaitModel:
    def __init__(self):
        self.scaler = StandardScaler()
        # Squared error (the usual regression loss) and a little L2 regularization so no
        # weight runs away. The learning rate is small and fixed: 0.005 made a model learning
        # from scratch swing to 17-minute errors on single noisy people; 0.001 stays within
        # 2 minutes and is as accurate. Fixed (not shrinking) so it never stops adapting.
        # The comparison is in DECISIONS.md "The wait-time model, built".
        self.regressor = SGDRegressor(loss="squared_error", penalty="l2", alpha=1e-4,
                                      learning_rate="constant", eta0=0.001, random_state=0)
        # How many served people it has learned from (Insights: "learned from 1,240 served")
        self.samples = 0

    # ---- Training ---------------------------------------------------------------------

    def warm_start(self, rows: list[list[float]], minutes: list[float], epochs: int = 5) -> None:
        """Learns from a batch before going live (simulated days). Several passes over the
        shuffled batch, each with partial_fit, so it's the same learning rule as later."""
        x = np.asarray(rows, dtype=float)
        y = np.asarray(minutes, dtype=float)
        self.scaler.partial_fit(x)
        rng = np.random.default_rng(0)
        for _ in range(epochs):
            order = rng.permutation(len(y))
            for start in range(0, len(y), 256):  # small batches: faster, same idea
                batch = order[start:start + 256]
                self.regressor.partial_fit(self.scaler.transform(x[batch]), y[batch])
        self.samples += len(y)

    def learn(self, row: list[float], minutes: float) -> None:
        """One served person: their real service time nudges the scaler and the weights."""
        x = np.asarray([row], dtype=float)
        self.scaler.partial_fit(x)
        self.regressor.partial_fit(self.scaler.transform(x), np.asarray([minutes]))
        self.samples += 1

    # ---- Using it ---------------------------------------------------------------------

    @property
    def ready(self) -> bool:
        """Untrained models can't predict (scikit-learn raises), so the server checks first."""
        return self.samples > 0

    def predict(self, row: list[float]) -> float:
        x = self.scaler.transform(np.asarray([row], dtype=float))
        minutes = float(self.regressor.predict(x)[0])
        return min(max(minutes, MIN_MINUTES), MAX_MINUTES)

    def weights(self) -> dict[str, float]:
        """Each feature's weight, in minutes per standard deviation of that feature: the
        numbers to read out at the defense (evaluate.py prints them)."""
        return dict(zip(FEATURE_NAMES, (float(w) for w in self.regressor.coef_)))

    # ---- Saving -----------------------------------------------------------------------

    def save(self, path: Path) -> None:
        """Written to a temporary file first, then swapped in, so a crash mid-save can't leave a
        half-written model for the next start."""
        path.parent.mkdir(parents=True, exist_ok=True)
        tmp = path.with_suffix(".tmp")
        joblib.dump(self, tmp)
        tmp.replace(path)

    @staticmethod
    def load(path: Path) -> "WaitModel":
        """The saved model, or a fresh untrained one if there's no file yet."""
        if path.exists():
            return joblib.load(path)
        return WaitModel()
