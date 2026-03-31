package com.maxkb4j.workflow.model;

import com.maxkb4j.workflow.logic.LfEdge;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * Navigator for workflow edges.
 * Provides methods to find upstream and downstream relationships between nodes.
 *
 * Extracted from Workflow class to improve single responsibility.
 */
@Getter
public class EdgeNavigator {

    private final List<LfEdge> edges;

    public EdgeNavigator(List<LfEdge> edges) {
        this.edges = edges != null ? edges : Collections.emptyList();
    }

    /**
     * Find downstream edges for a given node.
     *
     * @param nodeId the source node ID
     * @return list of edges starting from the given node
     */
    public List<LfEdge> findDownstreamEdges(String nodeId) {
        if (nodeId == null) {
            return Collections.emptyList();
        }
        return edges.stream()
                .filter(edge -> nodeId.equals(edge.getSourceNodeId()))
                .toList();
    }

    /**
     * Find upstream node IDs for a given node.
     *
     * @param nodeId the target node ID
     * @return list of upstream node IDs
     */
    public List<String> findUpstreamNodeIds(String nodeId) {
        if (nodeId == null) {
            return Collections.emptyList();
        }
        return edges.stream()
                .filter(edge -> nodeId.equals(edge.getTargetNodeId()))
                .map(LfEdge::getSourceNodeId)
                .toList();
    }

    /**
     * Check if the navigator has any edges.
     *
     * @return true if there are no edges
     */
    public boolean isEmpty() {
        return edges.isEmpty();
    }

    /**
     * Get the number of edges.
     *
     * @return edge count
     */
    public int size() {
        return edges.size();
    }
}