package com.maxkb4j.knowledge.linearrag.model;

import lombok.Getter;

/**
 * Represents a node in the LinearRAG Tri-Graph.
 * Tri-Graph has three types of nodes: ENTITY, SENTENCE, PARAGRAPH
 */
@Getter
public class GraphNode {

    public enum NodeType {
        ENTITY,
        SENTENCE,
        PARAGRAPH
    }

    private final String id;
    private final String name;
    private final NodeType type;
    private final String content;

    public GraphNode(String id, String name, NodeType type, String content) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.content = content;
    }

    public GraphNode(String id, String name, NodeType type) {
        this(id, name, type, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNode node = (GraphNode) o;
        return id.equals(node.id) && type == node.type;
    }

    @Override
    public int hashCode() {
        int result = id.hashCode();
        result = 31 * result + type.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "GraphNode{id='" + id + "', name='" + name + "', type=" + type + "}";
    }
}
