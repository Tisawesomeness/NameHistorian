CREATE TABLE IF NOT EXISTS `version` (
    `id` INTEGER PRIMARY KEY NOT NULL,
    `version` INTEGER NOT NULL
);
INSERT OR IGNORE INTO `version` (`id`, `version`) VALUES (0, 0);
CREATE TABLE IF NOT EXISTS `name_history` (
    `id` INTEGER PRIMARY KEY NOT NULL,
    `uuid` TEXT NOT NULL,
    `username` TEXT NOT NULL,
    `first_seen_time` INTEGER NOT NULL,
    `detected_time` INTEGER,
    `last_seen_time` INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS `first_seen_time_index` ON `name_history` (`first_seen_time`);
