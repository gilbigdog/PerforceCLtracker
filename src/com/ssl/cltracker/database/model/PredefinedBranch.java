package com.ssl.cltracker.database.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.ssl.cltracker.data.Utils;

public class PredefinedBranch {

    public static final String TABLE = "PredefinedBranch";

    public static class Columns {

        public static final String ALIAS = "alias";
        public static final String FULLNAME = "full_name";
    }

    private static final String CREATE =
            "CREATE TABLE " + TABLE + " ("
                    + Columns.ALIAS + " VARCHAR(20) NOT NULL,"
                    + Columns.FULLNAME + " VARCHAR(200) NOT NULL,"
                    + "PRIMARY KEY(" + Columns.ALIAS + ", " + Columns.FULLNAME + ")"
                    + ")";

    private static final String INSERT =
            "INSERT INTO " + TABLE
                    + " values ( ?, ?)"
                    + "ON DUPLICATE KEY UPDATE "
                    + Columns.ALIAS + "=?";

    // Where
    private static final String WHERE_ALIAS =
            " WHERE " + TABLE + "." + Columns.ALIAS
                    + " IN (%s)";
    private static final String WHERE_FULLNAME =
            " WHERE " + TABLE + "." + Columns.FULLNAME
                    + " IN (%s)";

    // Query
    private static final String QUERY =
            "SELECT %s FROM " + TABLE;

    // DELETE
    private static final String DELETE =
            "DELETE FROM " + TABLE;

    public String mFullName;
    public String mAlias;

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
     * Insert/update PredefinedBranch
     * @param connection MUST BE active connection, also ensure closing connection
     * @param pb PredefinedBranch
     * @return number of row updated/inserted, 1 or 0
     */
    public static int insert(Connection connection, PredefinedBranch pb) {
        List<PredefinedBranch> list = new ArrayList<PredefinedBranch>(1);
        list.add(pb);
        return insertAll(connection, list);
    }

    /**
     * Insert/update PredefinedBranches
     * @param connection MUST BE active connection, also ensure closing connection
     * @param pbs list of PredefinedBranch
     * @return number of updated rows
     */
    public static int insertAll(Connection connection, List<PredefinedBranch> pbs) {
        PreparedStatement statement = null;

        try {
            // commit at once.
            final boolean ac = connection.getAutoCommit();
            connection.setAutoCommit(false);

            // execute statement one by one
            int numUpdate = 0;
            statement = connection.prepareStatement(INSERT);
            for (PredefinedBranch pb : pbs) {
                statement.setString(1, pb.mAlias);
                statement.setString(2, pb.mFullName);
                statement.setString(3, pb.mAlias);
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
            Utils.say("Not able to insert for list");
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
     * return List of PredefinedBranch for all rows in table 
     * @param connection MUST BE active connection, also ensure closing connection
     * @return null if not exist
     */
    public static List<PredefinedBranch> query(Connection connection) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String query = String.format(QUERY, "*");
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            return getPredefinedBranches(rs);
        } catch (SQLException e) {
            Utils.say("Not able to query for all");
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
     * return single PredefinedBranch matched to given alias
     * @param connection MUST BE active connection, also ensure closing connection
     * @param alias alias to be queried
     * @return null if not exist
     */
    public static List<PredefinedBranch> queryAlias(Connection connection, String alias) {
        List<String> list = new ArrayList<String>(1);
        list.add(alias);
        return queryAlias(connection, list);
    }

    /**
     * return List of PredefinedBranch matched to given alias list
     * @param connection MUST BE active connection, also ensure closing connection
     * @param alias list of alias to be queried
     * @return null if not exist
     */
    public static List<PredefinedBranch> queryAlias(Connection connection, List<String> alias) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String query = String.format(QUERY + WHERE_ALIAS, "*", Utils.join(", ", alias, true));
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            return getPredefinedBranches(rs);
        } catch (SQLException e) {
            Utils.say("Not able to query for multiple alias");
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
     * return List of PredefinedBranch matched to given fullName
     * @param connection MUST BE active connection, also ensure closing connection
     * @param fullName fullname to be queried
     * @return null if not exist
     */
    public static List<PredefinedBranch> queryFullName(Connection connection, String fullName) {
        List<String> list = new ArrayList<String>(1);
        list.add(fullName);
        return queryFullName(connection, list);
    }

    /**
     * return List of PredefinedBranch matched to given fullName
     * @param connection MUST BE active connection, also ensure closing connection
     * @param fullName list of fullname to be queried 
     * @return null if not exist
     */
    public static List<PredefinedBranch> queryFullName(Connection connection, List<String> fullName) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String query = String.format(QUERY + WHERE_FULLNAME, "*", Utils.join(", ", fullName, true));
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            return getPredefinedBranches(rs);
        } catch (SQLException e) {
            Utils.say("Not able to query for multiple alias");
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
     * return 1 if there is match and deleted successfully
     * @param connection MUST BE active connection, also ensure closing connection
     * @param fullName fullName to be deleted
     * @return 0 if no match is found
     */
    public static int deleteFullName(Connection connection, String fullName) {
        List<String> list = new ArrayList<String>(1);
        list.add(fullName);
        return deleteFullName(connection, list);
    }

    /**
     * return number of rows deleted successfully
     * @param connection MUST BE active connection, also ensure closing connection
     * @param fullName list of fullName to be deleted
     * @return 0 if no match is found
     */
    public static int deleteFullName(Connection connection, List<String> fullName) {
        Statement statement = null;
        try {
            final String delete = String.format(DELETE + WHERE_FULLNAME, Utils.join(", ", fullName, true));
            statement = connection.createStatement();
            return statement.executeUpdate(delete);
        } catch (SQLException e) {
            Utils.say("Not able to query for multiple alias");
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
     * return 1 if there is match and deleted successfully
     * @param connection MUST BE active connection, also ensure closing connection
     * @param alias alias to be deleted
     * @return 0 if no match is found
     */
    public static int deleteAlias(Connection connection, String alias) {
        List<String> list = new ArrayList<String>(1);
        list.add(alias);
        return deleteFullName(connection, list);
    }

    /**
     * return number of rows deleted successfully
     * @param connection MUST BE active connection, also ensure closing connection
     * @param alias list of alias to be deleted
     * @return 0 if no match is found
     */
    public static int deleteAlias(Connection connection, List<String> alias) {
        Statement statement = null;
        try {
            final String delete = String.format(DELETE + WHERE_ALIAS, Utils.join(", ", alias, true));
            statement = connection.createStatement();
            return statement.executeUpdate(delete);
        } catch (SQLException e) {
            Utils.say("Not able to query for multiple alias");
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

    private static List<PredefinedBranch> getPredefinedBranches(ResultSet rs) throws SQLException {
        List<PredefinedBranch> results = new ArrayList<PredefinedBranch>();
        while (rs.next()) {
            PredefinedBranch pb = new PredefinedBranch();
            pb.mAlias = rs.getString(Columns.ALIAS);
            pb.mFullName = rs.getString(Columns.FULLNAME);

            results.add(pb);
        }
        return results;
    }
}
