package com.maxkb4j.knowledge.store.impl;

import com.maxkb4j.knowledge.service.KnowledgeModelService;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

    @Test
    void buildFetchUriResolvesRelativePathToLocalService() {
        URI uri = PgVectorEmbeddingStoreImpl.buildFetchUri("/api/image/1.png", 8080);

        assertNotNull(uri);
        assertEquals("http", uri.getScheme());
        assertEquals("127.0.0.1", uri.getHost());
        assertEquals(8080, uri.getPort());
        assertEquals("/api/image/1.png", uri.getPath());
    }

    @Test
    void buildFetchUriKeepsQueryAndBlocksHostInjectionInRelativePath() {
        // 旧实现的字符串拼接会把 "/@evil.com/x" 拼成 http://127.0.0.1:8080@evil.com/x（host 逃逸到外部主机）
        URI uri = PgVectorEmbeddingStoreImpl.buildFetchUri("/@evil.com/x?size=1", 8080);

        assertNotNull(uri);
        assertEquals("127.0.0.1", uri.getHost());
        assertEquals(8080, uri.getPort());
        assertEquals("/@evil.com/x", uri.getPath());
        assertEquals("size=1", uri.getQuery());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://169.254.169.254/latest/meta-data/",
            "http://127.0.0.1:8080/admin",
            "http://10.1.2.3/secret",
            "http://192.168.1.1/",
            "http://172.16.0.9/",
            "http://[::1]/",
            "http://[::ffff:10.0.0.1]/",
            "http://localhost:9000/",
            "http://[fd00::1]/",
            "http://100.64.0.1/",
            "http://example.com /x"    // 含空格的非法 URI
    })
    void buildFetchUriRejectsInternalMetadataAndInvalidUrls(String imageUrl) {
        assertNull(PgVectorEmbeddingStoreImpl.buildFetchUri(imageUrl, 8080));
    }

    @Test
    void buildFetchUriAcceptsPublicAbsoluteUrl() {
        // 数字字面量由 InetAddress 本地解析，不发起真实 DNS 查询
        URI uri = PgVectorEmbeddingStoreImpl.buildFetchUri("http://8.8.8.8/a.png", 8080);

        assertNotNull(uri);
        assertEquals("8.8.8.8", uri.getHost());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1", "0.0.0.0", "169.254.169.254", "10.0.0.5",
            "172.16.0.1", "192.168.1.1", "100.64.1.1", "224.0.0.1",
            "::1", "fe80::1", "fd00::1", "::ffff:10.0.0.1"
    })
    void isPublicAddressRejectsReservedRanges(String literal) throws Exception {
        assertFalse(PgVectorEmbeddingStoreImpl.isPublicAddress(InetAddress.getByName(literal)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "172.32.1.1", "100.128.0.1", "2606:4700::1111"})
    void isPublicAddressAcceptsPublicAddresses(String literal) throws Exception {
        assertTrue(PgVectorEmbeddingStoreImpl.isPublicAddress(InetAddress.getByName(literal)));
    }

    @Test
    void resolveRedirectTargetRejectsInternalRedirect() {
        URI current = URI.create("http://8.8.8.8/a.png");

        assertNull(PgVectorEmbeddingStoreImpl.resolveRedirectTarget(
                current, "http://169.254.169.254/latest/meta-data/", 3));
        assertNull(PgVectorEmbeddingStoreImpl.resolveRedirectTarget(
                current, "http://127.0.0.1:8080/admin", 3));
    }

    @Test
    void resolveRedirectTargetAllowsSameHostRelativeLocation() {
        URI current = URI.create("http://8.8.8.8/a.png");

        URI next = PgVectorEmbeddingStoreImpl.resolveRedirectTarget(current, "/b.png", 3);

        assertNotNull(next);
        assertEquals("http://8.8.8.8/b.png", next.toString());
    }

    @Test
    void resolveRedirectTargetRejectsWhenHopsExhaustedOrBlank() {
        URI current = URI.create("http://8.8.8.8/a.png");

        assertNull(PgVectorEmbeddingStoreImpl.resolveRedirectTarget(current, "/b.png", 0));
        assertNull(PgVectorEmbeddingStoreImpl.resolveRedirectTarget(current, "  ", 3));
    }

    @Test
    void toBase64ImageContentBlocksMetadataEndpointBeforeAnyIo() {
        // 169.254.169.254 为数字字面量，校验在建立任何连接之前完成
        assertNull(vectorStore.toBase64ImageContent("http://169.254.169.254/latest/meta-data/"));
    }

    @Test
    void toBase64ImageContentParsesDataUriDirectly() {
        assertNotNull(vectorStore.toBase64ImageContent("data:image/png;base64,aGVsbG8="));
    }
}
