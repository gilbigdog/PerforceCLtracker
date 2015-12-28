package com.ssl.cltracker.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

import com.ssl.cltracker.data.Utils;
import com.ssl.cltracker.database.model.CLRequestHistory;
import com.ssl.cltracker.database.model.CLResult;
import com.ssl.cltracker.database.model.PredefinedBranch;
import com.ssl.cltracker.database.model.Recipients;
import com.ssl.cltracker.database.model.CLResult.BranchTuple;

public class MariaDBHelper {

    private static final String MARIADB_DRIVER = "org.mariadb.jdbc.Driver";

    private static final String DB_SERVER_IP = "105.59.101.166";
    private static final String DB_NAME = "CLTracker";
    private static final String DB_CONNECT_URI =
            "jdbc:mariadb://"
                    + DB_SERVER_IP + "/"
                    + DB_NAME;

    /**
     * Establish DB connection. MUST ensure closing DB at the end.
     */
    public static Connection getConnection(String id, String password) {
        try {
            Class.forName(MARIADB_DRIVER);
            return DriverManager.getConnection(DB_CONNECT_URI, id, password);
        } catch (Exception e) {
            Utils.say("Could not connect to DB");
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] input) {
        // CLRequestHistory Test
        Connection con = getConnection("hyunseok.gil", "welcome!");

        Utils.say("query(5) " + CLRequestHistory.query(con).size());
        Utils.say("insert(1) " + CLRequestHistory.insert(con, new CLRequestHistory(5518858, null, 0)));
    }
}