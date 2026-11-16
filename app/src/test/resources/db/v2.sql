-- RECONSTRUCTED: NOT AN AUTHENTIC HISTORICAL SQLITE SCHEMA
-- Source evidence: docs/QURAN_DATABASE_V5_AR.md, current QuranLocalDatabase.kt on develop
-- Inference: original v1 fields, types and indexes NOT independently verified
CREATE TABLE quran_ayah_hafs (
  surah_no INTEGER NOT NULL, ayah_no INTEGER NOT NULL, text_uthmani TEXT NOT NULL,
  juz_no INTEGER, page_no INTEGER, PRIMARY KEY(surah_no, ayah_no)
);
CREATE TABLE quran_ayah_warsh (
  surah_no INTEGER NOT NULL, ayah_no INTEGER NOT NULL, text_uthmani TEXT NOT NULL,
  juz_no INTEGER, page_no INTEGER, PRIMARY KEY(surah_no, ayah_no)
);
CREATE TABLE quran_dataset_manifest (
  riwayah TEXT PRIMARY KEY, source_name TEXT NOT NULL, source_version TEXT NOT NULL,
  source_url TEXT NOT NULL, sha256 TEXT NOT NULL, ayah_count INTEGER NOT NULL,
  imported_at INTEGER NOT NULL
);
CREATE INDEX idx_hafs_surah ON quran_ayah_hafs(surah_no);
CREATE INDEX idx_warsh_surah ON quran_ayah_warsh(surah_no);
-- RECONSTRUCTED addition for SQLite v2; docs/PROJECT_V7_LOCAL_TIMING_AR.md
CREATE TABLE ayah_timings (
  riwayah TEXT NOT NULL, surah_no INTEGER NOT NULL, ayah_no INTEGER NOT NULL,
  start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, audio_uri TEXT,
  updated_at INTEGER NOT NULL, PRIMARY KEY(riwayah, surah_no, ayah_no)
);
PRAGMA user_version = 2;
