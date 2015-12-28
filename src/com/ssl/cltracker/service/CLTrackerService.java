package com.ssl.cltracker.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConfigException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.NoSuchObjectException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.exception.ResourceException;
import com.perforce.p4java.impl.mapbased.rpc.RpcPropertyDefs;
import com.perforce.p4java.server.CmdSpec;
import com.perforce.p4java.server.IServer;
import com.perforce.p4java.server.ServerFactory;
import com.ssl.cltracker.Commander.ServiceDelegator;
import com.ssl.cltracker.data.BranchTree;
import com.ssl.cltracker.data.FileNode;
import com.ssl.cltracker.data.PerforceBranchTree;
import com.ssl.cltracker.data.Utils;
import com.ssl.cltracker.database.MariaDBHelper;
import com.ssl.cltracker.database.model.CLRequestHistory;
import com.ssl.cltracker.database.model.CLResult;
import com.ssl.cltracker.database.model.PredefinedBranch;

public class CLTrackerService implements CLTrackerConstants, ServiceDelegator {

    public static final String TAG = "service";

    /**
     * <service> <db id> <db pwd> <p4 id> <p4 wd> <cl> ...
     */
    private static final int MIN_ARGS_INPUT = 6;
    private static final String SOCKET_TIMEOUT = "300000";
    private static IServer mP4Server;

    @Override
    public void run(String[] args) {

        // TODO : validate args to prevent exceptions

        /**
         * Make an instance of CLService object
         * @param: String ID, String password of port 1716
         */
        mP4Server = connect(args[3], args[4]);

        /**
         * CL multithreading
         */
        long start = System.currentTimeMillis();

        /**
         * initialize Thread Pool and
         * Determine number of thread to avoid deadlock from thread starvation
         * TODO : find optimal number of threads based on each results with various CLs
         */
        final int procs = Runtime.getRuntime().availableProcessors();
        final int clNum = args.length - MIN_ARGS_INPUT + 1;
        ForkJoinPool pool = new ForkJoinPool(Math.max(procs, clNum) * 2);

        /**
         * Submit cl task and collect ForkJoinTask to get result later
         */
        Connection con = MariaDBHelper.getConnection(args[1], args[2]);
        Map<Integer, ForkJoinTask<FileNode>> clTasks = new HashMap<Integer, ForkJoinTask<FileNode>>(clNum);
        for (int i = MIN_ARGS_INPUT - 1; i < args.length; i++) {
            CLTask task = new CLTask(con, Integer.valueOf(args[i]));
            clTasks.put(task.mCL, pool.submit(task));
        }

        /**
         * get result fileNode by waiting each threads
         */
        List<FileNode> taskResult = new ArrayList<FileNode>(clNum);
        for (Entry<Integer, ForkJoinTask<FileNode>> task : clTasks.entrySet()) {
            try {
                taskResult.add(task.getValue().get());
            } catch (InterruptedException | ExecutionException e) {
                Utils.say("task.get exception for CL : " + task.getKey());
                taskResult.add(new FileNode(null, task.getKey(), -1));
                e.printStackTrace();
            }
        }
        Utils.say("END all WORK for CL full thread " + (System.currentTimeMillis() - start));

        /**
         * Converts the tree to the CL Object for tale.
         */
        List<ForkJoinTask<Void>> dbTasks = new ArrayList<ForkJoinTask<Void>>(clNum);
        for (FileNode result : taskResult) {
            UpdateDB task = new UpdateDB(con, result);
            dbTasks.add(pool.submit(task));
        }

        /**
         * wait completion of DB update
         */
        for (ForkJoinTask<Void> task : dbTasks) {
            try {
                task.get();
            } catch (InterruptedException | ExecutionException e) {
                Utils.say("dbTasks.get exception");
                e.printStackTrace();
            }
        }
        try {
            con.close();
        } catch (SQLException e) {
            Utils.say("fail to close DB connection");
        }
    }

    public CLTrackerService() {
        // do nothing
    }

    public static IServer connect(String id, String password) {
        try {
            IServer server = connect(P4_1716_ADDRESS, id, password);
            Utils.say("p4: CONNECTED TO " + P4_1716_ADDRESS);
            return server;
        } catch (ConnectionException | NoSuchObjectException | ConfigException
                | ResourceException | URISyntaxException | AccessException | RequestException e) {
            Utils.say("p4: ERROR when connecting");
            e.printStackTrace();
        }
        return null;
    }

    public static IServer connect(String connectionString, String userName, String password)
            throws ConnectionException, NoSuchObjectException, ConfigException, ResourceException,
            URISyntaxException, AccessException, RequestException {
        // Set timeout
        Properties prop = new Properties();
        prop.setProperty(RpcPropertyDefs.RPC_SOCKET_SO_TIMEOUT_NICK, SOCKET_TIMEOUT);

        IServer server = ServerFactory.getServer(connectionString, prop);
        server.connect();
        server.setUserName(userName);
        server.login(password);
        return server;
    }

    /**
     * initialize cl information to start tree traverse
     */
    private class CLTask extends RecursiveTask<FileNode> {

        private int mCL;
        private Connection mCon;

        public CLTask(Connection con, int cl) {
            mCon = con;
            mCL = cl;
        }

        @Override
        protected FileNode compute() {
            long start = System.currentTimeMillis();
            Utils.say("CLTask(" + mCL + ") : " + Thread.currentThread().getName() + " START!");

            //Insert History Table first
            CLRequestHistory history = new CLRequestHistory(mCL, null, 0);
            CLRequestHistory.insert(mCon, history);

            // create a root first
            FileNode root = new FileNode("root", mCL, -1);
            // load CL's files
            List<FileNode> firstParents = generateFileList(mCL);
            // build parent-children relationship
            root.setChildren(firstParents);

            List<FormTree> treeTasks = new ArrayList<FormTree>();
            for (FileNode n : firstParents) {
                // setup the visited set and queue for each file from CL
                Set<String> visitedBranch = new HashSet<String>();
                Queue<FileNode> queue = new LinkedList<FileNode>();
                visitedBranch.add(n.getFileName());
                for (FileNode child : n.getChildren()) {
                    visitedBranch.add(child.getFileName());
                    queue.add(child);
                }
                // create thread per file and execute
                FormTree ft = new FormTree(queue, visitedBranch);
                ft.fork();
                treeTasks.add(ft);
            }

            for (FormTree ft : treeTasks) {
                ft.join();
            }

            Utils.say("CLTask(" + mCL + ") : " + Thread.currentThread().getName() + " FINISHED! Running FOR: "
                    + (System.currentTimeMillis() - start));
            return root;
        }

        private List<FileNode> generateFileList(int changeList) {
            // returns array of map containing file info based on changeList
            final Map<String, Object>[] maps = runFilesCmd(changeList);

            List<FileNode> filesFromCL = new ArrayList<FileNode>();
            for (Map<String, Object> map : maps) {
                // file information of files from CL = parents
                String ppath = getPathFromMap(map, KEY_FILE_FULLPATH);
                int prev = getRevFromMap(map, KEY_FILE_REVISION);
                FileNode fileFromCL = new FileNode(ppath, changeList, prev);
                // get children of files from CL
                List<FileNode> children = parseFileChildren(map);
                fileFromCL.setChildren(children);
                filesFromCL.add(fileFromCL);
            }
            return filesFromCL;
        }
    }

    /**
     * Traverse filenode in queue based on integration history
     */
    private class FormTree extends RecursiveAction {

        private Queue<FileNode> mQueue;
        private Set<String> mVisitedBranch;

        public FormTree(Queue<FileNode> queue, Set<String> visitedBranch) {
            mQueue = queue;
            mVisitedBranch = visitedBranch;
        }

        @Override
        protected void compute() {
            // // Utils.say("FormTree : " + Thread.currentThread().getName() +
            // " START!");
            // long start = System.currentTimeMillis();
            generateRecursive(mQueue, mVisitedBranch);
            // Utils.say("FormTree : " + Thread.currentThread().getName() +
            // " FINISHED! Running FOR: "
            // + (System.currentTimeMillis() - start));
        }

        private void generateRecursive(Queue<FileNode> queue, Set<String> visitedBranch) {
            while (!queue.isEmpty()) {
                FileNode fn = queue.poll();
                List<FileNode> list = null;
                try {
                    list = merge(fn);
                } catch (Exception e) {
                    Utils.say(Thread.currentThread().getName() + " exception occurred when merging "
                            + fn.getFileName());
                }
                for (FileNode n : list) {
                    if (!visitedBranch.contains(n.getFileName())) {
                        // mark ppath as visited - add into mVisitedPaths
                        visitedBranch.add(n.getFileName());
                        n.setParent(fn);
                        fn.addChild(n);
                        queue.add(n);
                    }
                }
            }
        }

        public List<FileNode> merge(FileNode fn) {
            // raw version of output from p4 "integrate" command
            Map<String, Object>[] rawOutput = runIntegratedCmd(fn.getFileName());
            // create an editedOutput from rawOutput
            /*
             * e.g. editedOutput: fn' rev# Integrated File Integrated rev# 1
             * //A/B/C 10 //D/E/G 6 //H/I/J 3 2 //A/B/C 5 //D/E/F 80
             */
            Map<Integer, Map<String, Integer>> editedOutput = new HashMap<Integer, Map<String, Integer>>(
                    fn.getRevision().size());
            // initialization
            for (Integer rev : fn.getRevision()) {
                editedOutput.put(rev, new HashMap<String, Integer>());
            }
            // now, do the parsing from rawOutput to editedOutput
            for (Map<String, Object> rawMap : rawOutput) {
                int startRev = 0;
                int endRev = 0;
                try {
                    startRev = getRevisionNumber((String) rawMap.get(KEY_INTEGRATED_START_TO_REVISION));
                    endRev = getRevisionNumber((String) rawMap.get(KEY_INTEGRATED_END_TO_REVISION));
                } catch (Exception e) {
                    // Utils.say(Thread.currentThread().getName() +
                    // " perhaps an access to "
                    // + fn.getFileBranch() + " is denied");
                }
                for (Integer rev : fn.getRevision()) {
                    // if the revision of the fn is in between startRev and
                    // endRev,
                    // then this means the file we found from rawOutput is
                    // integrated from fn
                    if (startRev < rev && rev <= endRev) {
                        Map<String, Integer> branches = editedOutput.get(rev);
                        final int fromRev = getRevisionNumber((String) rawMap
                                .get(CLTrackerConstants.KEY_INTEGRATED_END_FROM_REVISION));
                        branches.put((String) rawMap.get(CLTrackerConstants.KEY_INTEGRATED_FROMFILE),
                                fromRev);
                    }
                }
            }

            /*
             * Next, if fn contains multiple revisions, each entry from the
             * revision must be compared w/ other revision's entry. So that we
             * only consider files from all revisions of fn.
             */

            // picking a map w/ smallest size for a comparison pivot.
            // Returns its rev # = key of editedOutput
            final int pivotRevision = pickPivot(editedOutput);

            // Return a map of from editedOutput, which satisfies the above
            // condition
            /*
             * e.g. editedMap: File Name its revisions a/b/c/ 1 e/d/f/ 1,2,3
             * g/h/i/ 4,8,23
             */
            Map<String, List<Integer>> editedMap = mergeAllRevs(pivotRevision, editedOutput);

            // finally return list of files that's considered to be integrated
            // from fn.
            List<FileNode> editedList = convertMergedToFileNode(editedMap);
            return editedList;
        }
    }

    private class UpdateDB extends RecursiveAction {

        private Connection mCon;
        private FileNode mResult;

        public UpdateDB(Connection con, FileNode filenode) {
            mCon = con;
            mResult = filenode;
        }

        @Override
        protected void compute() {
            // Insert CL result only if result is available
            if (mResult.getFileName() != null) {
                CLResult result = new CLResult();
                result.mCL = mResult.getCL();
                result.branches = compare(mResult, PredefinedBranch.query(mCon));
                CLResult.insert(mCon, result);
            }

            // update history
            CLRequestHistory history = new CLRequestHistory(mResult.getCL(), null, 1);
            if (mResult.getFileName() == null) {
                Utils.say("ERROR");
                history.mState = 0;
            }
            CLRequestHistory.insert(mCon, history);
        }
    }

    private static Map<String, Object> getIntegratedFiles(String path, int revision) {
        String optionParam = path + "#" + String.valueOf(revision);
        String[] options = {
                "-m", "1", optionParam
        };
        return execCmd(CmdSpec.FILELOG.toString(), options)[0];
    }

    private static List<FileNode> parseFileChildren(Map<String, Object> map) {
        List<FileNode> children = new ArrayList<FileNode>();
        // calculate how many children exists
        int childrenNumber = (map.size() - MAP_NON_DESCENDANT_KEYS) / MAP_DESCENDANT_KEYSET;
        Map<String, FileNode> visitedChildren = new HashMap<String, FileNode>();
        for (int i = 0; i < childrenNumber; i++) {
            String cpath = getPathFromMap(map, KEY_DESCENDANT_FILE_FULLPATH + i);
            FileNode child;
            if (visitedChildren.containsKey(cpath)) {
                // if cpath already exists, that means revisions should be
                // merged
                child = visitedChildren.get(cpath);
            } else {
                child = new FileNode(cpath);
            }
            int startRev = getRevFromMap(map, KEY_DESCENDANT_FILE_REVISION_START + i);
            int endRev = getRevFromMap(map, KEY_DESCENDANT_FILE_REVISION_END + i);
            for (int rev = endRev; rev > startRev; rev--) {
                child.addRevision(rev);
            }
            visitedChildren.put(cpath, child);
        }
        for (Entry<String, FileNode> entry : visitedChildren.entrySet()) {
            children.add(entry.getValue());
        }
        return children;
    }

    private static String getPathFromMap(Map<String, Object> map, String key) {
        return (String) map.get(key);
    }

    private static int getRevFromMap(Map<String, Object> map, String key) {
        final String value = (String) map.get(key);
        return getRevisionNumber(value);
    }

    private static int getRevisionNumber(String revision) {
        String substring = revision;
        if ('#' == substring.charAt(0)) {
            substring = revision.substring(1);
        }
        if ("none".equals(substring)) {
            return 0;
        }
        return Integer.parseInt(substring);
    }

    private static Map<String, Object>[] runFilesCmd(int changelist) {
        // P4 COMMAND: p4 -p 105.59.102.30:1716 filelog -c <CL> -m 1 //...
        String options[] = {
                "-c", String.valueOf(changelist), "-m", "1", "//..."
        };
        return execCmd(CmdSpec.FILELOG.toString(), options);
    }

    private Map<String, Object>[] runIntegratedCmd(String path) {
        // P4 COMMAND: p4 -p 105.59.102.30:1716 integrated <path>
        return execCmd(CmdSpec.INTEGRATED.toString(), path);
    }

    private static Map<String, Object>[] execCmd(String cmdType, String... options) {
        try {
            return mP4Server.execInputStringMapCmd(cmdType, options, null);
        } catch (P4JavaException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static List<FileNode> convertMergedToFileNode(Map<String, List<Integer>> merged) {
        List<FileNode> nodes = new ArrayList<FileNode>();
        for (Entry<String, List<Integer>> mergedListEntry : merged.entrySet()) {
            FileNode nd = new FileNode();
            nd.setFileName(mergedListEntry.getKey());
            // now, revision part - don't include duplicates
            for (Integer rev : mergedListEntry.getValue()) {
                nd.addRevision(rev);
            }
            nodes.add(nd);
        }
        return nodes;
    }

    private static Map<String, List<Integer>> mergeAllRevs(int pivotRevNum,
            Map<Integer, Map<String, Integer>> fileMap) {
        // first, from fileMap, pick the map w/ the smallest size
        Map<String, Integer> pivotMap = fileMap.get(pivotRevNum);

        // remove the entry w/ pivotRevNum from fileMap
        if (fileMap.size() > 1) {
            fileMap.remove(pivotRevNum);
        }

        Map<String, List<Integer>> merged = new HashMap<String, List<Integer>>();

        for (Entry<Integer, Map<String, Integer>> entries : fileMap.entrySet()) {
            // entryMap containing non-pivot map's entries
            Map<String, Integer> entryMap = entries.getValue();

            // traverse each path and its revision from pivot map
            for (Entry<String, Integer> pivotEntry : pivotMap.entrySet()) {
                final String pivotEntryPath = pivotEntry.getKey();
                // if entryMap's key (path) matches with pviot's key (path)
                if (entryMap.containsKey(pivotEntryPath)) {
                    // make a list of integers - and add revision# to list of
                    // integers - then put it to mergedMap
                    List<Integer> revisionList = null;
                    // check to see if mergedMap already contains
                    // entryMap's key (path)
                    if (merged.containsKey(pivotEntryPath)) {
                        // if exists, add revision# to list of integers
                        revisionList = merged.get(pivotEntryPath);
                    } else {
                        // if DNE, make a new list and add it to mergedList
                        revisionList = new ArrayList<Integer>();
                        // also, add pivot's value's revision#
                        revisionList.add(pivotEntry.getValue());
                    }
                    final int matchedRevNumber = entryMap.get(pivotEntryPath);
                    revisionList.add(matchedRevNumber);
                    merged.put(pivotEntryPath, revisionList);
                }
            }
        }

        // Next remove entries from the mergedMap that's samller than
        // hasMap.size() [Original Size]
        final int fileMapSize = 1 + fileMap.size();
        // Utils.say("fileMapSize - " + fileMapSize);

        List<String> toRemove = new ArrayList<String>();
        for (Entry<String, List<Integer>> mergedListEntry : merged.entrySet()) {
            final int listSize = mergedListEntry.getValue().size();
            if (fileMapSize != listSize) {
                toRemove.add(mergedListEntry.getKey());
            }
        }
        for (String remove : toRemove) {
            merged.remove(remove);
        }
        return merged;
    }

    private static int pickPivot(Map<Integer, Map<String, Integer>> map) {
        int localMin = Integer.MAX_VALUE;
        int revision = -1;
        for (Entry<Integer, Map<String, Integer>> entry : map.entrySet()) {
            Map<String, Integer> mapFromPivot = entry.getValue();
            if (localMin > mapFromPivot.size()) {
                localMin = mapFromPivot.size();
                revision = entry.getKey();
            }
        }
        return revision;
    }

    private static List<CLResult.BranchTuple> compare(FileNode root, List<PredefinedBranch> branches) {
        Queue<FileNode> q = new LinkedList<FileNode>();
        BranchTree<FileNode> bt = new PerforceBranchTree<>("root");

        if (root.getChildren() == null) {
            return null;
        }

        int filesInCL = 0;
        for (FileNode child : root.getChildren()) {
            q.add(child);
            filesInCL++;
        }

        while (!q.isEmpty()) {
            FileNode node = q.poll();
            bt.put(node.getFileName(), node);
            if (null != node.getChildren()) {
                for (FileNode nd : node.getChildren()) {
                    q.add(nd);
                }
            }
        }

        List<CLResult.BranchTuple> brancheOfCLResult = new ArrayList<CLResult.BranchTuple>();
        for (PredefinedBranch pb : branches) {
            int state = 0;
            String[] leaves = null;
            if (bt.containsPartial(pb.mFullName)) {
                state = 1;
                leaves = bt.getLeaves(pb.mFullName);
                int leafCount = leaves.length;
                if( filesInCL != leafCount) {
                    state = 2;
                }
            }
            if (2== state) {
                brancheOfCLResult.add(new CLResult.BranchTuple(pb.mAlias, state, leaves));
            } else {
                brancheOfCLResult.add(new CLResult.BranchTuple(pb.mAlias, state));
            }
        }

        return brancheOfCLResult;
    }

    public static void printTree(FileNode root, String index) throws IOException {
        File fout = new File("output_" + index + ".txt");
        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
        Queue<FileNode> queue = new LinkedList<FileNode>();
        queue.add(root);
        while (!queue.isEmpty()) {
            FileNode node = queue.poll();
            /*
             * Utils.say("===============START===============");
             * Utils.say("FileNode name: " + node.getFileBranch());
             * Utils.say("FileNode CL: " + node.getCL());
             * Utils.say("FileNode revisions: " + node.getRevision());
             * Utils.say("===============END===============");
             */
            bw.write("===============START===============");
            bw.newLine();
            bw.write("FileNode name: " + node.getFileName());
            bw.newLine();
            bw.write("FileNode CL: " + node.getCL());
            bw.newLine();
            bw.write("FileNode revisions: " + node.getRevision());
            bw.newLine();
            bw.write("===============END===============");
            bw.newLine();
            if (null != node.getChildren()) {
                for (FileNode n : node.getChildren()) {
                    queue.add(n);
                }
            }
        }
        bw.close();
    }
}
