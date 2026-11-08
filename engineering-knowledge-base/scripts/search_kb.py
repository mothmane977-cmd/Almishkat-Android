#!/usr/bin/env python3
"""Offline search across repository metadata and local SKILL.md files."""
import json,sys,pathlib
root=pathlib.Path(__file__).resolve().parents[1]
q=' '.join(sys.argv[1:]).strip().lower()
if not q: raise SystemExit('Usage: python scripts/search_kb.py <keyword>')
for r in json.loads((root/'registry/repositories.json').read_text()):
 if q in json.dumps(r,ensure_ascii=False).lower(): print(f"REPO {r['repo']} | {r['status']} | {r['url']}")
for p in (root/'.agents/skills').rglob('SKILL.md'):
 if q in p.read_text().lower():print(f'SKILL {p.parent.name} | {p.relative_to(root)}')
