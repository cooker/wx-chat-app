CREATE TABLE IF NOT EXISTS images (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    image_url TEXT NOT NULL,
    author_name TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS uploaded_files (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    original_name TEXT NOT NULL,
    storage_name TEXT NOT NULL,
    file_hash TEXT NOT NULL UNIQUE,
    file_path TEXT NOT NULL,
    url TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS albums (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    cover_file_id INTEGER,
    cover_url TEXT NOT NULL DEFAULT '',
    image_folder TEXT NOT NULL DEFAULT '',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS album_images (
    album_id INTEGER NOT NULL,
    file_id INTEGER NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (album_id, file_id)
);

CREATE TABLE IF NOT EXISTS access_events (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    album_id INTEGER NOT NULL,
    visitor_id TEXT NOT NULL,
    ip TEXT,
    device TEXT,
    device_version TEXT,
    viewed_at INTEGER NOT NULL
);

-- Existing file DBs keep their old table shape; IF NOT EXISTS does not add new columns.
-- These ALTERs align schema before data.sql; on fresh DBs they duplicate columns and are skipped via spring.sql.init.continue-on-error.
ALTER TABLE access_events ADD COLUMN ip TEXT;
ALTER TABLE access_events ADD COLUMN device TEXT;
ALTER TABLE access_events ADD COLUMN device_version TEXT;
