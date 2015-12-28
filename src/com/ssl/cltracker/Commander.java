package com.ssl.cltracker;

import com.ssl.cltracker.data.Utils;
import com.ssl.cltracker.emailreport.SendEmail;
import com.ssl.cltracker.service.CLTrackerService;

public class Commander {

    public interface ServiceDelegator {

        public void run(String[] args);
    }

    /**
     * Showing example of how CLService should be used
     */
    public static void main(String[] args) {

        if (args.length < 1) {
            Utils.say("Input must include at least one");
        }

        ServiceDelegator del = getDelegator(args[0]);
        if (del == null) {
            Utils.say("Cannot find option : " + args[0]);
            return;
        }

        Utils.say("Start service name " + args[0]);
        del.run(args);
        Utils.say("Done!");
    }

    private static ServiceDelegator getDelegator(String option) {
        switch (option) {
            case CLTrackerService.TAG:
                return new CLTrackerService();
            case SendEmail.TAG:
                return new SendEmail();
        }

        return null;
    }
}