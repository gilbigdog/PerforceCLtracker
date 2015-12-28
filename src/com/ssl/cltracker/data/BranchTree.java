package com.ssl.cltracker.data;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.Queue;

/**
 * the format of String MUST BE below <p>
 * "ABC/DEF/ABC" no leading and trailing special characters <p>
 * tag in leaf node must be unique along the path <p>
 * ex) "ABC/DEF" tree cannot contain "ABC/DEF/QWE" <p>
 * {@link BranchTree#getDelimiter()} will be referred to parse and <p>
 * generate string
 * @author hyunseok.gil
 *
 * @param <K>
 */
public abstract class BranchTree<K> {

    /**
     * KEY : tag of child node <p>
     * VALUE : child node
     */
    protected Map<String, BranchTree<K>> child;

    /**
     * it indicates the name of folder at the level, <p> 
     * HOWEVER!!, tag in root node WILL NOT be applicable it, so it should be used
     * only to identify entire tree.
     */
    protected String tag;
    protected boolean isLeaf = false;

    /**
     * for now, only leaf node can have it
     */
    protected K container = null;

    public BranchTree(String node) {
        this.tag = node;
    }

    public BranchTree(String node, Map<String, BranchTree<K>> child) {
        this.child = child;
        this.tag = node;
    }

    /**
     * True if child of current node is empty
     * @return
     */
    public boolean isEmpty() {
        return child == null || child.isEmpty();
    }

    /**
     * set tag
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * Return tag of current node
     */
    public String getTag() {
        return tag;
    }

    public Map<String, BranchTree<K>> getChild() {
        return child;
    }

    /**
     * put Child to child list
     */
    public abstract void putChild(String tag);

    /**
     * format of String must be following <p>
     * ex) "abc/def/1de" <p>
     */
    protected abstract String stripOffStrings(String str);

    /**
     * return delimiter in String
     */
    protected abstract String getDelimiter();

    protected abstract String getPreAppend();

    protected abstract String getPostAppend();

    /**
     * True if successfully add given branch to tree without duplicate <P>
     * container can be null
     */
    public boolean put(String branch, K container) {
        if (branch == null || branch.length() == 0) {
            throw new IllegalArgumentException();
        }

        return addBranch(stripOffStrings(branch).split(getDelimiter()), container);
    }

    /**
     * Construct full BranchTree with full lists of branches <p>
     * containers can be null
     */
    public void putAll(List<String> branches, List<K> containers) {
        if (branches == null || branches.isEmpty()) {
            throw new IllegalArgumentException("branches are empty");
        }

        if (containers != null && containers.size() != branches.size()) {
            throw new IllegalArgumentException("container is not null and size is diff from branch");
        }

        for (int index = 0; index < branches.size(); index++) {
            put(branches.get(index), containers.get(index));
        }
    }

    /**
     * True if current tree contains given branch path
     */
    public boolean containsInFull(String branch) {
        BranchTree<K> node = findLastNode(branch);
        if (node == null) {
            return false;
        }

        return node.isLeaf;
    }

    /**
     * True if current tree contains given branch path partially <p>
     * ex) True => Tree : //A/B/C      branch : //A/B
     */
    public boolean containsPartial(String branch) {
        return findLastNode(branch) != null;
    }

    /**
     * True if S is subTree of T
     */
    public static <K> boolean isSubTree(BranchTree<K> T, BranchTree<K> S) {
        /* base cases */
        if (S == null) {
            return true;
        }

        if (T == null && S != null) {
            return false;
        }

        if (S.child == null) {
            return true;
        }

        if (T.child == null && S.child != null) {
            return false;
        }

        for (Entry<String, BranchTree<K>> entry : S.child.entrySet()) {
            if (!T.child.containsKey(entry.getKey())) {
                return false;
            }

            if (!isSubTree(T.child.get(entry.getKey()), entry.getValue())) {
                return false;
            }
        }

        return true;
    }

    /**
     * add all containers stored in tree to given empty list 
     */
    public void getAllContainers(List<K> container) {
        if (isLeaf) {
            container.add(this.container);
        } else {
            for (Entry<String, BranchTree<K>> entry : child.entrySet()) {
                entry.getValue().getAllContainers(container);
            }
        }
    }

    /**
     * add all containers belong given branch to given container
     */
    public void getAllContainers(String branch, List<K> container) {
        BranchTree<K> lastNode = findLastNode(branch);
        if (lastNode == null) {
            throw new IllegalArgumentException("given branch is not belong to current tree");
        }
        lastNode.getAllContainers(container);
    }

    /**
     * get container stored at given branch <p>
     * return value can be null <p> 
     * if last node of given branch is not leaf node <p>
     * OR just null
     */
    public K getContainer(String branch) {
        BranchTree<K> lastNode = findLastNode(branch);
        if (lastNode == null) {
            throw new IllegalArgumentException("given branch is not belong to current tree");
        }
        return lastNode.container;
    }

    /**
     * Convert tree to String format <p>
     * NOTE : container can be null if not needed
     */
    public void toString(List<String> branches, List<K> container) {
        final String temp = tag;
        tag = "";
        toString(branches, "", container);
        tag = temp;
    }

    /**
     * Convert sub tree under given branch to String format <p>
     * NOTE : container can be null if not needed
     */
    public void toStringForSubTree(String branch, List<String> branches, List<K> container) {

        // Search node corresponding to last node of given branch
        BranchTree<K> curr = this;
        StringBuilder sb = new StringBuilder();
        String[] branchSeg = stripOffStrings(branch).split(getDelimiter());
        for (String seg : branchSeg) {
            if (!isValidTag(seg)) {
                throw new IllegalArgumentException("Invalid branch format : branchSeg = " + seg);
            }

            if (!curr.hasChild(seg)) {
                throw new IllegalArgumentException("given branch is not belong to current tree");
            }
            sb.append(seg);
            sb.append(getDelimiter());

            curr = curr.child.get(seg);
        }

        final String temp = curr.tag;
        curr.tag = "";
        curr.toString(branches, sb.toString(), container);
        curr.tag = temp;
    }

    public void printAllTree() {
        Queue<BranchTree<K>> q = new LinkedList<BranchTree<K>>();
        q.add(this);
        while (!q.isEmpty()) {
            BranchTree<K> bt = q.poll();

            System.out.println("Node : " + bt.tag + ", isLeaf : " + bt.isLeaf);
            if (bt.child != null) {
                for (Entry<String, BranchTree<K>> entry : bt.child.entrySet()) {
                    q.add(entry.getValue());
                }
            }
        }
    }

    private void toString(List<String> branches, String str, List<K> container) {
        String res = str + tag;
        if (isLeaf) {
            branches.add(getPreAppend() + res + getPostAppend());
            if (container != null && this.container != null) {
                container.add(this.container);
            }
            return;
        }

        if (!tag.isEmpty()) {
            res += getDelimiter();
        }
        for (Entry<String, BranchTree<K>> entry : child.entrySet()) {
            entry.getValue().toString(branches, res, container);
        }
    }

    private BranchTree<K> findLastNode(String branch) {
        String[] branchSeg = stripOffStrings(branch).split(getDelimiter());

        BranchTree<K> curr = this;
        for (String seg : branchSeg) {
            if (!isValidTag(seg)) {
                throw new IllegalArgumentException("Invalid branch format : branchSeg = " + seg);
            }

            if (!curr.hasChild(seg)) {
                return null;
            }

            curr = curr.child.get(seg);
        }

    /**
     * Return leaves of the starting branch
     * @param branch
     * @return array of String containing leaves of the starting branch
     */
    public String[] getLeaves(String branch) {
        Stack<BranchTree<K>> stack = new Stack<BranchTree<K>>();
        BranchTree<K> curr = this;
        stack.push(curr.findLastNode(branch));
        List<String> leaves = new LinkedList<String>();
        while(!stack.empty()) {
            curr = stack.pop();
            if(curr.isLeaf) {
                leaves.add(curr.tag);
            } else {
                Map<String, BranchTree<K>> subTreeMap = curr.getChild();
                for(Entry<String, BranchTree<K>> entry : subTreeMap.entrySet()) {
                    stack.push(entry.getValue());
                }
            }
        }
        return leaves.toArray(new String[leaves.size()]);
    }

    /**
     * True if successfully add given branch to tree without duplicate 
     */
    private boolean addBranch(String[] branchSeg, K container) {
        if (branchSeg == null || branchSeg.length == 0) {
            throw new IllegalArgumentException();
        }

        boolean success = false;
        BranchTree<K> curr = this;
        for (String seg : branchSeg) {
            if (!isValidTag(seg)) {
                throw new IllegalArgumentException("Invalid branch format : branchSeg = " + seg);
            }

            if (!curr.hasChild(seg)) {
                curr.putChild(seg);
                success = true;
            }

            curr = curr.getBranchTree(seg);
        }

        curr.container = container;
        curr.isLeaf = true;
        return success;
    }

    /**
     * tag must start with alphabet or digit <p>
     * ex) "..." => false <p>
     * ex) "2abc" => true
     */
    private static boolean isValidTag(String tag) {
        if (tag == null || tag.isEmpty()) {
            return false;
        }
        return Character.isAlphabetic(tag.charAt(0)) || Character.isDigit(tag.charAt(0));
    }

    /**
     * Return {@link BranchTree} associated with tag from child of current node
     */
    private BranchTree<K> getBranchTree(String tag) {
        if (child != null && tag != null && !tag.isEmpty()) {
            return child.get(tag);
        }

        return null;
    }

    /**
     * true if child of current node has tag
     */
    private boolean hasChild(String tag) {
        return child != null && child.containsKey(tag);
    }
}
