package aprove.verification.confluence.Utility;

import java.util.*;
import java.util.function.*;
import java.util.stream.*;

import aprove.verification.oldframework.Utility.Graph.*;
import aprove.verification.oldframework.Utility.Graph.Node;

public final class GraphAnalysis {

    public static <N, E> Set<Node<N>> findSharedComponents(SimpleGraph<N, E> graph) {
        Set<Node<N>> joinPoints = graph.getNodes()
                    .stream()
                    .filter(x -> (graph.getIn(x).size() >= 2))
                    .collect(Collectors.toSet());
        return graph.determineReachableNodes(joinPoints);
    }

    /**
     * Decomposes a graph into distinct "flows" initiated by its source branches.
     * It works by:
     * 1. Identifying a "core" of join points and their descendants.
     * 2. Temporarily removing the core to find the remaining "source" branches.
     * 3. Grouping connected source branches using Weakly Connected Components (WCCs).
     * 4. For each source group, finding its complete set of descendants in the original graph.
     *
     * @param graph The graph to analyze.
     * @return A set where each inner set represents a full flow from a source group.
     */
    public static <N, E> Set<Set<N>> findSourceInitiatedFlows(SimpleGraph<N, E> graph) {
        Set<Node<N>> sharedNodes = findSharedComponents(graph);

        Set<Node<N>> sourceNodes = new HashSet<>(graph.getNodes());
        sourceNodes.removeAll(sharedNodes);

        if (sourceNodes.isEmpty()) {
            return Collections.emptySet();
        }

        return graph.getSubGraph(sourceNodes)
                .getWCCs()
                .stream()
                .map(graph::determineReachableNodes)
                .map(nodeSet -> nodeSet.stream()
                        .map(Node::getObject)
                        .collect(Collectors.toSet()))
                .collect(Collectors.toSet());
    }
    
    
    /**
     * Identifies shared components, excluding nodes that match a "poison" predicate
     * or are ancestors of such nodes.
     *
     * @param graph        The graph to analyze.
     * @param isPoisoned   A predicate that returns true if a node is "poisoned".
     * @return A set of all nodes that are part of a valid shared component.
     */
    public static <N, E> Set<Node<N>> findSharedComponents(SimpleGraph<N, E> graph, Predicate<Node<N>> isPoisoned) {
        Set<Node<N>> poisonedNodes = graph.getNodes()
                .stream()
                .filter(isPoisoned)
                .collect(Collectors.toSet());

        // Optimization: If no nodes are poisoned, no nodes can be disqualified.
        if (poisonedNodes.isEmpty()) {
            return findSharedComponents(graph);
        }

        // A node is disqualified if it's poisoned or is an ancestor of a poisoned node.
        Set<Node<N>> disqualifiedNodes = new HashSet<>(poisonedNodes);
        disqualifiedNodes.addAll(determineAncestors(graph, poisonedNodes));

        // Step 3: Identify "valid" join points (in-degree >= 2 and not disqualified).
        Set<Node<N>> validJoinPoints = graph.getNodes()
                .stream()
                .filter(node -> graph.getIn(node).size() >= 2)
                .filter(node -> !disqualifiedNodes.contains(node)) // The crucial filter condition
                .collect(Collectors.toSet());

        if (validJoinPoints.isEmpty()) {
            return Collections.emptySet();
        }

        // Step 4: The shared components are all nodes reachable from these valid join points.
        return graph.determineReachableNodes(validJoinPoints);
    }

    /**
     * Decomposes a graph into distinct "flows" using a predicate to identify "poisoned" nodes.
     *
     * @param graph             The graph to analyze.
     * @param isObjectPoisoned  A predicate to identify if a node's content object is "poisoned".
     * @return A set where each inner set represents a full flow from a source group.
     */
    public static <N, E> Set<Set<N>> findSourceInitiatedFlows(SimpleGraph<N, E> graph, Predicate<N> isObjectPoisoned) {
        // Adapt the user-provided predicate on <N> to the internal predicate on <Node<N>>.
        Predicate<Node<N>> isNodePoisoned = node -> isObjectPoisoned.test(node.getObject());

        // 1. Identifying the "core" using the new predicate-based logic.
        Set<Node<N>> sharedNodes = findSharedComponents(graph, isNodePoisoned);

        // The rest of the logic remains exactly the same.
        Set<Node<N>> sourceNodes = new HashSet<>(graph.getNodes());
        sourceNodes.removeAll(sharedNodes);

        if (sourceNodes.isEmpty()) {
            return Collections.emptySet();
        }

        return graph.getSubGraph(sourceNodes)
                .getWCCs()
                .stream()
                .map(graph::determineReachableNodes)
                .map(nodeSet -> nodeSet.stream()
                        .map(Node::getObject)
                        .collect(Collectors.toSet()))
                .collect(Collectors.toSet());
    }
    
    
    private static <N, E> Set<Node<N>> determineAncestors(SimpleGraph<N, E> graph, Set<Node<N>> startNodes) {
        Set<Node<N>> ancestors = new HashSet<>();
        Queue<Node<N>> worklist = new LinkedList<>(startNodes);
        Set<Node<N>> visited = new HashSet<>(startNodes);

        while (!worklist.isEmpty()) {
            Node<N> currentNode = worklist.poll();
            for (Node<N> predecessor : graph.getIn(currentNode)) {
                if (visited.add(predecessor)) { // If predecessor hasn't been visited yet
                    ancestors.add(predecessor);
                    worklist.add(predecessor);
                }
            }
        }
        return ancestors;
    }
    
}
