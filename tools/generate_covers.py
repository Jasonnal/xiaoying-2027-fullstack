#!/usr/bin/env python3
"""生成模板封面（纯 SVG，无外部依赖）。

封面是随包发布的示例素材，不依赖任何图床。每张封面用不同的配色与几何构成，
避免列表页看起来像同一张图换了颜色。
"""
from __future__ import annotations

import math
import pathlib
import random

OUT_DIR = pathlib.Path(__file__).resolve().parents[1] / "backend/src/main/resources/demo/covers"

W, H = 1200, 800

# 每套配色：[背景起, 背景止, 主色, 辅色, 点缀色]
PALETTES = {
    "travel-postcard": ["#0E1B26", "#123243", "#2FD6C3", "#F2B04C", "#F7F3E8"],
    "retro-film": ["#1A1210", "#2C1B12", "#E8A65B", "#C6643B", "#F4E7D3"],
    "neon-portrait": ["#120B24", "#241043", "#8B5CF6", "#22D3EE", "#F472B6"],
    "spring-festival": ["#2A0A0E", "#4A1016", "#F25C54", "#F6C453", "#FFE9C9"],
    "product-hero": ["#0B1220", "#131C2E", "#3B82F6", "#22D3A8", "#E2E8F0"],
    "grid-collage": ["#101014", "#1B1B22", "#F5A524", "#EC4899", "#E7E7EA"],
    "story-cover": ["#0D1A16", "#123027", "#34D399", "#A7F3D0", "#F0FDF4"],
    "brand-banner": ["#141018", "#241A2B", "#A78BFA", "#F97316", "#F5F3FF"],
}


def blob(cx: float, cy: float, r: float, fill: str, opacity: float) -> str:
    return f'<circle cx="{cx:.0f}" cy="{cy:.0f}" r="{r:.0f}" fill="{fill}" opacity="{opacity}"/>'


def arcs(colors: list[str], seed: int) -> str:
    rnd = random.Random(seed)
    parts = []
    cx, cy = rnd.randint(700, 1000), rnd.randint(200, 600)
    for i in range(5):
        r = 120 + i * 78
        parts.append(
            f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="none" stroke="{colors[2 if i % 2 else 3]}" '
            f'stroke-width="2" opacity="{0.34 - i * 0.05:.2f}"/>'
        )
    return "".join(parts)


def stripes(colors: list[str], seed: int) -> str:
    rnd = random.Random(seed)
    parts = [f'<g opacity="0.5">']
    for i in range(11):
        x = -200 + i * 150
        parts.append(
            f'<rect x="{x}" y="-120" width="26" height="1100" fill="{colors[2] if i % 2 else colors[3]}" '
            f'opacity="{rnd.uniform(0.05, 0.16):.2f}" transform="rotate(18 0 0)"/>'
        )
    parts.append("</g>")
    return "".join(parts)


def mesh(colors: list[str], seed: int) -> str:
    rnd = random.Random(seed)
    parts = []
    for _ in range(7):
        parts.append(
            blob(rnd.randint(-100, W + 100), rnd.randint(-80, H + 80), rnd.randint(90, 320),
                 rnd.choice([colors[2], colors[3], colors[4]]), rnd.uniform(0.05, 0.16))
        )
    return "".join(parts)


def grid(colors: list[str]) -> str:
    parts = ['<g opacity="0.18" stroke="%s" stroke-width="1">' % colors[4]]
    for x in range(0, W + 1, 80):
        parts.append(f'<line x1="{x}" y1="0" x2="{x}" y2="{H}"/>')
    for y in range(0, H + 1, 80):
        parts.append(f'<line x1="0" y1="{y}" x2="{W}" y2="{y}"/>')
    parts.append("</g>")
    return "".join(parts)


def ring(colors: list[str]) -> str:
    return (
        f'<circle cx="880" cy="560" r="250" fill="none" stroke="{colors[2]}" stroke-width="54" opacity="0.5"/>'
        f'<circle cx="880" cy="560" r="150" fill="{colors[3]}" opacity="0.28"/>'
        f'<rect x="150" y="180" width="320" height="320" rx="48" fill="{colors[3]}" opacity="0.22" '
        f'transform="rotate(-14 310 340)"/>'
    )


def compose(name: str, palette: list[str], motif: str, seed: int) -> str:
    c = palette
    body = []
    body.append(f'<rect width="{W}" height="{H}" fill="url(#bg-{name})"/>')

    if motif == "arcs":
        body.append(arcs(c, seed))
    elif motif == "stripes":
        body.append(stripes(c, seed))
    elif motif == "mesh":
        body.append(mesh(c, seed))
    elif motif == "grid":
        body.append(grid(c))
    elif motif == "ring":
        body.append(ring(c))

    # 统一叠一层柔光与暗角，让不同几何构图之间保持同一套视觉语言
    body.append(f'<ellipse cx="380" cy="180" rx="520" ry="360" fill="{c[2]}" opacity="0.10"/>')
    body.append(
        f'<linearGradient id="shade-{name}" x1="0" y1="1" x2="0" y2="0">'
        f'<stop offset="0" stop-color="#05070A" stop-opacity="0.85"/>'
        f'<stop offset="0.6" stop-color="#05070A" stop-opacity="0"/>'
        f"</linearGradient>"
    )
    body.append(f'<rect width="{W}" height="{H}" fill="url(#shade-{name})"/>')

    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" '
        f'role="img" aria-label="{name}">'
        f'<defs><linearGradient id="bg-{name}" x1="0" y1="0" x2="1" y2="1">'
        f'<stop offset="0" stop-color="{c[0]}"/><stop offset="1" stop-color="{c[1]}"/></linearGradient></defs>'
        + "".join(body)
        + "</svg>"
    )


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    motifs = {
        "travel-postcard": ("mesh", 1),
        "retro-film": ("stripes", 2),
        "neon-portrait": ("arcs", 3),
        "spring-festival": ("mesh", 4),
        "product-hero": ("ring", 5),
        "grid-collage": ("grid", 6),
        "story-cover": ("arcs", 7),
        "brand-banner": ("stripes", 8),
    }
    for name, palette in PALETTES.items():
        motif, seed = motifs[name]
        (OUT_DIR / f"{name}.svg").write_text(compose(name, palette, motif, seed), encoding="utf-8")
        print("written", name)


if __name__ == "__main__":
    main()
