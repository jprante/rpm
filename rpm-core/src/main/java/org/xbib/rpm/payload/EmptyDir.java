package org.xbib.rpm.payload;

/**
 *
 */
public class EmptyDir {

    private static final int FILE_FLAG = 0100000;

    private static final int DIR_FLAG = 040000;

    private String path;

    private String username;

    private String group;

    private int filemode = -1;

    private int dirmode = -1;

    public String getPath() {
        return this.path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getGroup() {
        return this.group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public int getFilemode() {
        return this.filemode;
    }

    public void setFilemode(String filemode) {
        this.filemode = FILE_FLAG | Integer.parseInt(filemode, 8);
    }

    public int getDirmode() {
        return this.dirmode;
    }

    public void setDirmode(String dirmode) {
        this.dirmode = DIR_FLAG | Integer.parseInt(dirmode, 8);
    }
}
