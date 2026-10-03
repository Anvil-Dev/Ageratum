"""Install the opt-in 1.21.1 capture harness into an existing isolated source checkout."""
from pathlib import Path
import argparse
import shutil

parser = argparse.ArgumentParser()
parser.add_argument("reference", type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
reference = args.reference.resolve()
if reference == root or not (reference / ".git").exists():
    raise SystemExit("Use an isolated 1.21.1 Ageratum Git checkout")
fixture = root / "src/clientTest/reference/StructureTest.java"
target = reference / "src/clientTest/java/dev/anvilcraft/resource/ageratum/test/StructureTest.java"
target.parent.mkdir(parents=True, exist_ok=True)
shutil.copyfile(fixture, target)
assets = root / "src/clientTest/resources/assets/ageratum/ageratum"
for source in assets.glob("*.snbt"):
    target = reference / "src/clientTest/resources/assets/ageratum/ageratum" / source.name
    target.parent.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, target)
print(reference)
