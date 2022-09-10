package com.tisawesomeness.namehistorian;

import lombok.Cleanup;
import org.sqlite.SQLiteDataSource;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class NameHistorian {

    private static final int VERSION = 0;

    private static final String GET_VERSION_SQL = """
            SELECT `version` FROM `version`;
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
     * @throws IllegalStateException if the database is not at the correct version
     */
    public NameHistorian(Path databasePath) throws SQLException {
        SQLiteDataSource ds = new SQLiteConnectionPoolDataSource();
        ds.setUrl("jdbc:sqlite:" + databasePath.toFile().getAbsolutePath());
        ds.setEncoding("UTF-8");
        source = ds;

        runScript("schema.sql");
        if (getVersion() != VERSION) {
            throw new IllegalStateException("Database version is not " + VERSION);
        }
    }

    private int getVersion() throws SQLException {
        @Cleanup Connection con = source.getConnection();
        @Cleanup Statement st = con.createStatement();
        @Cleanup ResultSet rs = st.executeQuery(GET_VERSION_SQL);
        if (!rs.next()) {
            throw new IllegalStateException("Database version is not set");
        }
        return rs.getInt("version");
    }

    private void runScript(String scriptName) throws SQLException {
        String sql = Util.loadResource(scriptName);
        @Cleanup Connection con = source.getConnection();
        for (String statement : sql.split(";")) {
            if (!statement.isBlank()) {
                @Cleanup Statement st = con.createStatement();
                st.execute(statement);
            }
        }
    }

    /**
     * Records a player name.
     * If the player was previously seen with the given username, updates the last seen time.
     * Otherwise, records a name change at the current time.
     * @param uuid the player's UUID
     * @param username the player's username
     * @throws SQLException on database error
     */
    public void recordName(UUID uuid, String username) throws SQLException {
        asTransaction(con -> recordName(con, uuid, username));
    }

    /**
     * Records a collection of player names.
     * If the player was previously seen with the given username, updates the last seen time.
     * Otherwise, records a name change at the current time.
     * @param players the player UUIDs and usernames
     * @throws SQLException on database error
     */
    public void recordNames(Collection<NamedPlayer> players) throws SQLException {
        if (players.isEmpty()) {
            return; // Skip making db connection
        }
        asTransaction(con -> {
            for (NamedPlayer np : players) {
                recordName(con, np.uuid(), np.username());
            }
        });
    }

    private void recordName(Connection con, UUID uuid, String username) throws SQLException {
        NameDBRecord nr = findLatestNameRecord(con, uuid);
        if (nr != null && nr.username().equals(username)) {
            updateLastSeenTime(con, nr);
        } else {
            recordNewName(con, uuid, username);
        }
    }

    private @Nullable NameDBRecord findLatestNameRecord(Connection con, UUID uuid) throws SQLException {
        @Cleanup PreparedStatement st = con.prepareStatement(READ_LATEST_SQL);
        st.setString(1, uuid.toString());
        @Cleanup ResultSet rs = st.executeQuery();
        if (rs.next()) {
            return readDBRecord(rs, uuid);
        }
        return null;
    }

    private void updateLastSeenTime(Connection con, NameDBRecord nr) throws SQLException {
        @Cleanup PreparedStatement st = con.prepareStatement(UPDATE_LAST_SEEN_SQL);
        st.setLong(1, System.currentTimeMillis());
        st.setInt(2, nr.id());
        st.executeUpdate();
    }

    private void recordNewName(Connection con, UUID uuid, String username) throws SQLException {
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

    private void asTransaction(SqlFunction func) throws SQLException {
        @Cleanup Connection con = source.getConnection();
        con.setAutoCommit(false);
        try {
            func.accept(con);
            con.commit();
        } catch (Exception e) {
            con.rollback();
            throw e;
        }
    }
    @FunctionalInterface
    private interface SqlFunction {
        void accept(Connection con) throws SQLException;
    }

}
