package com.maxkb4j.knowledge.linearrag;

import com.maxkb4j.knowledge.linearrag.entity.GraphEntityNode;
import com.maxkb4j.knowledge.linearrag.model.GraphEdge;
import com.maxkb4j.knowledge.linearrag.model.GraphNode;
import com.maxkb4j.knowledge.linearrag.model.TriGraph;

import java.text.BreakIterator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the LinearRAG Tri-Graph (Entity-Sentence-Paragraph) from raw data.
 * Tri-Graph construction:
 * 1. Entity nodes  - extracted entities (from MongoDB GraphEntityNode collection)
 * 2. Sentence nodes - paragraph content split into sentences
 * 3. Paragraph nodes - original paragraphs from knowledge base
 * Edges:
 * - ENTITY_IN_SENTENCE:    entity name appears in sentence text
 * - ENTITY_IN_PARAGRAPH:   entity appears in paragraph (via sentences)
 * - SENTENCE_IN_PARAGRAPH: sentence belongs to paragraph
 * - ENTITY_CO_OCCURRENCE:  two entities co-occur in same sentence
 */
public class TriGraphBuilder {

    private static final int MIN_SENTENCE_LENGTH = 5;
    private static final int MIN_ENTITY_LENGTH = 2;

    /**
     * Build a complete Tri-Graph from entities and paragraph data.
     *
     * @param entities   list of entity nodes from MongoDB
     * @param paragraphs map of paragraph ID -> paragraph content (title + content)
     * @return fully constructed TriGraph
     */
    public static TriGraph build(List<GraphEntityNode> entities, Map<String, String> paragraphs) {
        TriGraph graph = new TriGraph();

        if (entities == null || entities.isEmpty() || paragraphs == null || paragraphs.isEmpty()) {
            return graph;
        }

        // Step 1: Add paragraph and sentence nodes
        Map<String, List<String>> paragraphSentenceMap = new HashMap<>();
        for (Map.Entry<String, String> entry : paragraphs.entrySet()) {
            String paragraphId = entry.getKey();
            String content = entry.getValue();

            // Add paragraph node
            graph.addNode(new GraphNode(
                    "p:" + paragraphId,
                    paragraphId,
                    GraphNode.NodeType.PARAGRAPH,
                    content
            ));

            // Split content into sentences and add sentence nodes
            List<String> sentences = splitIntoSentences(content);
            List<String> sentenceIds = new ArrayList<>();

            for (int i = 0; i < sentences.size(); i++) {
                String sentenceId = "s:" + paragraphId + ":" + i;
                graph.addNode(new GraphNode(
                        sentenceId,
                        sentences.get(i),
                        GraphNode.NodeType.SENTENCE,
                        sentences.get(i)
                ));

                // SENTENCE_IN_PARAGRAPH edge
                graph.addEdge(new GraphEdge(
                        sentenceId,
                        "p:" + paragraphId,
                        GraphEdge.EdgeType.SENTENCE_IN_PARAGRAPH
                ));

                sentenceIds.add(sentenceId);
            }

            paragraphSentenceMap.put(paragraphId, sentenceIds);
        }

        // Step 2: Add entity nodes and build entity-sentence edges
        for (GraphEntityNode entity : entities) {
            String entityId = "e:" + entity.getName();

            // Add entity node (may already exist from another paragraph)
            if (graph.getNode(entityId) == null) {
                graph.addNode(new GraphNode(
                        entityId,
                        entity.getOriginalName() != null ? entity.getOriginalName() : entity.getName(),
                        GraphNode.NodeType.ENTITY
                ));
            }

            // Link entity to paragraphs it appears in
            if (entity.getParagraphIds() != null) {
                for (String paragraphId : entity.getParagraphIds()) {
                    if (!paragraphs.containsKey(paragraphId)) {
                        continue;
                    }

                    // ENTITY_IN_PARAGRAPH edge
                    graph.addEdge(new GraphEdge(
                            entityId,
                            "p:" + paragraphId,
                            GraphEdge.EdgeType.ENTITY_IN_PARAGRAPH
                    ));

                    // ENTITY_IN_SENTENCE edges (check which sentences contain this entity)
                    List<String> sentenceIds = paragraphSentenceMap.getOrDefault(paragraphId, Collections.emptyList());
                    String lowerEntityName = entity.getName().toLowerCase();

                    for (String sentenceId : sentenceIds) {
                        GraphNode sentenceNode = graph.getNode(sentenceId);
                        if (sentenceNode != null && sentenceNode.getContent() != null) {
                            String lowerSentence = sentenceNode.getContent().toLowerCase();
                            if (lowerSentence.contains(lowerEntityName)) {
                                graph.addEdge(new GraphEdge(
                                        entityId,
                                        sentenceId,
                                        GraphEdge.EdgeType.ENTITY_IN_SENTENCE
                                ));
                            }
                        }
                    }
                }
            }
        }

        // Step 3: Build entity co-occurrence edges
        buildCoOccurrenceEdges(graph, paragraphSentenceMap);

        return graph;
    }

    /**
     * Build ENTITY_CO_OCCURRENCE edges between entities that appear in the same sentence.
     */
    private static void buildCoOccurrenceEdges(TriGraph graph, Map<String, List<String>> paragraphSentenceMap) {
        // For each sentence, find all entities that appear in it
        Map<String, Set<String>> sentenceToEntities = new HashMap<>();

        for (String entityId : graph.getAllEntityIds()) {
            Set<String> entitySentences = graph.getEntitySentences(entityId);
            for (String sentenceId : entitySentences) {
                sentenceToEntities.computeIfAbsent(sentenceId, k -> new HashSet<>()).add(entityId);
            }
        }

        // Create co-occurrence edges
        Set<String> processedPairs = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : sentenceToEntities.entrySet()) {
            Set<String> entities = entry.getValue();
            if (entities.size() < 2) {
                continue;
            }

            List<String> entityList = new ArrayList<>(entities);
            for (int i = 0; i < entityList.size(); i++) {
                for (int j = i + 1; j < entityList.size(); j++) {
                    String e1 = entityList.get(i);
                    String e2 = entityList.get(j);
                    String pairKey = e1.compareTo(e2) < 0 ? e1 + "|" + e2 : e2 + "|" + e1;

                    if (processedPairs.add(pairKey)) {
                        graph.addEdge(new GraphEdge(
                                e1, e2,
                                GraphEdge.EdgeType.ENTITY_CO_OCCURRENCE,
                                1.0
                        ));
                    }
                }
            }
        }
    }

    /**
     * Split text into sentences using BreakIterator (supports Chinese and English).
     */
    public static List<String> splitIntoSentences(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> sentences = new ArrayList<>();
        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.CHINESE);
        iterator.setText(text);

        int start = iterator.first();
        int end = iterator.next();
        while (end != BreakIterator.DONE) {
            String sentence = text.substring(start, end).trim();
            if (sentence.length() >= MIN_SENTENCE_LENGTH) {
                sentences.add(sentence);
            }
            start = end;
            end = iterator.next();
        }

        // If no sentences were found (very short text), use the text itself
        if (sentences.isEmpty() && text.length() >= MIN_SENTENCE_LENGTH) {
            sentences.add(text.trim());
        }

        return sentences;
    }

    /**
     * Extract entity occurrences from a sentence given a list of entity names.
     * Returns entity names that appear in the sentence text.
     */
    public static List<String> findEntitiesInText(String text, Collection<String> entityNames) {
        if (text == null || entityNames == null) {
            return Collections.emptyList();
        }
        String lowerText = text.toLowerCase();
        return entityNames.stream()
                .filter(name -> name.length() >= MIN_ENTITY_LENGTH)
                .filter(name -> lowerText.contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }
}
