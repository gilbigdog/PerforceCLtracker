package com.ssl.cltracker.database.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.ssl.cltracker.data.Utils;

public class Recipients {

    public static final String TABLE = "Recipients";

    public static class Columns {
        public static final String EMAIL = "Email";
    }

    private static final String CREATE =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE + " ("
                    + Columns.EMAIL + " VARCHAR(100) NOT NULL,"
                    + "PRIMARY KEY(" + Columns.EMAIL + ")"
                    + ")";

    private static final String INSERT =
            "INSERT INTO " + TABLE
                    + " values (?)";

    // Query
    private static final String QUERY =
            "SELECT %s FROM " + TABLE;

    // Delete
    private static final String DELETE =
            "DELETE FROM " + TABLE;

    // Where
    private static final String WHERE_CL =
            " WHERE " + TABLE + "." + Columns.EMAIL
                    + " = ?";

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
     * Insert email
     * @param connection MUST BE active connection, also ensure closing connection
     * @param email email
     * @return number of row inserted, 1 or 0
     */
    public static int insert(Connection connection, String email) {
        List<String> list = new ArrayList<String>(1);
        list.add(email);
        return insert(connection, list);
    }

    /**
     * Insert emails
     * @param connection MUST BE active connection, also ensure closing connection
     * @param emails list of email
     * @return number of row inserted, anynumber less than size of email list
     */
    public static int insert(Connection connection, List<String> emails) {
        PreparedStatement statement = null;

        try {
            // commit at once.
            final boolean ac = connection.getAutoCommit();
            connection.setAutoCommit(false);

            // execute statement one by one
            int numUpdate = 0;
            statement = connection.prepareStatement(INSERT);
            for (String email : emails) {
                statement.setString(1, email);
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
            Utils.say("Not able to insert for emails : " + emails.size());
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
     * @param connection MUST BE active connection, also ensure closing connection
     * @param email email
     * @return true if given email exist
     */
    public static boolean query(Connection connection, String email) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String query = String.format(QUERY, "*", "");
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            return rs.next();
        } catch (SQLException e) {
            Utils.say("Not able to query for " + email);
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
        return false;
    }

    /**
     * return list of email if exist any
     * @param connection MUST BE active connection, also ensure closing connection
     * @return null if not exist
     */
    public static List<String> query(Connection connection) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String query = String.format(QUERY, "*", "");
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString(Columns.EMAIL));
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
     * delete row from table based on email
     * @param connection MUST BE active connection, also ensure closing connection
     * @param email email
     * @param join if true, it will delete corresponding row from {@link CLResult#TABLE} as well as this table
     * @return number of deleted row
     */
    public static int delete(Connection connection, String email) {
        PreparedStatement statement = null;

        try {
            statement = connection.prepareStatement(DELETE + WHERE_CL);
            statement.setString(1, email);
            return statement.executeUpdate();
        } catch (SQLException e) {
            Utils.say("Not able to delete for email : " + email);
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
