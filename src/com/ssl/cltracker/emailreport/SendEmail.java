package com.ssl.cltracker.emailreport;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.ssl.cltracker.Commander;
import com.ssl.cltracker.Commander.ServiceDelegator;
import com.ssl.cltracker.data.Utils;
import com.ssl.cltracker.database.MariaDBHelper;
import com.ssl.cltracker.database.model.CLRequestHistory;
import com.ssl.cltracker.database.model.CLResult;
import com.ssl.cltracker.database.model.PredefinedBranch;
import com.ssl.cltracker.database.model.Recipients;
import com.ssl.cltracker.database.model.CLResult.BranchTuple;
import com.ssl.cltracker.service.CLTrackerService;

public class SendEmail implements ServiceDelegator {

    public static final String TAG = "email";

    private static final String EMAIL_TITLE = "[CLTracker] Daily report for ";
    private static final String EMAIL_CONTENT_TITLE = "This is daily report as of now.";
    private static final String CONTENT_TYPE = "text/html; charset=utf-8";
    private static final String SMTP_SERVER = "localhost";

    @Override
    public void run(String[] args) {
        if (args.length != 5) {
            Utils.say("Argument should look like below");
            Utils.say("USAGE : email <db id> <db password> <perforce id> <perforce passwd>");
        }

        // get All CL list from DB
        Connection con = MariaDBHelper.getConnection(args[1], args[2]);
        List<CLRequestHistory> reuqesthistory = CLRequestHistory.query(con);
        if (reuqesthistory != null) {
            List<String> clCommands = new ArrayList<String>(reuqesthistory.size());

            // service command set
            clCommands.add(CLTrackerService.TAG);
            clCommands.add(args[3]);
            clCommands.add(args[4]);

            // add all CLs
            for (CLRequestHistory history : reuqesthistory) {
                clCommands.add(String.valueOf(history.mCL));
            }

            // run service command
            String[] commandInArray = clCommands.toArray(new String[] {});
            Commander.main(commandInArray);
        } else {
            Utils.say("No request history is available. Done!");
            return;
        }

        // Recipient's email ID needs to be mentioned.
        sendEmail(con, Recipients.query(con));
    }

    public static void sendEmail(Connection con, List<String> to) {
        Properties properties = System.getProperties();

        // Setup mail server
        properties.setProperty("mail.smtp.host", SMTP_SERVER);

        // Get the default Session object.
        Session session = Session.getDefaultInstance(properties);

        // Create a default MimeMessage object.
        MimeMessage msg = new MimeMessage(session);

        // Set To: header field of the header.
        try {
            msg.setSubject(EMAIL_TITLE + Utils.getDateInString());
            for (String recipient : to) {
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(recipient));
            }
            msg.setContent(generateTable(con), CONTENT_TYPE);
            msg.setSentDate(new Date());
            Transport.send(msg);
            Utils.say("Sent message successfully....");
        } catch (javax.mail.MessagingException e) {
            Utils.say("Fail to send message");
        }

        try {
            con.close();
        } catch (SQLException e) {
            Utils.say("Having issue to close DB connection, but keep continue");
        }
    }

    private static class HtmlProperties {

        public static class Table {

            private static final int BORDER_SIZE = 2;
            private static final int CELL_PADDING = 5;
            private static final int CELL_SPACE = 0;
            private static final String ALIGN = "center";

            private static class Style {

                private static final int FONT_SIZE_TITLE = 10;
                private static final String FONT_FMAILY_TITLE = "Tahoma, Geneva, sans-serif";
                private static final String FONT_WEIGHT_TITLE = "bold";
                private static final String FONT_COLOR_TITLE = "#666";
                private static final String FONT_STYLE_TITLE =
                        "<p style=\"font-family: " + FONT_FMAILY_TITLE
                                + "; font-weight:  " + FONT_WEIGHT_TITLE
                                + "; color: " + FONT_COLOR_TITLE
                                + "; font-size: " + FONT_SIZE_TITLE
                                + "\">";

                private static final int FONT_SIZE_RESULT = 8;
                private static final String FONT_FMAILY_RESULT = "Tahoma, Geneva, sans-serif";
                private static final String FONT_WEIGHT_RESULT = "bold";
                private static final String FONT_COLOR_RESULT = "black";
                private static final String FONT_COLOR_RESULT_CAUTION = "red";
                private static final String FONT_STYLE_RESULT =
                        "<p style=\"font-family: " + FONT_FMAILY_RESULT
                                + "; font-weight:  " + FONT_WEIGHT_RESULT
                                + "; color: %s" // Insert later
                                + "; font-size: " + FONT_SIZE_RESULT
                                + "\">";

                private static final String FONT_STYLE_END = "</p>";
            }

            public static StringBuilder getColumnProperty() {
                StringBuilder sb = new StringBuilder();

                sb.append("<table ");
                sb.append("border=" + HtmlProperties.Table.BORDER_SIZE);
                sb.append(" cellpadding=" + HtmlProperties.Table.CELL_PADDING);
                sb.append(" cellspace=" + HtmlProperties.Table.CELL_SPACE);
                sb.append(">");

                return sb;
            }

            public static String wrapColumnTitle(Object title) {
                return "<th align=" + ALIGN + ">"
                        + Style.FONT_STYLE_TITLE
                        + title
                        + Style.FONT_STYLE_END
                        + "</th>";
            }

            public static String wrapRow(Object item, String color) {
                return "<td align=" + ALIGN + ">"
                        + String.format(Style.FONT_STYLE_RESULT, color)
                        + item
                        + Style.FONT_STYLE_END
                        + "</td>";
            }
        }

        public static class Title {

            private static final int HEADER_SIZE = 3;
            private static final String FONT_COLOR = "black";

            public static String wrapTitle(String title) {
                return "<h" + HEADER_SIZE + "><div style=\""
                        + "color:" + FONT_COLOR
                        + ";\">" + title
                        + "</div>"
                        + "</h" + HEADER_SIZE + ">";
            }
        }
    }

    public static String generateTable(Connection con) {
        List<PredefinedBranch> branches = PredefinedBranch.query(con);
        List<CLResult> clResult = CLResult.query(con);
        List<CLRequestHistory> clTime = CLRequestHistory.query(con);

        // Header
        StringBuilder sb = new StringBuilder();
        sb.append("<html>");
        sb.append("<head>");
        sb.append(HtmlProperties.Title.wrapTitle(EMAIL_CONTENT_TITLE));
        sb.append("</head>");

        // Table Setting
        sb.append(HtmlProperties.Table.getColumnProperty());

        // Add title of column
        sb.append(HtmlProperties.Table.wrapColumnTitle("CL"));
        for (PredefinedBranch branch : branches) {
            sb.append(HtmlProperties.Table.wrapColumnTitle(branch.mAlias));
        }
        sb.append(HtmlProperties.Table.wrapColumnTitle("Last Query"));

        // Add CL Rows
        final Map<Integer, Map<String, Integer>> clResultMap = converResultToMap(clResult);
        final Map<Integer, Timestamp> clTimeMap = converTimeToMap(clTime);
        for (Entry<Integer, Map<String, Integer>> cl : clResultMap.entrySet()) {
            sb.append("<tr>");
            sb.append(HtmlProperties.Table.wrapRow(cl.getKey(), HtmlProperties.Table.Style.FONT_COLOR_RESULT));

            final Map<String, Integer> branchResult = cl.getValue();
            for (PredefinedBranch branch : branches) {
                final int state = branchResult.get(branch.mAlias);
                String stateInString = "O";
                String color = HtmlProperties.Table.Style.FONT_COLOR_RESULT;
                if (state != 1) {
                    stateInString = "X";
                    color = HtmlProperties.Table.Style.FONT_COLOR_RESULT_CAUTION;
                }
                sb.append(HtmlProperties.Table.wrapRow(stateInString, color));
            }
            sb.append(HtmlProperties.Table.wrapRow(clTimeMap.get(cl.getKey()),
                    HtmlProperties.Table.Style.FONT_COLOR_RESULT));
            sb.append("</tr>");
        }

        sb.append("</table>");
        sb.append("</html>");

        return sb.toString();
    }

    private static Map<Integer, Timestamp> converTimeToMap(List<CLRequestHistory> clTime) {
        Map<Integer, Timestamp> res = new HashMap<Integer, Timestamp>();
        for (CLRequestHistory cl : clTime) {
            res.put(cl.mCL, cl.mTimeStamp);
        }
        return res;
    }

    private static Map<Integer, Map<String, Integer>> converResultToMap(List<CLResult> clResult) {
        Map<Integer, Map<String, Integer>> res = new HashMap<Integer, Map<String, Integer>>(clResult.size());

        for (CLResult cl : clResult) {
            Map<String, Integer> clMap = new HashMap<String, Integer>(cl.branches.size());
            for (BranchTuple branch : cl.branches) {
                clMap.put(branch.branchName, branch.state);
            }

            res.put(cl.mCL, clMap);
        }

        return res;
    }
}