package com.ssl.cltracker.database.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.ssl.cltracker.data.Utils;

public class CLRequestHistory {

    public static final String TABLE = "CLRequestHistory";

    public static class Columns {

        public static final String CL = "CL";
        public static final String TIMESTAMP = "TimeStamp";
        public static final String STATE = "State";
    }

    private static final String CREATE =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE + " ("
                    + Columns.CL + " MEDIUMINT NOT NULL,"
                    + Columns.TIMESTAMP + " TIMESTAMP,"
                    + Columns.STATE + " SMALLINT NOT NULL,"
                    + "PRIMARY KEY(" + Columns.CL + ")"
                    + ")";

    private static final String INSERT =
            "INSERT INTO " + TABLE
                    + " values (?, NULL, 1) "
                    + "ON DUPLICATE KEY UPDATE "
                    + Columns.STATE + "=?";

    // Query
    private static final String QUERY =
            "SELECT %s FROM " + TABLE
                    + " %s";

    // Delete
    private static final String DELETE =
            "DELETE FROM " + TABLE;

    private static final String DELETE_JOIN_CLREQUEST =
            "DELETE " + TABLE + ", " + CLResult.TABLE
                    + " FROM " + TABLE
                    + " INNER JOIN " + CLResult.TABLE
                    + " ON " + CLResult.TABLE + "." + CLResult.Columns.CL
                    + " = " + TABLE + "." + Columns.CL;

    // Where
    private static final String WHERE_CL =
            " WHERE " + TABLE + "." + Columns.CL
                    + " = ?";
    private static final String WHERE_TIME_PAST =
            " WHERE " + TABLE + "." + Columns.TIMESTAMP
                    + " < (NOW() - INTERVAL %s)";
    private static final String WHERE_TIME_WITHIN =
            " WHERE " + TABLE + "." + Columns.TIMESTAMP
                    + " > (NOW() - INTERVAL %s)";

    public int mCL;
    public Timestamp mTimeStamp;
    public int mState;

    public CLRequestHistory(int cl, Timestamp time, int state) {
        mCL = cl;
        mTimeStamp = time;
        mState = state;
    }

    public static boolean createTableIfNotExist(Connection connection) {
        Statement statement = null;

        try {
            statement = connection.createStatement();
            return statement.execute(CREATE);
        } catch (SQLException e) {
            Utils.say("Not able to create table");
        } finally {
            try {
                if (statement != null)
                    statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * Insert/update CL with current timestamp
     * @param connection MUST BE active connection, also ensure closing connection
     * @param cl changeList
     * @return number of row updated/inserted, 1 or 0
     */
    public static int insert(Connection connection, CLRequestHistory cl) {
        List<CLRequestHistory> list = new ArrayList<CLRequestHistory>(1);
        list.add(cl);
        return insertAll(connection, list);
    }

    /**
     * Insert/update CL with current timestamp
     * @param connection MUST BE active connection, also ensure closing connection
     * @param cls list of changeList
     * @return number of row updated/inserted, anynumber less than size of cl list
     */
    public static int insertAll(Connection connection, List<CLRequestHistory> cls) {
        PreparedStatement statement = null;

        try {
            // commit at once.
            final boolean ac = connection.getAutoCommit();
            connection.setAutoCommit(false);

            // execute statement one by one
            int numUpdate = 0;
            statement = connection.prepareStatement(INSERT);
            for (CLRequestHistory cl : cls) {
                statement.setInt(1, cl.mCL);
                statement.setInt(2, cl.mState);
                numUpdate += statement.executeUpdate();
            }

            // commit all changes above
            connection.commit();

            // restore previous AutoCommit setting
            connection.setAutoCommit(ac);

            // Close Statement to release all resources
            statement.close();
            return numUpdate;
        } catch (SQLException e) {
            Utils.say("Not able to insert for CLs : " + cls.size());
        } finally {
            try {
                if (statement != null)
                    statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * return CLRequestHistory object with CL and corresponding Timestamp if exist
     * @param connection MUST BE active connection, also ensure closing connection
     * @param cl changelist
     * @return null if not exist
     */
    public static CLRequestHistory query(Connection connection, int cl) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String query = String.format(QUERY, "*", "");
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            while (rs.next()) {
                final Timestamp ts = rs.getTimestamp(Columns.TIMESTAMP);
                final int state = rs.getInt(Columns.STATE);
                return new CLRequestHistory(cl, ts, state);
            }
        } catch (SQLException e) {
            Utils.say("Not able to query for " + cl);
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (statement != null)
                    statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * return list of CLRequestHistory object if exist any
     * @param connection MUST BE active connection, also ensure closing connection
     * @return null if not exist
     */
    public static List<CLRequestHistory> query(Connection connection) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String query = String.format(QUERY, "*", "");
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            List<CLRequestHistory> list = new ArrayList<>();
            while (rs.next()) {
                final int cl = rs.getInt(Columns.CL);
                final Timestamp ts = rs.getTimestamp(Columns.TIMESTAMP);
                final int state = rs.getInt(Columns.STATE);

                list.add(new CLRequestHistory(cl, ts, state));
            }
            return list;
        } catch (SQLException e) {
            Utils.say("Not able to query anything");
        } finally {
            try {
                if (rs != null)
                    rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            try {
                if (statement != null)
                    statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * delete row from table based on changelist
     * @param connection MUST BE active connection, also ensure closing connection
     * @param cl changelist
     * @param join if true, it will delete corresponding row from {@link CLResult#TABLE} as well as this table
     * @return number of deleted row
     */
    public static int delete(Connection connection, int cl, boolean join) {
        PreparedStatement statement = null;

        try {
            String deleteState = null;
            if (join) {
                deleteState = DELETE_JOIN_CLREQUEST;
            } else {
                deleteState = DELETE;
            }
            statement = connection.prepareStatement(deleteState + WHERE_CL);
            statement.setInt(1, cl);
            return statement.executeUpdate();
        } catch (SQLException e) {
            Utils.say("Not able to delete for CL : " + cl);
        } finally {
            try {
                if (statement != null)
                    statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }

    /**
     * delete row from table based on changelist
     * @param connection MUST BE active connection, also ensure closing connection
     * @param time time in following format( x HOUR/MINUTE where x is integer, ex) 1 HOUR, or 60 MINUTE
     * @param within if true, delete rows time within given time. otherwise, delete row past given time.
     * @param join if true, it will delete corresponding row from {@link CLResult#TABLE} as well as this table
     * @return number of deleted row
     */
    public static int deleteTimeBased(Connection connection, String time, boolean within, boolean join) {
        Statement statement = null;

        try {
            String deleteState = null;
            if (join) {
                deleteState = DELETE_JOIN_CLREQUEST;
            } else {
                deleteState = DELETE;
            }

            String whereClause = null;
            if (within) {
                whereClause = WHERE_TIME_WITHIN;
            } else {
                whereClause = WHERE_TIME_PAST;
            }

            final String delete = String.format(deleteState + whereClause, time);
            statement = connection.createStatement();
            return statement.executeUpdate(delete);
        } catch (SQLException e) {
            Utils.say("Not able to delete for time : " + time);
        } finally {
            try {
                if (statement != null)
                    statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return 0;
    }
}
