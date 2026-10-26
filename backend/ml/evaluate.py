"""Is the model better than the rolling average? (BACKEND.md step 9.4)

Prequential evaluation, also called "predict, then learn": go through held-out days in time
order; at each served person, every method first predicts how long they'll take, the error is
recorded, and only then does the online model learn the true answer. Nothing is ever judged on
data it already learned from.

Compared:
- rolling average: the last 5 service times (what the server used before phase 9)
- trained once: the warm-started model, never updated
- learning online: the warm-started model, learning after every person (what the server does)
- as deployed: the rolling average until a queue has served 10 people today, then the online
  model (the server's actual rule, app/services/estimator.py)

Plus a learning curve: a model that starts knowing nothing and learns online over all the days,
against the rolling average, to show the error falling as it learns.

    .venv\\Scripts\\python -m ml.evaluate

Writes ml/results/evaluation.json and ml/results/learning_curve.svg for the report.
"""

import copy
import json
from pathlib import Path

from ml.features import to_row
from ml.simulate import simulate
from ml.train import WARM_START_DAYS
from ml.wait_model import MAX_MINUTES, MIN_MINUTES, WaitModel

RESULTS = Path(__file__).parent / "results"
TEST_DAYS = 14
MIN_SERVED_FOR_MODEL = 10   # the server's rule (estimator.MODEL_AFTER_SERVED)


def clamp(minutes: float) -> float:
    return min(max(minutes, MIN_MINUTES), MAX_MINUTES)


def evaluate(seed: int = 7) -> dict:
    events = simulate(WARM_START_DAYS + TEST_DAYS, seed=seed)
    train = [e for e in events if e.day < WARM_START_DAYS]
    test = [e for e in events if e.day >= WARM_START_DAYS]

    warm = WaitModel()
    warm.warm_start([to_row(e.snapshot) for e in train], [e.minutes for e in train])
    online = copy.deepcopy(warm)

    errors = {"rolling_average": 0.0, "trained_once": 0.0, "learning_online": 0.0, "as_deployed": 0.0}
    served_today: dict[tuple[int, str], int] = {}
    for e in test:
        row = to_row(e.snapshot)
        key = (e.day, e.queue)
        seen = served_today.get(key, 0)
        guesses = {
            "rolling_average": clamp(e.snapshot.rolling_average),
            "trained_once": warm.predict(row),
            "learning_online": online.predict(row),
        }
        guesses["as_deployed"] = (guesses["learning_online"] if seen >= MIN_SERVED_FOR_MODEL
                                  else guesses["rolling_average"])
        for name, guess in guesses.items():
            errors[name] += abs(guess - e.minutes)
        online.learn(row, e.minutes)          # only now does it see the answer
        served_today[key] = seen + 1

    mae = {name: total / len(test) for name, total in errors.items()}
    return {
        "train_days": WARM_START_DAYS, "test_days": TEST_DAYS,
        "train_events": len(train), "test_events": len(test),
        "mean_absolute_error_minutes": {k: round(v, 3) for k, v in mae.items()},
        "improvement_over_rolling_average_percent": {
            k: round(100 * (mae["rolling_average"] - v) / mae["rolling_average"], 1)
            for k, v in mae.items() if k != "rolling_average"},
        "weights_after_test": {k: round(v, 3) for k, v in online.weights().items()},
        "learning_curve": learning_curve(events),
    }


def learning_curve(events, window: int = 1000) -> list[dict]:
    """A model that starts from nothing, learning online: its error and the rolling average's,
    averaged over every `window` served people."""
    model = WaitModel()
    points, model_err, avg_err, n = [], 0.0, 0.0, 0
    for e in events:
        row = to_row(e.snapshot)
        guess = model.predict(row) if model.ready else clamp(e.snapshot.rolling_average)
        model_err += abs(guess - e.minutes)
        avg_err += abs(clamp(e.snapshot.rolling_average) - e.minutes)
        model.learn(row, e.minutes)
        n += 1
        if n % window == 0:
            points.append({"served": n, "model": round(model_err / window, 3),
                           "rolling_average": round(avg_err / window, 3)})
            model_err = avg_err = 0.0
    return points


def svg_chart(points: list[dict]) -> str:
    """The learning curve as a small SVG (no plotting library needed)."""
    w, h, left, bottom, top, right = 640, 360, 56, 40, 24, 16
    xs = [p["served"] for p in points]
    ys = [p["model"] for p in points] + [p["rolling_average"] for p in points]
    y_max = max(ys) * 1.1
    def sx(v): return left + (v - xs[0]) / (xs[-1] - xs[0]) * (w - left - right)
    def sy(v): return h - bottom - v / y_max * (h - bottom - top)
    def line(key, colour):
        d = " ".join(f"{'M' if i == 0 else 'L'}{sx(p['served']):.1f},{sy(p[key]):.1f}"
                     for i, p in enumerate(points))
        return f'<path d="{d}" fill="none" stroke="{colour}" stroke-width="2"/>'
    ticks = "".join(
        f'<line x1="{left}" x2="{w - right}" y1="{sy(v):.1f}" y2="{sy(v):.1f}" stroke="#e5e5e5"/>'
        f'<text x="{left - 8}" y="{sy(v) + 4:.1f}" font-size="11" text-anchor="end">{v:g}</text>'
        for v in [round(y_max * i / 4, 1) for i in range(5)])
    return (f'<svg xmlns="http://www.w3.org/2000/svg" width="{w}" height="{h}" '
            f'font-family="sans-serif" fill="#222">'
            f'<rect width="100%" height="100%" fill="white"/>{ticks}'
            f'{line("rolling_average", "#8c8474")}{line("model", "#0B4F4A")}'
            f'<text x="{left}" y="16" font-size="13">Mean error per person (minutes), '
            f'every {points[0]["served"]:,} served</text>'
            f'<text x="{w - right}" y="{h - 10}" font-size="11" text-anchor="end">'
            f'served people seen ({xs[-1]:,})</text>'
            f'<text x="{w - right - 4}" y="{sy(points[-1]["model"]) - 8:.1f}" font-size="12" '
            f'text-anchor="end" fill="#0B4F4A">learning model</text>'
            f'<text x="{w - right - 4}" y="{sy(points[-1]["rolling_average"]) - 8:.1f}" '
            f'font-size="12" text-anchor="end" fill="#8c8474">rolling average</text></svg>')


if __name__ == "__main__":
    result = evaluate()
    RESULTS.mkdir(exist_ok=True)
    (RESULTS / "evaluation.json").write_text(json.dumps(result, indent=2))
    (RESULTS / "learning_curve.svg").write_text(svg_chart(result["learning_curve"]))
    print(f"Trained on {result['train_days']} days ({result['train_events']} served), "
          f"tested on {result['test_days']} more ({result['test_events']} served)")
    print("Mean error per person, minutes (lower is better):")
    for name, mae in result["mean_absolute_error_minutes"].items():
        better = result["improvement_over_rolling_average_percent"].get(name)
        print(f"  {name:<16} {mae:5.2f}" + (f"   {better:+.1f}% vs rolling average" if better is not None else ""))
    print(f"Wrote {RESULTS / 'evaluation.json'} and learning_curve.svg")
