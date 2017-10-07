package org.xbib.gradle.plugin.rpm

class DependencyGraph {

    Collection<DependencyGraphNode> nodes = []

    DependencyGraph(List<String> graph) {
        graph.each { nodes << parseNode(it) }
    }

    DependencyGraph(String... graph) {
        this(graph as List)
    }

    DependencyGraph(Map tuple) {
        nodes = tuple.nodes
    }
    
    private DependencyGraphNode parseNode(String s) {
        // Don't use tokenize, it'll make each character a possible delimeter, e.g. \t\n would tokenize on both
        // \t OR \n, not the combination of \t\n.
        def parts = s.split('->')
        def (group, artifact, version) = parts[0].trim().tokenize(':')
        def coordinate = new Coordinate(group: group, artifact: artifact, version: version)
        def dependencies = (parts.size() > 1) ? parseDependencies(parts[1]) : []
        new DependencyGraphNode(coordinate: coordinate, dependencies: dependencies)
    }

    private List<Coordinate> parseDependencies(String s) {
        List<Coordinate> dependencies = []
        s.tokenize('|').each { String dependency ->
            def (group, artifact, version) = dependency.trim().tokenize(':')
            dependencies << new Coordinate(group: group, artifact: artifact, version: version)
        }
        dependencies
    }
}
