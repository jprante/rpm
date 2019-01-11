package org.xbib.gradle.plugin.rpm

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopyProcessingSpec
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTree
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopySpecInternal
import org.gradle.api.internal.file.copy.DefaultCopySpec
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.specs.Spec
import org.gradle.internal.reflect.Instantiator

import java.util.regex.Pattern

/**
 * An extension which can be attached to the project.
 * This is a superset of SystemPackagingExtension because we don't
 * want the @Delegate to inherit the copy spec parts.
 *
 * We can't extends DefaultCopySpec, since it's @NotExtensible, meaning that we won't get any convention
 * mappings. If we extend DelegatingCopySpec we get groovy compilation errors around the return types between
 * CopySourceSpec's methods and the ones overriden in DelegatingCopySpec, even though that's perfectly valid
 * Java code. The theory is that it's some bug in groovyc.
 */
class ProjectPackagingExtension extends SystemPackagingExtension {

    CopySpecInternal delegateCopySpec

    ProjectPackagingExtension(Project project) {
        FileResolver resolver = ((ProjectInternal) project).getFileResolver()
        Instantiator instantiator = ((ProjectInternal) project).getServices().get(Instantiator)
        delegateCopySpec = new DefaultCopySpec(resolver, instantiator)
    }

    /*
     * Special Use cases that involve Closure's which we want to wrap.
     */
    CopySpec from(Object sourcePath, Closure c) {
        def preserveSymlinks = FromConfigurationFactory.preserveSymlinks(this)
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().from(sourcePath, c << preserveSymlinks)
        }
    }

    CopySpec from(Object... sourcePaths) {
        def spec = null
        for (Object sourcePath : sourcePaths) {
            spec = from(sourcePath, {})
        }
        spec
    }

    CopySpec into(Object destPath, Closure configureClosure) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().into(destPath, configureClosure)
        }
    }

    CopySpec include(Closure includeSpec) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().include(includeSpec)
        }
    }

    CopySpec exclude(Closure excludeSpec) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().exclude(excludeSpec)
        }
    }

    CopySpec filter(Closure closure) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().filter(closure)
        }
    }

    CopySpec rename(Closure closure) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().rename(closure)
        }
    }

    CopySpec eachFile(Closure closure) {
        use(CopySpecEnhancement) {
            return getDelegateCopySpec().eachFile(closure)
        }
    }

    /*
     * Copy and Paste from org.gradle.api.internal.file.copy.DelegatingCopySpec, since extending it causes
     * compilation problems. The methods above are special cases and are commented out below.
     */
    RelativePath getDestPath() {
        getDelegateCopySpec().getDestPath()
    }

    FileTree getSource() {
        getDelegateCopySpec().getSource()
    }

    boolean hasSource() {
        getDelegateCopySpec().hasSource()
    }

    Collection<? extends Action<? super FileCopyDetails>> getAllCopyActions() {
        getDelegateCopySpec().getAllCopyActions()
    }

    boolean isCaseSensitive() {
        getDelegateCopySpec().isCaseSensitive()
    }

    void setCaseSensitive(boolean caseSensitive) {
        getDelegateCopySpec().setCaseSensitive(caseSensitive)
    }

    boolean getIncludeEmptyDirs() {
        getDelegateCopySpec().getIncludeEmptyDirs()
    }

    void setIncludeEmptyDirs(boolean includeEmptyDirs) {
        getDelegateCopySpec().setIncludeEmptyDirs(includeEmptyDirs)
    }

    DuplicatesStrategy getDuplicatesStrategy() {
        getDelegateCopySpec().getDuplicatesStrategy()
    }

    void setDuplicatesStrategy(DuplicatesStrategy strategy) {
        getDelegateCopySpec().setDuplicatesStrategy(strategy)
    }

    CopySpec filesMatching(String pattern, Action<? super FileCopyDetails> action) {
        getDelegateCopySpec().filesMatching(pattern, action)
    }

    CopySpec filesNotMatching(String pattern, Action<? super FileCopyDetails> action) {
        getDelegateCopySpec().filesNotMatching(pattern, action)
    }

    CopySpec with(CopySpec... sourceSpecs) {
        getDelegateCopySpec().with(sourceSpecs)
    }

    CopySpec setIncludes(Iterable<String> includes) {
        getDelegateCopySpec().setIncludes(includes)
    }

    CopySpec setExcludes(Iterable<String> excludes) {
        getDelegateCopySpec().setExcludes(excludes)
    }

    CopySpec include(String... includes) {
        getDelegateCopySpec().include(includes)
    }

    CopySpec include(Iterable<String> includes) {
        getDelegateCopySpec().include(includes)
    }

    CopySpec include(Spec<FileTreeElement> includeSpec) {
        getDelegateCopySpec().include(includeSpec)
    }

    CopySpec exclude(String... excludes) {
        getDelegateCopySpec().exclude(excludes)
    }

    CopySpec exclude(Iterable<String> excludes) {
        getDelegateCopySpec().exclude(excludes)
    }

    CopySpec exclude(Spec<FileTreeElement> excludeSpec) {
        getDelegateCopySpec().exclude(excludeSpec)
    }

    CopySpec into(Object destPath) {
        getDelegateCopySpec().into(destPath)
    }

    CopySpec rename(String sourceRegEx, String replaceWith) {
        getDelegateCopySpec().rename(sourceRegEx, replaceWith)
    }

    CopyProcessingSpec rename(Pattern sourceRegEx, String replaceWith) {
        getDelegateCopySpec().rename(sourceRegEx, replaceWith)
    }

    CopySpec filter(Map<String, ?> properties, Class<? extends FilterReader> filterType) {
        getDelegateCopySpec().filter(properties, filterType)
    }

    CopySpec filter(Class<? extends FilterReader> filterType) {
        getDelegateCopySpec().filter(filterType)
    }

    CopySpec expand(Map<String, ?> properties) {
        getDelegateCopySpec().expand(properties)
    }

    CopySpec eachFile(Action<? super FileCopyDetails> action) {
        getDelegateCopySpec().eachFile(action)
    }

    Integer getFileMode() {
        getDelegateCopySpec().getFileMode()
    }

    CopyProcessingSpec setFileMode(Integer mode) {
        getDelegateCopySpec().setFileMode(mode)
    }

    Integer getDirMode() {
        getDelegateCopySpec().getDirMode()
    }

    CopyProcessingSpec setDirMode(Integer mode) {
        getDelegateCopySpec().setDirMode(mode)
    }

    Set<String> getIncludes() {
        getDelegateCopySpec().getIncludes()
    }

    Set<String> getExcludes() {
        getDelegateCopySpec().getExcludes()
    }

    Iterable<CopySpecInternal> getChildren() {
        getDelegateCopySpec().getChildren()
    }

    FileTree getAllSource() {
        getDelegateCopySpec().getAllSource()
    }

    DefaultCopySpec addChild() {
        getDelegateCopySpec().addChild()
    }

    DefaultCopySpec addFirst() {
        getDelegateCopySpec().addFirst()
    }

    void walk(Action<? super CopySpecInternal> action) {
        action.execute(this)
        for (CopySpecInternal child : getChildren()) {
            child.walk(action)
        }
    }
}
