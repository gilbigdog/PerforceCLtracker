package com.ssl.cltracker.data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileNode {
    private String mFileName;
    private int mCL;
    private Set<Integer> mRevision;
    private List<FileNode> mChildren;
    private FileNode mParent;

    public FileNode(String fileBranch) {
        this.mFileName = fileBranch;
    }

    public FileNode(String fileBranch, int changeList, int revision) {
        this.mFileName = fileBranch;
        this.mCL = changeList;
        if (null == this.mRevision) {
            this.mRevision = new HashSet<Integer>();
        }
        if(!this.mRevision.contains(revision)) {
            this.mRevision.add(revision);
        }
    }
    public FileNode(String fileBranch, int changeList, Set<Integer> revisions) {
        this.mFileName = fileBranch;
        this.mCL = changeList;
        if(null != this.mRevision) this.mRevision.clear();
        this.mRevision = new HashSet<Integer>(revisions);
    }

    public FileNode(String fileBranch, Set<Integer> revisions) {
        this.mFileName = fileBranch;
        if(null != this.mRevision) this.mRevision.clear();
        this.mRevision = new HashSet<Integer>(revisions);
    }

    public FileNode() {
        //do nothing
    }

    public String getFileName() {
        return mFileName;
    }

    public void setFileName(String fileName) {
        mFileName = fileName;
    }

    public Set<Integer> getRevision() {
        return mRevision;
    }

    public void addRevision(int revision) {
        if (null == mRevision) {
            mRevision = new HashSet<Integer>();
        }
        if(!mRevision.contains(revision)) {
            mRevision.add(revision);
        }
    }
    public void setRevision(Set<Integer> revisions) {
        if(null != mRevision) mRevision.clear();
        mRevision = new HashSet<Integer>(revisions);
    }

    public int getCL() {
        return mCL;
    }

    public void setCL(int CL) {
        this.mCL = CL;
    }

    public List<FileNode> getChildren() {
        return mChildren;
    }

    public void setChildren(Collection<FileNode> children) {
        if(null == mChildren) {
            mChildren = new ArrayList<FileNode>();
        }
        for(FileNode child : children) {
            mChildren.add(child);
        }
    }

    public void addChild(FileNode child) {
        if (null == mChildren) {
            mChildren = new ArrayList<FileNode>();
        }
        mChildren.add(child);
    }

    public FileNode getParent() {
        return mParent;
    }

    public void setParent(FileNode parent) {
        this.mParent = parent;
    }
}