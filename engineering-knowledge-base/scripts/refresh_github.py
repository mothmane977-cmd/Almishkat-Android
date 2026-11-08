#!/usr/bin/env python3
"""Opt-in refresh of public GitHub metadata. Requires internet; preserves review decisions."""
import json,datetime,urllib.request,pathlib,time
root=pathlib.Path(__file__).resolve().parents[1]
p=root/'registry/repositories.json'; rows=json.loads(p.read_text())
for r in rows:
 try:
  req=urllib.request.Request('https://api.github.com/repos/'+r['repo'],headers={'Accept':'application/vnd.github+json','User-Agent':'ALMISHKAT-KB-Refresh'})
  with urllib.request.urlopen(req,timeout=15) as resp: d=json.load(resp)
  r.update(stars=d.get('stargazers_count'),license=(d.get('license') or {}).get('spdx_id') or 'UNKNOWN',last_push_utc=d.get('pushed_at'),primary_language=d.get('language'),archived=d.get('archived'),metadata_checked_utc=datetime.datetime.now(datetime.timezone.utc).date().isoformat())
  print('OK',r['repo'])
 except Exception as e: print('FAILED',r['repo'],str(e))
 time.sleep(0.25)
p.write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
print('Metadata refreshed; license and code review decisions are NOT auto-approved.')
