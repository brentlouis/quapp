"""The wait-time model (BACKEND.md phase 9; DECISIONS.md "Wait-time estimation learns online").

- features.py: what the model looks at, for a queue at one moment
- wait_model.py: the scaler and the SGDRegressor, saved to disk, learning one person at a time
- simulate.py: simulated queue days to train and test on (there's no real data yet)
- train.py: warm start from simulated days, saved where the server loads it
- evaluate.py: rolling average vs trained once vs learning online, predict-then-learn
"""
