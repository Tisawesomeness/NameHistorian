package com.tisawesomeness.namehistorian;

import lombok.Cleanup;
import org.sqlite.SQLiteDataSource;
import org.sqlite.javax.SQLiteConnectionPoolDataSource;

import javax.annotation.Nullable;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public final class NameHistorian {

    private static final int VERSION = 0;

    private static final String GET_VERSION_SQL = "" +
            "SELECT `version` FROM `version`;";
    private static final String READ_ALL_HISTORY_SQL = "" +
            "SELECT `id`, `username`, `first_seen_time`, `detected_time`, `last_seen_time`\n" +
            "FROM `name_history`\n" +
            "WHERE `uuid` = ?\n" +
            "ORDER BY `first_seen_time` DESC, `last_seen_time` DESC;";
    private static final String READ_LATEST_SQL = "" +
            "SELECT `id`, `username`, `first_seen_time`, `detected_time`, `last_seen_time`\n" +
            "FROM `name_history`\n" +
            "WHERE `uuid` = ?\n" +
            "ORDER BY `first_seen_time` DESC\n" +
            "LIMIT 1;";
    private static final String UPDATE_LAST_SEEN_SQL = "" +
            "UPDATE `name_history`\n" +
            "SET `last_seen_time` = ?\n" +
            "WHERE `id` = ?;";
    private static final String INSERT_NAME_RECORD_SQL = "" +
            "INSERT INTO `name_history` (\n" +
            "    `uuid`,\n" +
            "    `username`,\n" +
            "    `first_seen_time`,\n" +
            "    `detected_time`,\n" +
            "    `last_seen_time`\n" +
            ") VALUES (?, ?, ?, ?, ?);";
    private static final String DELETE_ALL_HISTORY_SQL = "" +
            "DELETE FROM `name_history`\n" +
            "WHERE `uuid` = ?;";

    private final DataSource source;
    private final MojangLookup lookup;

    /**
     * Initializes NameHistorian by connecting to a SQLite database.
     * If the database doesn't exist, creates it.
     * @param databasePath the path to the database file
     * @throws SQLException if the database cannot be accessed, the parent folder doesn't exist,
     * or an error occurs when creating the table
     * @throws IllegalStateException if the database is not at the correct version
     */
    public NameHistorian(Path databasePath) throws SQLException {
        this(databasePath, new MojangLookupImpl());
    }
    NameHistorian(Path databasePath, MojangLookup lookup) throws SQLException {
        SQLiteDataSource ds = new SQLiteConnectionPoolDataSource();
        ds.setUrl("jdbc:sqlite:" + databasePath.toFile().getAbsolutePath());
        source = ds;

        runScript("schema.sql");
        if (getVersion() != VERSION) {
            throw new IllegalStateException("Database version is not " + VERSION);
        }

        this.lookup = lookup;
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
            if (!Util.isBlank(statement)) {
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
        Instant now = Instant.now();
        recordName(new NameRecord(uuid, username, now, null, now));
    }
    public void recordName(NameRecord nr) throws SQLException {
        asTransaction(con -> recordName(con, nr));
    }

    /**
     * Records a collection of player names.
     * If the player was previously seen with the given username, updates the last seen time.
     * Otherwise, records a name change at the current time.
     * @param players the player UUIDs and usernames
     * @throws SQLException on database error
     */
    public void recordNames(Collection<NamedPlayer> players) throws SQLException {
        Instant now = Instant.now();
        List<NameRecord> records = players.stream()
                .map(np -> new NameRecord(np.getUuid(), np.getUsername(), now, null, now))
                .collect(Collectors.toList());
        recordNameRecords(records);
    }
    private void recordNameRecords(Collection<NameRecord> records) throws SQLException {
        if (records.isEmpty()) {
            return; // Skip making db connection
        }
        asTransaction(con -> {
            for (NameRecord nr : records) {
                recordName(con, nr);
            }
        });
    }

    private void recordName(Connection con, NameRecord recordToAdd) throws SQLException {
        NameDBRecord latestRecord = findNameRecord(con, recordToAdd.getUuid());
        if (latestRecord != null && latestRecord.getUsername().equals(recordToAdd.getUsername())) {
            updateLastSeenTime(con, latestRecord.getId());
        } else {
            recordNewName(con, recordToAdd);
        }
    }

    private @Nullable NameDBRecord findNameRecord(Connection con, UUID uuid) throws SQLException {
        @Cleanup PreparedStatement st = con.prepareStatement(READ_LATEST_SQL);
        st.setString(1, uuid.toString());
        @Cleanup ResultSet rs = st.executeQuery();
        if (rs.next()) {
            return readDBRecord(rs, uuid);
        }
        return null;
    }

    private void updateLastSeenTime(Connection con, int id) throws SQLException {
        @Cleanup PreparedStatement st = con.prepareStatement(UPDATE_LAST_SEEN_SQL);
        st.setLong(1, System.currentTimeMillis());
        st.setInt(2, id);
        st.executeUpdate();
    }

    private void recordNewName(Connection con, NameRecord nr) throws SQLException {
        @Cleanup PreparedStatement st = con.prepareStatement(INSERT_NAME_RECORD_SQL);
        st.setString(1, nr.getUuid().toString());
        st.setString(2, nr.getUsername());
        st.setLong(3, nr.getFirstSeenTime().toEpochMilli());
        setNullableLong(st, 4, Util.mapNullable(nr.getRawDetectedTime(), Instant::toEpochMilli));
        st.setLong(5, nr.getLastSeenTime().toEpochMilli());
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
        return retrieveNameHistory(con, uuid);
    }
    private List<NameRecord> retrieveNameHistory(Connection con, UUID uuid) throws SQLException {
        @Cleanup PreparedStatement st = con.prepareStatement(READ_ALL_HISTORY_SQL);
        st.setString(1, uuid.toString());
        @Cleanup ResultSet rs = st.executeQuery();
        List<NameRecord> list = new ArrayList<>();
        while (rs.next()) {
            list.add(readDBRecord(rs, uuid).toNameRecord());
        }
        return list;
    }

    /**
     * Requests the player's name history from the Mojang API and saves it to the database.
     * This will overwrite the player's existing name history.
     * BEWARE: this may error when Mojang disables the API endpoint on September 13th!
     * @param uuid The UUID of the player to look up
     * @return True on success, false if history cannot be looked up
     * @throws IOException see {@link MojangLookup#fetchNameChanges(UUID)}
     * @throws SQLException on database error
     */
    public boolean saveMojangHistory(UUID uuid) throws IOException, SQLException {
        List<NameChange> history = lookup.fetchNameChanges(uuid);
        if (history.isEmpty()) {
            return false;
        }
        List<NameRecord> records = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < history.size(); i++) {
            NameChange change = history.get(i);

            NameChange next = i + 1 < history.size() ? history.get(i + 1) : null;
            Instant firstSeen;
            if (!change.isOriginal()) {
                firstSeen = change.getChangeTime();
            } else if (next != null) {
                firstSeen = next.getChangeTime();
            } else {
                firstSeen = now;
            }
            Instant lastSeen = next != null ? next.getChangeTime() : now;

            records.add(new NameRecord(uuid, change.getName(), firstSeen, now, lastSeen));
        }

        asTransaction(con -> {
            deleteHistory(con, uuid);
            for (NameRecord nr : records) {
                recordNewName(con, nr);
            }
        });
        return true;
    }
    private void deleteHistory(Connection con, UUID uuid) throws SQLException {
        @Cleanup PreparedStatement st = con.prepareStatement(DELETE_ALL_HISTORY_SQL);
        st.setString(1, uuid.toString());
        st.executeUpdate();
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
    private void setNullableLong(PreparedStatement st, int index, @Nullable Long value) throws SQLException {
        if (value == null) {
            st.setNull(index, Types.INTEGER);
        } else {
            st.setLong(index, value);
        }
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
