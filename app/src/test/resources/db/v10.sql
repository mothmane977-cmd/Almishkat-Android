-- SQLite v10: DERIVED FROM CURRENT onCreate, Git Blob 497c62c571d54cd0ed1dbe8a7ca074473c63bd20
-- Not an authenticated on-device v10 database snapshot
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
 source_url TEXT NOT NULL, sha256 TEXT NOT NULL, ayah_count INTEGER NOT NULL, imported_at INTEGER NOT NULL
);
CREATE INDEX idx_hafs_surah ON quran_ayah_hafs(surah_no);
CREATE INDEX idx_warsh_surah ON quran_ayah_warsh(surah_no);
CREATE TABLE ayah_timings (
 riwayah TEXT NOT NULL, surah_no INTEGER NOT NULL, ayah_no INTEGER NOT NULL,
 start_ms INTEGER NOT NULL, end_ms INTEGER NOT NULL, audio_uri TEXT,
 updated_at INTEGER NOT NULL, PRIMARY KEY(riwayah, surah_no, ayah_no)
);
CREATE TABLE studio_projects (
 project_id TEXT PRIMARY KEY, title TEXT NOT NULL, riwayah TEXT NOT NULL,
 surah_no INTEGER NOT NULL, theme_id TEXT NOT NULL, canvas_ratio TEXT NOT NULL,
 audio_uri TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
);
CREATE TABLE audio_clip_selections (
 clip_id TEXT PRIMARY KEY, source_uri TEXT NOT NULL, start_ms INTEGER NOT NULL,
 end_ms INTEGER NOT NULL, project_id TEXT, riwayah TEXT, surah_no INTEGER, ayah_no INTEGER,
 created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
);
CREATE INDEX idx_audio_clip_uri ON audio_clip_selections(source_uri);
CREATE INDEX idx_audio_clip_project ON audio_clip_selections(project_id);
CREATE TABLE scene_layers (
 layer_id TEXT PRIMARY KEY, project_id TEXT NOT NULL, layer_type TEXT NOT NULL,
 title TEXT NOT NULL, content TEXT NOT NULL, start_ms INTEGER NOT NULL,
 end_ms INTEGER NOT NULL, position_x REAL NOT NULL, position_y REAL NOT NULL,
 font_size REAL NOT NULL, visible INTEGER NOT NULL, z_order INTEGER NOT NULL,
 created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, scene_id TEXT
);
CREATE INDEX idx_scene_layers_project ON scene_layers(project_id, z_order);
CREATE INDEX idx_scene_layers_scene ON scene_layers(scene_id, z_order);
CREATE TABLE scene_backgrounds (
 project_id TEXT NOT NULL, image_uri TEXT, video_uri TEXT, scene_id TEXT,
 preset TEXT NOT NULL DEFAULT 'emerald', motion_enabled INTEGER NOT NULL DEFAULT 1,
 updated_at INTEGER NOT NULL, PRIMARY KEY(project_id, scene_id)
);
CREATE INDEX idx_scene_backgrounds_scene ON scene_backgrounds(scene_id);
CREATE TABLE studio_scenes (
 scene_id TEXT PRIMARY KEY, project_id TEXT NOT NULL, title TEXT NOT NULL,
 scene_order INTEGER NOT NULL, duration_ms INTEGER NOT NULL,
 transition_in TEXT NOT NULL DEFAULT 'CUT', transition_duration_ms INTEGER NOT NULL DEFAULT 0,
 notes TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL
);
CREATE INDEX idx_studio_scenes_project_order ON studio_scenes(project_id, scene_order);

PRAGMA user_version = 10;
