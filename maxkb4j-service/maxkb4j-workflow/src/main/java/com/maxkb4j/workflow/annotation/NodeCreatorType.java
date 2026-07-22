package com.maxkb4j.workflow.annotation;

import com.maxkb4j.workflow.enums.NodeType;

import java.lang.annotation.*;

/**
 * Marks a concrete {@link com.maxkb4j.workflow.node.AbsNode} subclass as an auto-discoverable
 * node creator. Scanned by {@link com.maxkb4j.workflow.processor.NodeCreatorAutoRegistrar},
 * which builds a reflective creator and registers it into
 * {@link com.maxkb4j.workflow.registry.NodeCenter} keyed by {@link NodeType#getKey()}.
 *
 * <p>This replaces the hand-written register(NodeType.X, XNode::new) table that previously
 * lived in NodeCenter, mirroring the annotation-driven registration already used by node
 * handlers ({@link NodeHandlerType}).
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NodeCreatorType {
    /** The single node type this implementation creates. */
    NodeType value();
}