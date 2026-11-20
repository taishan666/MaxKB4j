package com.maxkb4j.knowledge.store.impl;

import com.maxkb4j.knowledge.service.KnowledgeModelService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Iterator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PgVectorEmbeddingStoreImplTest {

    @Mock
    private KnowledgeModelService knowledgeModelService;
    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private PreparedStatement statement;
    @Mock
    private ResultSet resultSet;
    @Mock
    private EmbeddingStore<TextSegment> store1024;
    @Mock
    private EmbeddingStore<TextSegment> store1536;

    private PgVectorEmbeddingStoreImpl vectorStore;

    @BeforeEach
    void setUp() {
        vectorStore = spy(new PgVectorEmbeddingStoreImpl(knowledgeModelService, dataSource));
    }

    @Test
    void deleteByKnowledgeId_afterRestart_deletesFromPersistedDimensionTables() throws Exception {
        mockEmbeddingTables("embedding_1024", "embedding_1536", "embedding_invalid", "other_table");
        doReturn(store1024).when(vectorStore).get(1024);
        doReturn(store1536).when(vectorStore).get(1536);

        vectorStore.deleteByKnowledgeId("knowledge-1");

        verify(store1024).removeAll(any(Filter.class));
        verify(store1536).removeAll(any(Filter.class));
    }

    private void mockEmbeddingTables(String... tableNames) throws Exception {
        Iterator<String> tables = Arrays.asList(tableNames).iterator();
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenAnswer(invocation -> tables.hasNext());
        when(resultSet.getString(1)).thenAnswer(invocation -> tables.next());
    }
}
