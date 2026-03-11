#!/usr/bin/env python3
import sys, math, json, subprocess
from collections import Counter
from pathlib import Path

def shannon_entropy(text: str) -> float:
    if not text or len(text) < 10:
        return 0.0
    freq = Counter(text)
    probs = [count / len(text) for count in freq.values()]
    return -sum(p * math.log2(p) for p in probs if p > 0)

# Get changed files safely for pull_request events
changed_files = []
try:
    # GitHub provides github.event.pull_request.base.sha and head.sha in the context
    base_sha = subprocess.check_output(['git', 'rev-parse', 'origin/${{ github.base_ref }}'], text=True).strip()
    changed_files = subprocess.check_output(
        ['git', 'diff', '--name-only', base_sha, 'HEAD'], text=True
    ).splitlines()
except subprocess.CalledProcessError:
    # Fallback for first-time PRs or edge cases: use the merge-base or just files in HEAD
    try:
        changed_files = subprocess.check_output(
            ['git', 'diff', '--name-only', 'HEAD~1', 'HEAD'], text=True
        ).splitlines()
    except subprocess.CalledProcessError:
        # Last resort: all files in the repo
        changed_files = subprocess.check_output(['git', 'ls-files'], text=True).splitlines()

results = []
total_ent = 0.0
count = 0

for f in changed_files:
    path = Path(f.strip())
    if not path.exists() or path.suffix in {'.png', '.jpg', '.gif', '.bin', '.lock', '.exe', '.dll', '.so'}:
        continue
    try:
        content = path.read_text(encoding='utf-8', errors='ignore')
        ent = shannon_entropy(content)
        results.append(f"{f}: {ent:.3f}")
        total_ent += ent
        count += 1
    except Exception:
        pass

avg = round(total_ent / count, 3) if count > 0 else 0.0

verdict = (
    "✅ Mid-4 beauty detected (thoughtful human code!)" if 4.3 <= avg <= 4.7 else
    "⚠️ Consider review — entropy outside sweet spot" if avg > 0 else
    "No source files changed"
)

with open('/tmp/beauty.json', 'w') as f:
    json.dump({
        "average_entropy": avg,
        "verdict": verdict,
        "files": results[:20]
    }, f, indent=2)

print(f"Average entropy: {avg}")
print(verdict)
