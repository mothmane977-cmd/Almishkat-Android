-- RECONSTRUCTED v8, NOT VERIFIED HISTORICAL DDL OR ACTUAL DB SNAPSHOT
-- Evidence: current v10 onUpgrade and docs/PROJECT_V12_PERSISTED_CLIPS_AR.md, docs/PROJECT_V13_AYAH_LINKING_AR.md, docs/PROJECT_V16_SCENE_LAYERS_AR.md, docs/PROJECT_V18_BACKGROUNDS_AR.md, docs/PROJECT_V19_MOTION_BACKGROUNDS_AR.md
-- ASSUMPTION: historical column order, constraints and legacy indexes are inferred
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
-- RECONSTRUCTED addition for SQLite v3; docs/PROJECT_V8_PROJECTS_AR.md
CREATE TABLE studio_projects (
  project_id TEXT PRIMARY KEY, title TEXT NOT NULL, riwayah TEXT NOT NULL,
  surah_no INTEGER NOT NULL, theme_id TEXT NOT NULL, canvas_ratio TEXT NOT NULL,
  audio_uri TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
);
-- SQLite v4: audio clips without descriptive project/ayah links (V12 documentation)
CREATE TABLE audio_clip_selections (
    clip_id TEXT PRIMARY KEY,
    source_uri TEXT NOT NULL,
    start_ms INTEGER NOT NULL,
    end_ms INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX idx_audio_clip_uri ON audio_clip_selections(source_uri);
-- SQLite v5: historically described ALTER ADD LINK columns (V13 documentation)
ALTER TABLE audio_clip_selections ADD COLUMN project_id TEXT;
ALTER TABLE audio_clip_selections ADD COLUMN riwayah TEXT;
ALTER TABLE audio_clip_selections ADD COLUMN surah_no INTEGER;
ALTER TABLE audio_clip_selections ADD COLUMN ayah_no INTEGER;
CREATE INDEX idx_audio_clip_project ON audio_clip_selections(project_id);
-- SQLite v6: scene layers; no scene_id yet (V16 documentation)
CREATE TABLE scene_layers (
    layer_id TEXT PRIMARY KEY,
    project_id TEXT NOT NULL,
    layer_type TEXT NOT NULL,
    title TEXT NOT NULL,
    content TEXT NOT NULL,
    start_ms INTEGER NOT NULL,
    end_ms INTEGER NOT NULL,
    position_x REAL NOT NULL,
    position_y REAL NOT NULL,
    font_size REAL NOT NULL,
    visible INTEGER NOT NULL,
    z_order INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX idx_scene_layers_project ON scene_layers(project_id, z_order);
-- SQLite v7: project-only background before video_uri (V18 documentation)
CREATE TABLE scene_backgrounds (
    project_id TEXT PRIMARY KEY,
    image_uri TEXT,
    preset TEXT NOT NULL DEFAULT 'emerald',
    motion_enabled INTEGER NOT NULL DEFAULT 1,
    updated_at INTEGER NOT NULL
);
-- SQLite v8: V19 added video_uri
ALTER TABLE scene_backgrounds ADD COLUMN video_uri TEXT;

PRAGMA user_version = 8;
