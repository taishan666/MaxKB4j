package com.maxkb4j.knowledge.linearrag;

import com.maxkb4j.knowledge.linearrag.entity.GraphEntityNode;
import com.maxkb4j.knowledge.linearrag.model.GraphNode;
import com.maxkb4j.knowledge.linearrag.model.TriGraph;

import java.text.BreakIterator;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the LinearRAG Tri-Graph (Entity-Sentence-Paragraph) per the paper specification.
 *
 * Graph construction steps:
 * 1. Add paragraph nodes and split content into sentence nodes
 * 2. Add entity nodes, build entity↔sentence bidirectional index
 * 3. Build weighted entity↔paragraph edges: count(entity,passage) / total_entities_in_passage
 * 4. Build paragraph↔paragraph adjacent edges: weight = 1.0
 *
 * The entity↔sentence index is used during BFS diffusion (not as explicit PPR graph edges).
 * Only entity↔paragraph and paragraph↔paragraph edges participate in PPR.
 */
public class TriGraphBuilder {

    private static final int MIN_SENTENCE_LENGTH = 5;
    private static final int MIN_ENTITY_LENGTH = 2;

    /**
     * Build a complete Tri-Graph from entities and paragraph data.
     *
     * @param entities   list of entity nodes from MongoDB
     * @param paragraphs ordered map of paragraph ID -> paragraph content (title + content)
     *                   The iteration order determines paragraph adjacency.
     * @return fully constructed TriGraph
     */
    public static TriGraph build(List<GraphEntityNode> entities, Map<String, String> paragraphs) {
        TriGraph graph = new TriGraph();

        if (entities == null || entities.isEmpty() || paragraphs == null || paragraphs.isEmpty()) {
            return graph;
        }

        // Step 1: Add paragraph and sentence nodes, track paragraph ordering for adjacency
        List<String> orderedParagraphIds = new ArrayList<>(paragraphs.keySet());
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
                sentenceIds.add(sentenceId);
            }

            paragraphSentenceMap.put(paragraphId, sentenceIds);
        }

        // Step 2: Add entity nodes, build entity↔sentence index, count occurrences per paragraph
        for (GraphEntityNode entity : entities) {
            String entityId = "e:" + entity.getName().toLowerCase();

            // Add entity node (may already exist from another paragraph)
            if (graph.getNode(entityId) == null) {
                graph.addNode(new GraphNode(
                        entityId,
                        entity.getOriginalName() != null ? entity.getOriginalName() : entity.getName(),
                        GraphNode.NodeType.ENTITY
                ));
            }

            if (entity.getParagraphIds() == null) continue;

            for (String paragraphId : entity.getParagraphIds()) {
                if (!paragraphs.containsKey(paragraphId)) continue;

                List<String> sentenceIds = paragraphSentenceMap.getOrDefault(paragraphId, Collections.emptyList());
                String lowerEntityName = entity.getName().toLowerCase();

                // Count entity occurrences across all sentences in this paragraph
                int totalOccurrences = 0;
                for (String sentenceId : sentenceIds) {
                    GraphNode sentenceNode = graph.getNode(sentenceId);
                    if (sentenceNode == null || sentenceNode.getContent() == null) continue;

                    String lowerSentence = sentenceNode.getContent().toLowerCase();
                    int count = countOccurrences(lowerSentence, lowerEntityName);

                    if (count > 0) {
                        // Link entity to sentence in bidirectional index (for BFS)
                        graph.linkEntityToSentence(entityId, sentenceId);
                        totalOccurrences += count;
                    }
                }

                // Record entity occurrence count in paragraph
                if (totalOccurrences > 0) {
                    graph.addEntityParagraphOccurrence(entityId, "p:" + paragraphId, totalOccurrences);
                }
            }
        }

        // Step 3: Build weighted entity↔paragraph edges
        // Weight = count(entity, passage) / total_entity_occurrences_in_passage
        buildEntityParagraphEdges(graph, orderedParagraphIds);

        // Step 4: Build paragraph↔paragraph adjacent edges (weight = 1.0)
        buildParagraphAdjacencyEdges(graph, orderedParagraphIds);

        return graph;
    }

    /**
     * Build weighted entity↔paragraph edges for the PPR graph.
     * Weight = count(entity, passage) / total_entity_occurrences_in_passage
     */
    private static void buildEntityParagraphEdges(TriGraph graph, List<String> orderedParagraphIds) {
        for (String paragraphId : orderedParagraphIds) {
            String pNodeId = "p:" + paragraphId;

            // Calculate total entity occurrences in this paragraph
            int totalEntityCount = 0;
            Map<String, Integer> entityCounts = new HashMap<>();

            for (String entityId : graph.getAllEntityIds()) {
                int count = graph.getEntityOccurrenceInParagraph(entityId, pNodeId);
                if (count > 0) {
                    entityCounts.put(entityId, count);
                    totalEntityCount += count;
                }
            }

            if (totalEntityCount == 0) continue;

            // Add weighted edges: entity → paragraph
            for (Map.Entry<String, Integer> entry : entityCounts.entrySet()) {
                double weight = (double) entry.getValue() / totalEntityCount;
                graph.addWeightedEdge(entry.getKey(), pNodeId, weight);
            }
        }
    }

    /**
     * Build paragraph↔paragraph adjacency edges for sequential paragraphs.
     * Adjacent paragraphs (by document order) are connected with weight = 1.0.
     */
    private static void buildParagraphAdjacencyEdges(TriGraph graph, List<String> orderedParagraphIds) {
        for (int i = 0; i < orderedParagraphIds.size() - 1; i++) {
            String p1 = "p:" + orderedParagraphIds.get(i);
            String p2 = "p:" + orderedParagraphIds.get(i + 1);

            // Only link if both nodes exist
            if (graph.getNode(p1) != null && graph.getNode(p2) != null) {
                graph.addWeightedEdge(p1, p2, 1.0);
            }
        }
    }

    /**
     * Count non-overlapping occurrences of a substring in text.
     */
    static int countOccurrences(String text, String sub) {
        if (text == null || sub == null || sub.isEmpty()) return 0;
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
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
