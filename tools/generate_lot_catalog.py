"""Genera el catálogo de búsqueda de lotes desde el GeoPDF oficial de GeoLuker."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from collections import defaultdict
from pathlib import Path

import pdfplumber


VALID_RANGES = {
    "CHU": (1, 50),
    "AP": (18, 25),
    "MP": (1, 4),
    "A": (16, 27),
    "B": (20, 34),
    "C": (23, 35),
    "D": (21, 34),
    "E": (17, 34),
    "F": (15, 33),
    "G": (11, 27),
    "H": (9, 23),
    "I": (5, 17),
    "J": (9, 14),
}

TOKEN_PATTERN = re.compile(
    r"(CHU|AP|MP|[A-J])-?(\d{1,2})([A-Z#]{0,4}?)"
    r"(?=(?:(?:CHU|AP|MP|[A-J])-?\d)|$)",
)


def natural_key(code: str) -> tuple[str, int, str]:
    match = re.fullmatch(r"(CHU|AP|MP|[A-J])(\d{1,2})([A-Z]*)", code)
    if not match:
        return code, 0, ""
    prefix, number, suffix = match.groups()
    return prefix, int(number), suffix


def extract_catalog(source: Path) -> dict[str, object]:
    positions: dict[str, list[tuple[float, float]]] = defaultdict(list)

    with pdfplumber.open(source) as document:
        if len(document.pages) != 1:
            raise ValueError("El mapa oficial debe tener exactamente una página")
        page = document.pages[0]
        for word in page.extract_words(
            x_tolerance=2,
            y_tolerance=2,
            keep_blank_chars=False,
        ):
            raw = re.sub(r"[^A-Z0-9#-]", "", word["text"].upper())
            for match in TOKEN_PATTERN.finditer(raw):
                prefix, number_text, suffix = match.groups()
                number = int(number_text)
                minimum, maximum = VALID_RANGES[prefix]
                if number not in range(minimum, maximum + 1):
                    continue

                code = f"{prefix}{number}{suffix.replace('#', '')}"
                x_fraction = (word["x0"] + word["x1"]) / 2 / page.width
                y_fraction = (word["top"] + word["bottom"]) / 2 / page.height
                positions[code].append((x_fraction, y_fraction))

        lots = []
        for code in sorted(positions, key=natural_key):
            samples = positions[code]
            lots.append(
                {
                    "code": code,
                    "xFraction": round(sum(x for x, _ in samples) / len(samples), 6),
                    "yFraction": round(sum(y for _, y in samples) / len(samples), 6),
                },
            )

        return {
            "source": source.name,
            "sourceSha256": hashlib.sha256(source.read_bytes()).hexdigest(),
            "pageWidth": page.width,
            "pageHeight": page.height,
            "lotCount": len(lots),
            "lots": lots,
        }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("output", type=Path)
    arguments = parser.parse_args()

    catalog = extract_catalog(arguments.source)
    arguments.output.parent.mkdir(parents=True, exist_ok=True)
    arguments.output.write_text(
        json.dumps(catalog, ensure_ascii=False, indent=2) + "\n",
        encoding="utf-8",
    )
    print(f"Catálogo generado: {catalog['lotCount']} lotes")


if __name__ == "__main__":
    main()
