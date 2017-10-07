package org.xbib.rpm.exception;

/**
 * Scan path for discovering files is not within a child path of the base build path.
 */
public class PathOutsideBuildPathException extends RpmException {

    private static final long serialVersionUID = 7028847909078304806L;

    /**
     *
     * @param scanPath  Scan path
     * @param buildPath Build path
     */
    public PathOutsideBuildPathException(String scanPath, String buildPath) {
        super(String.format("Scan path %s outside of build directory %s", scanPath, buildPath));
    }
}
