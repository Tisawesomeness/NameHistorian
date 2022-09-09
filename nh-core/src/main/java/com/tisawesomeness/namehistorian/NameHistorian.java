package com.tisawesomeness.namehistorian;

import lombok.Cleanup;
import org.sqlite.SQLiteDataSource;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NameHistorian {

    private static final String SCHEMA_SQL = """
            CREATE TABLE IF NOT EXISTS `name_history` (
                `id` INTEGER PRIMARY KEY NOT NULL,
                `uuid` TEXT NOT NULL,
                `username` TEXT NOT NULL,
                `first_seen_time` INTEGER NOT NULL,
                `detected_time` INTEGER,
                `last_seen_time` INTEGER NOT NULL
            );
            """;
    private static final String INDEX_SQL = """
            CREATE INDEX IF NOT EXISTS `first_seen_time_index` ON `name_history` (`first_seen_time`);
            """;
    private static final String READ_ALL_HISTORY_SQL = """
            SELECT `id`, `username`, `first_seen_time`, `detected_time`, `last_seen_time`
            FROM `name_history`
            WHERE `uuid` = ?
            ORDER BY `first_seen_time` DESC;
            """;
    private static final String READ_LATEST_SQL = """
            SELECT `id`, `username`, `first_seen_time`, `detected_time`, `last_seen_time`
            FROM `name_history`
            WHERE `uuid` = ?
            ORDER BY `first_seen_time` DESC
            LIMIT 1;
            """;
    private static final String UPDATE_LAST_SEEN_SQL = """
            UPDATE `name_history`
            SET `last_seen_time` = ?
            WHERE `id` = ?;
            """;
    private static final String INSERT_NAME_RECORD_SQL = """
            INSERT INTO `name_history` (
                `uuid`,
                `username`,
                `first_seen_time`,
                `detected_time`,
                `last_seen_time`
            ) VALUES (?, ?, ?, ?, ?);
            """;

    private final DataSource source;

    /**
     * Initializes NameHistorian by connecting to a SQLite database.
     * If the database doesn't exist, creates it.
     * @param databasePath the path to the database file
     * @throws SQLException if the database cannot be accessed, the parent folder doesn't exist,
     * or an error occurs when creating the table
     */
    public NameHistorian(Path databasePath) throws SQLException {
        SQLiteDataSource ds = new SQLiteConnectionPoolDataSource();
        ds.setUrl("jdbc:sqlite:" + databasePath.toFile().getAbsolutePath());
        ds.setEncoding("UTF-8");
        source = ds;
        createTable();
    }
    private void createTable() throws SQLException {
        @Cleanup Connection con = source.getConnection();
        con.createStatement().execute(SCHEMA_SQL);
        con.createStatement().execute(INDEX_SQL);
    }

    /**
     * Records a player name.
     * If the player was previously seen with the given username, updates the last seen time.
     * Otherwise, records a name change.
     * @param uuid the player's UUID
     * @param username the player's username
     * @throws SQLException on database error
     */
    public void recordName(UUID uuid, String username) throws SQLException {
        NameDBRecord nr = findLatestNameRecord(uuid);
        if (nr != null && nr.username().equals(username)) {
            updateLastSeenTime(nr);
        } else {
            recordNewName(uuid, username);
        }
    }

    private @Nullable NameDBRecord findLatestNameRecord(UUID uuid) throws SQLException {
        @Cleanup Connection con = source.getConnection();
        @Cleanup PreparedStatement st = con.prepareStatement(READ_LATEST_SQL);
        st.setString(1, uuid.toString());
        @Cleanup ResultSet rs = st.executeQuery();
        if (rs.next()) {
            return readDBRecord(rs, uuid);
        }
        return null;
    }

    private void updateLastSeenTime(NameDBRecord nr) throws SQLException {
        @Cleanup Connection con = source.getConnection();
        @Cleanup PreparedStatement st = con.prepareStatement(UPDATE_LAST_SEEN_SQL);
        st.setLong(1, System.currentTimeMillis());
        st.setInt(2, nr.id());
        st.executeUpdate();
    }

    private void recordNewName(UUID uuid, String username) throws SQLException {
        @Cleanup Connection con = source.getConnection();
        @Cleanup PreparedStatement st = con.prepareStatement(INSERT_NAME_RECORD_SQL);
        st.setString(1, uuid.toString());
        st.setString(2, username);
        st.setLong(3, System.currentTimeMillis());
        st.setNull(4, Types.INTEGER);
        st.setLong(5, System.currentTimeMillis());
        st.executeUpdate();
    }

    /**
     * Gets the player's name history, most recent first.
     * Each record contains the username, the time it was first seen, and the time it was last seen.
     * @param uuid the player's UUID
     * @return a list of name records or empty if the player has never been seen
     * @throws SQLException on database error
     */
    public List<NameRecord> getNameHistory(UUID uuid) throws SQLException {
        @Cleanup Connection con = source.getConnection();
        @Cleanup PreparedStatement st = con.prepareStatement(READ_ALL_HISTORY_SQL);
        st.setString(1, uuid.toString());
        @Cleanup ResultSet rs = st.executeQuery();
        List<NameRecord> list = new ArrayList<>();
        while (rs.next()) {
            list.add(readDBRecord(rs, uuid).toNameRecord());
        }
        return list;
    }

    private NameDBRecord readDBRecord(ResultSet rs, UUID uuid) throws SQLException {
        return new NameDBRecord(
                rs.getInt("id"),
                uuid.toString(),
                rs.getString("username"),
                rs.getLong("first_seen_time"),
                readNullableLong(rs, "detected_time"),
                rs.getLong("last_seen_time")
        );
    }
    private @Nullable Long readNullableLong(ResultSet rs, String column) throws SQLException {
        long l = rs.getLong(column);
        return rs.wasNull() ? null : l;
    }

}
