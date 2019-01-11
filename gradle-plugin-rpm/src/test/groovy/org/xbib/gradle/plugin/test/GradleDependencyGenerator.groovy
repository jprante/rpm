package org.xbib.gradle.plugin.test

class GradleDependencyGenerator {

    static final String STANDARD_SUBPROJECT_BLOCK = '''\
        subprojects {
            apply plugin: 'maven-publish'
            apply plugin: 'java'

            publishing {
                repositories {
                    maven {
                        url "../mavenrepo"
                    }
                }
                publications {
                    maven(MavenPublication) {
                        from components.java
                    }
                }
            }
        }
    '''.stripIndent()
    static final String BUILD_GRADLE = 'build.gradle'

    private boolean generated = false

    DependencyGraph graph
    File gradleRoot
    File mavenRepoDir

    GradleDependencyGenerator(DependencyGraph graph, String directory = 'build/testrepogen') {
        this.graph = graph
        this.gradleRoot = new File(directory)
        this.mavenRepoDir = new File(directory, 'mavenrepo')
        generateGradleFiles()
    }

    File generateTestMavenRepo() {
        runTasks('publishMavenPublicationToMavenRepository')

        mavenRepoDir
    }

    String getMavenRepoDirPath() {
        mavenRepoDir.absolutePath
    }

    String getMavenRepoUrl() {
        mavenRepoDir.toURI().toURL()
    }

    String getMavenRepositoryBlock() {
        """\
            maven { url '${getMavenRepoUrl()}' }
        """.stripIndent()
    }

    private void generateGradleFiles() {
        if (generated) {
            return
        } else {
            generated = true
        }

        gradleRoot.mkdirs()
        def rootBuildGradle = new File(gradleRoot, BUILD_GRADLE)
        rootBuildGradle.text = STANDARD_SUBPROJECT_BLOCK
        def includes = []
        graph.nodes.each { DependencyGraphNode n ->
            String subName = "${n.group}.${n.artifact}_${n.version.replaceAll(/\./, '_')}"
            includes << subName
            def subfolder = new File(gradleRoot, subName)
            subfolder.mkdir()
            def subBuildGradle = new File(subfolder, BUILD_GRADLE)
            subBuildGradle.text = generateSubBuildGradle(n)
        }
        def settingsGradle = new File(gradleRoot, 'settings.gradle')
        settingsGradle.text = 'include ' + includes.collect { "'${it}'"}.join(', ')
    }

    private String generateSubBuildGradle(DependencyGraphNode node) {

        StringWriter block = new StringWriter()
        if (node.dependencies) {
            block.withPrintWriter { writer ->
                writer.println 'dependencies {'
                node.dependencies.each { writer.println "    compile '${it}'" }
                writer.println '}'
            }
        }

        """\
            group = '${node.group}'
            version = '${node.version}'
            ext { artifactName = '${node.artifact}' }
        """.stripIndent() + block.toString()
    }

    private void runTasks(String tasks) {
        def runner = GradleRunnerFactory.createTooling()
        runner.run(gradleRoot, tasks.tokenize()).rethrowFailure()
    }
}