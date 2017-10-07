package org.xbib.rpm.ant;

import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.TarFileSet;
import org.xbib.rpm.payload.Directive;

import java.util.EnumSet;

/**
 * A {@code RpmFileSet} is a {@link FileSet} to support RPM directives that can't be expressed
 * using ant's built-in {@code FileSet} classes.
 */
public class RpmFileSet extends TarFileSet {

    private EnumSet<Directive> directives = EnumSet.of(Directive.NONE);

    /**
     * Constructor for {@code RpmFileSet}.
     */
    public RpmFileSet() {
        super();
    }

    /**
     * Constructor using a fileset arguement.
     *
     * @param fileset the {@link FileSet} to use
     */
    protected RpmFileSet(FileSet fileset) {
        super(fileset);
    }

    /**
     * Constructor using a archive fileset argument.
     *
     * @param fileset the {@code RpmFileSet} to use
     */
    protected RpmFileSet(RpmFileSet fileset) {
        super(fileset);
        directives = fileset.getDirectives();
    }

    public EnumSet<Directive> getDirectives() {
        return directives;
    }

    /**
     * Supports {@code %ghost} directive, used to flag the specified file as being a ghost file.
     * By adding this directive to the line containing a file, RPM will know about the ghosted file, but will
     * not add it to the package.
     * Permitted values for this directive are:
     * <ul>
     * <li> {@code true}    (equivalent to specifying {@code %ghost}
     * <li> {@code false}     (equivalent to omitting {@code %ghost})
     * </ul>
     *
     * @param ghost the ghost
     */
    public void setGhost(boolean ghost) {
        checkRpmFileSetAttributesAllowed();
        if (ghost) {
            directives.add(Directive.GHOST);
        } else {
            directives.remove(Directive.GHOST);
        }
    }

    /**
     * Supports RPM's {@code %config} directive, used to flag the specified file as being a configuration file.
     * RPM performs additional processing for config files when packages are erased, and during installations
     * and upgrades.
     * Permitted values for this directive are:
     * <ul>
     * <li> {@code true}    (equivalent to specifying {@code %config}
     * <li> {@code false}     (equivalent to omitting {@code %config})
     * </ul>
     *
     * @param config the config
     */
    public void setConfig(boolean config) {
        checkRpmFileSetAttributesAllowed();
        if (config) {
            directives.add(Directive.CONFIG);
        } else {
            directives.remove(Directive.CONFIG);
        }
    }

    /**
     * Supports RPM's {@code %config(noreplace)} directive. This directive modifies how RPM manages edited config
     * files.
     * Permitted values for this directive are:
     * <ul>
     * <li> {@code true}    (equivalent to specifying {@code %noreplace}
     * <li> {@code false}     (equivalent to omitting {@code %noreplace})
     * </ul>
     *
     * @param noReplace the noreplace
     */
    public void setNoReplace(boolean noReplace) {
        checkRpmFileSetAttributesAllowed();
        if (noReplace) {
            directives.add(Directive.NOREPLACE);
        } else {
            directives.remove(Directive.NOREPLACE);
        }
    }

    /**
     * Supports RPM's {@code %doc} directive, which flags the files as being documentation.  RPM keeps track of
     * documentation files in its database, so that a user can easily find information about an installed package.
     * Permitted values for this directive are:
     * <ul>
     * <li> {@code true}    (equivalent to specifying {@code %doc}
     * <li> {@code false}     (equivalent to omitting {@code %doc})
     * </ul>
     *
     * @param doc the doc
     */
    public void setDoc(boolean doc) {
        checkRpmFileSetAttributesAllowed();
        if (doc) {
            directives.add(Directive.DOC);
        } else {
            directives.remove(Directive.DOC);
        }
    }

    /**
     * Return a ArchiveFileSet that has the same properties as this one.
     *
     * @return the cloned archiveFileSet
     */
    public Object clone() {
        if (isReference()) {
            return getRef(getProject()).clone();
        }
        return super.clone();
    }

    private void checkRpmFileSetAttributesAllowed() {
        if (getProject() == null ||
                (isReference() && (getRefid().getReferencedObject(getProject()) instanceof RpmFileSet))) {
            checkAttributesAllowed();
        }
    }
}
