"""Compare the fixed source/native preview geometry inside the common clipped viewport."""
from pathlib import Path
import argparse
import json
import numpy as np
from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument("native", type=Path)
parser.add_argument("reference", type=Path)
args = parser.parse_args()
report = {}
for name in ("preview-full", "preview-layer"):
    native = np.asarray(Image.open(args.native / (name + ".png")).convert("RGB"))
    reference = np.asarray(Image.open(args.reference / (name + ".png")).convert("RGB"))
    assert native.shape == reference.shape == (600, 800, 3)
    # Excludes framework controls/export and the source renderer's out-of-viewport pixels.
    region = np.s_[60:132, 140:220]
    a, b = native[region], reference[region]
    mask_a = np.any(a != native[50, 100], axis=2)
    mask_b = np.any(b != reference[50, 100], axis=2)
    union = np.count_nonzero(mask_a | mask_b)
    iou = np.count_nonzero(mask_a & mask_b) / union
    error = np.abs(a.astype(float) - b).mean()
    assert iou >= 0.995, (name, "geometry changed", iou)
    assert error <= 4, (name, "lighting/material regression", error)
    report[name] = {"geometry_iou": iou, "mean_rgb_error": error, "compared_pixels": int(a.shape[0] * a.shape[1])}
print(json.dumps(report, indent=2))
