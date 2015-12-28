package com.ssl.cltracker.database.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ssl.cltracker.data.Utils;

public class CLResult {

    public static final String TABLE = "CLResult";

    public static class Columns {

        public static final String CL = "CL";
        public static final String BRANCHES = "branches";
    }

    // Create
    private static final String CREATE =
            "CREATE TABLE IF NOT EXISTS "
                    + TABLE + " ("
                    + Columns.CL + " MEDIUMINT NOT NULL,"
                    + Columns.BRANCHES + " blob,"
                    + "PRIMARY KEY(" + Columns.CL + ")"
                    + ")";

    // Insert
    private static final String INSERT =
            "INSERT INTO " + TABLE
                    + " values (%s, COLUMN_CREATE(%s))"
                    + " ON DUPLICATE KEY UPDATE "
                    + Columns.BRANCHES + "= COLUMN_CREATE(%s)";

    // Where
    private static final String WHERE_CL =
            " WHERE " + Columns.CL + " = ?";
    private static final String WHERE_BRANCH =
            " WHERE COLUMN_EXISTS(" + Columns.BRANCHES + ", '%s')";
    private static final String WHERE_BRANCH_STATE =
            " WHERE COLUMN_GET(" + Columns.BRANCHES + ", '%s' as CHAR) = %d";

    // Query
    private static final String QUERY =
            "SELECT " + Columns.CL + ", " + "COLUMN_JSON(" + Columns.BRANCHES + ")"
                    + " FROM " + TABLE;
    private static final String QUERY_CL = QUERY + WHERE_CL;
    private static final String QUERY_BRANCH = QUERY + WHERE_BRANCH;
    private static final String QUERY_BRANCH_STATE = QUERY + WHERE_BRANCH_STATE;

    /**
     * Must append WHERE CLAUSE
     */
    private static final String DELETE =
            "DELETE FROM " + TABLE;
    private static final String DELETE_CL = DELETE + WHERE_CL;
    private static final String DELETE_BRANCH = DELETE + WHERE_BRANCH;
    private static final String DELETE_BRANCH_IN_CL =
            DELETE + WHERE_BRANCH
                    + " AND " + Columns.CL + " = %s";

    public static class BranchTuple {

        public String branchName;
        public int state;
        public List<String> partialFiles;  //when state == 2, add files that are ONLY present on that branch 

        public BranchTuple(String bn, int state) {
            this.branchName = bn;
            this.state = state;
        }
        public BranchTuple(String bn, int state, String[] pfs) {
            this.branchName = bn;
            this.state = state;
            this.partialFiles =  Arrays.asList(pfs);
        }
        @Override
        public String toString() {
            return "'" + branchName + "', " + state;
        }
    }

    public int mCL;
    public int mSize;
    public List<BranchTuple> branches;

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
     * Insert/update CL with branch states
     * @param connection MUST BE active connection, also ensure closing connection
     * @param clr CLResult
     * @return number of row updated/inserted, 1 or 0
     */
    public static int insert(Connection connection, CLResult clr) {
        Statement statement = null;

        try {
            final String updatedBranches = Utils.join(", ", clr.branches, false);
            final String insert = String.format(INSERT, clr.mCL, updatedBranches, updatedBranches);
            statement = connection.createStatement();
            return statement.executeUpdate(insert);
        } catch (SQLException e) {
            Utils.say("Not able to insert for CL : " + clr.mCL);
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
     * It is NOT RECOMMNEDED! USER {@link #insert(Connection, CLResult)} as soon as <p>
     * single result is ready
     * @param connection MUST BE active connection, also ensure closing connection
     * @param clrs list of CLResult
     * @return number of row updated/inserted
     */
    public static int insertAll(Connection connection, List<CLResult> clrs) {
        int count = 0;
        for (CLResult clr : clrs) {
            count += insert(connection, clr);
        }

        return count;
    }

    /**
     * return List of CLResult for all rows in table 
     * @param connection MUST BE active connection, also ensure closing connection
     * @return null if not exist
     */
    public static List<CLResult> query(Connection connection) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            statement = connection.createStatement();
            rs = statement.executeQuery(QUERY);
            List<CLResult> results = new ArrayList<CLResult>();
            while (rs.next()) {
                CLResult result = getAsCLResult(rs);

                results.add(result);
            }
            return results;
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
     * return CLResult object with CL and corresponding branch list if exist
     * @param connection MUST BE active connection, also ensure closing connection
     * @param cl changelist
     * @return null if not exist
     */
    public static CLResult query(Connection connection, int cl) {
        ResultSet rs = null;
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(QUERY_CL);
            statement.setInt(1, cl);
            rs = statement.executeQuery();
            while (rs.next()) {
                return getAsCLResult(rs);
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
     * return all CLResult rows that has given branches in column
     * @param connection MUST BE active connection, also ensure closing connection
     * @param branches list of abbreviation of full branch path
     * @return null if not exist
     */
    public static List<CLResult> query(Connection connection, List<String> branches) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String queryBranch = String.format(QUERY_BRANCH, Utils.join(", ", branches, false));
            statement = connection.createStatement();
            rs = statement.executeQuery(queryBranch);

            List<CLResult> results = new ArrayList<CLResult>();
            while (rs.next()) {
                CLResult result = getAsCLResult(rs);

                results.add(result);
            }
            return results;
        } catch (SQLException e) {
            Utils.say("Not able to query for branches");
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
     * return CLResult row that has given branch in column
     * @param connection MUST BE active connection, also ensure closing connection
     * @param branch abbreviation of full branch path
     * @return null if not exist
     */
    public static List<CLResult> query(Connection connection, String branch) {
        List<String> list = new ArrayList<String>(1);
        list.add(branch);

        return query(connection, list);
    }

    /**
     * return all CLResult rows that has given state of given branch
     * @param connection MUST BE active connection, also ensure closing connection
     * @param branch abbreviation of full branch path
     * @param state state of given branch
     * @return null if not exist
     */
    public static List<CLResult> query(Connection connection, String branch, int state) {
        ResultSet rs = null;
        Statement statement = null;
        try {
            final String queryBranch = String.format(QUERY_BRANCH_STATE, branch, state);
            statement = connection.createStatement();
            rs = statement.executeQuery(queryBranch);

            List<CLResult> results = new ArrayList<CLResult>();
            while (rs.next()) {
                CLResult result = getAsCLResult(rs);

                results.add(result);
            }
            return results;
        } catch (SQLException e) {
            Utils.say("Not able to query for " + branch);
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
     * Delete a row having given CL
     * @param connection MUST BE active connection, also ensure closing connection
     * @param cl changelist to be deleted
     * @param join delete a row from both this table and {@link CLRequestHistory}
     * @return int number of updates
     */
    public static int delete(Connection connection, int cl, boolean join) {
        if (join) {
            return CLRequestHistory.delete(connection, cl, true);
        } else {
            PreparedStatement statement = null;
            try {
                statement = connection.prepareStatement(DELETE_CL);
                statement.setInt(1, cl);
                return statement.executeUpdate();
            } catch (SQLException e) {
                Utils.say("Not able to delete for " + cl);
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

    /**
     * delete columns matched to given branch from all rows
     * @param connection MUST BE active connection, also ensure closing connection
     * @param branch list of abbreviation of full branch path
     * @return number of updated rows
     */
    public static int delete(Connection connection, List<String> branch) {
        Statement statement = null;
        try {
            final String deleteBranch = String.format(DELETE_BRANCH, Utils.join(", ", branch, false));
            statement = connection.createStatement();
            return statement.executeUpdate(deleteBranch);
        } catch (SQLException e) {
            Utils.say("Not able to delete for list");
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
     * delete column matched to given branch from all rows
     * @param connection MUST BE active connection, also ensure closing connection
     * @param branch single abbreviation of full branch path
     * @return number of updated row
     */
    public static int delete(Connection connection, String branch) {
        List<String> list = new ArrayList<String>(1);
        list.add(branch);

        return delete(connection, list);
    }

    /**
     * delete rows that has given cl AND given branches
     * @param connection MUST BE active connection, also ensure closing connection
     * @param branch list of abbreviation of full branch path
     * @param cl changelist to be deleted
     * @return number of row changes
     */
    public static int delete(Connection connection, List<String> branch, int cl) {
        Statement statement = null;
        try {
            final String deleteBranch = String.format(DELETE_BRANCH_IN_CL, Utils.join(", ", branch, false), cl);
            statement = connection.createStatement();
            return statement.executeUpdate(deleteBranch);
        } catch (SQLException e) {
            Utils.say("Not able to delete for list in CL " + cl);
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
     * delete rows that has given cl AND given branch
     * @param connection MUST BE active connection, also ensure closing connection
     * @param branch single abbreviation of full branch path
     * @param cl changelist to be deleted
     * @return 0 or 1
     */
    public static int delete(Connection connection, String branch, int cl) {
        List<String> list = new ArrayList<String>(1);
        list.add(branch);

        return delete(connection, list, cl);
    }

    private static CLResult getAsCLResult(ResultSet rs) throws SQLException {
        // collect cl
        final int cl = rs.getInt(Columns.CL);

        // collect list of branch
        final String branches = rs.getString(2);
        List<BranchTuple> bt = getBranchTuple(branches);

        // create object
        CLResult result = new CLResult();
        result.branches = bt;
        result.mCL = cl;
        return result;
    }

    private static List<BranchTuple> getBranchTuple(final String branches) {
        List<BranchTuple> bt = new ArrayList<BranchTuple>();
        JsonElement je = new JsonParser().parse(branches);
        for (Entry<String, JsonElement> ele : je.getAsJsonObject().entrySet()) {
            bt.add(new BranchTuple(ele.getKey(), ele.getValue().getAsInt()));
        }
        return bt;
    }
}
