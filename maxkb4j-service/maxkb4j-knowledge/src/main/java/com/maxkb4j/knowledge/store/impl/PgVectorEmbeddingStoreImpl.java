package com.maxkb4j.knowledge.store.impl;

import com.maxkb4j.common.util.BatchUtil;
import com.maxkb4j.knowledge.consts.SourceType;
import com.maxkb4j.knowledge.entity.EmbeddingEntity;
import com.maxkb4j.knowledge.retrieval.SearchRequest;
import com.maxkb4j.knowledge.service.KnowledgeModelService;
import com.maxkb4j.knowledge.vo.TextChunkVO;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ContentType;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.request.EmbeddingInput;
import dev.langchain4j.model.embedding.request.EmbeddingRequest;
import dev.langchain4j.model.embedding.response.EmbeddingResponse;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.pgvector.DefaultMetadataStorageConfig;
import dev.langchain4j.store.embedding.pgvector.MetadataStorageMode;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * Langchain4j {@link PgVectorEmbeddingStore} 实现的向量后端。
 * <p>检索结果得分与 minScore 阈值统一采用 langchain4j 的 {@code [0,1]} 归一化区间
 * （即 {@code (余弦相似度 + 1) / 2}），与 {@link FullTextStoreImpl} 保持同一量纲，
 * 便于 {@link CompositeStoreImpl} 跨后端融合。</p>
 */
@Slf4j
@Component("vectorStore")
@RequiredArgsConstructor
public class PgVectorEmbeddingStoreImpl extends BaseStoreImpl {

    /**
     * 写入/检索 metadata 使用的字段名，与检索过滤条件保持一致。
     */
    private static final String META_KNOWLEDGE_ID = "knowledgeId";
    private static final String META_DOCUMENT_ID = "documentId";
    private static final String META_SOURCE_ID = "sourceId";
    private static final String META_SOURCE_TYPE = "sourceType";
    private static final String FIND_EMBEDDING_TABLES_SQL = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = current_schema() AND table_name LIKE 'embedding_%'
            """;
    private static final Pattern EMBEDDING_TABLE_PATTERN = Pattern.compile("^embedding_(\\d+)$");
    /**
     * 从Markdown文本中提取所有图片URL
     */
    private static final Pattern MD_IMAGE_PATTERN = Pattern.compile("!\\[[^]]*]\\(([^)\\s]+)(?:\\s+\"[^\"]*\")?\\)");
    /**
     * 下载图片用于转 Base64 Data URI 时的连接/读取超时（毫秒）。
     */
    private static final int IMAGE_CONNECT_TIMEOUT_MS = 5000;
    private static final int IMAGE_READ_TIMEOUT_MS = 10000;
    /**
     * 允许抓取的绝对地址 URL scheme，其余 scheme（file/ftp/jar 等）一律拒绝。
     */
    private static final Set<String> ALLOWED_URL_SCHEMES = Set.of("http", "https");
    /**
     * 单张图片抓取的最大字节数：防止超大响应撑爆堆内存（旧实现 readAllBytes 无上限）。
     */
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    /**
     * 手动跟随重定向的最大跳数，每一跳都重新校验目标地址，防止公网地址 302 转向内网。
     */
    private static final int MAX_REDIRECT_HOPS = 3;
    private final KnowledgeModelService knowledgeModelService;
    private final DataSource dataSource;
    /**
     * 按 embedding 维度缓存 PgVectorEmbeddingStore 实例。
     * <p>原先的 {@code public static HashMap} 既线程不安全，又因 {@code getOrDefault} 而从未真正缓存（每次新建）。
     * 这里改为实例字段 + {@link ConcurrentHashMap#computeIfAbsent} 保证原子初始化与缓存命中。</p>
     */
    private final Map<Integer, EmbeddingStore<TextSegment>> stores = new ConcurrentHashMap<>();
    @Value("${vector.store.batch-size:10}")
    private int batchSize = 10;
    @Value("${vector.store.retry-times:3}")
    private int retryTimes = 3;
    @Value("${vector.store.retry-delay-ms:1000}")
    private int retryDelayMs = 1000;
    /**
     * 项目服务端口，用于将相对图片路径拼接为可访问的完整地址。
     */
    @Value("${server.port:8080}")
    private int serverPort = 8080;

    /**
     * 将 langchain4j 归一化到 {@code [0,1]} 的相似度还原为 {@code [-1,1]} 的余弦相似度，
     * 与 {@link BaseStoreImpl} 中 {@code (minScore + 1.0) / 2.0} 的归一化互为逆运算。
     */
    private static double denormalizeScore(double score) {
        return 2.0 * score - 1.0;
    }

    private static double normalizeScore(double score) {
        return (score + 1.0) / 2.0;
    }

    private EmbeddingStore<TextSegment> build(int dimension) {
        return PgVectorEmbeddingStore.datasourceBuilder()
                .datasource(dataSource)
                .table("embedding_" + dimension)
                .dimension(dimension)
                .searchMode(PgVectorEmbeddingStore.SearchMode.VECTOR)
                .metadataStorageConfig(DefaultMetadataStorageConfig.builder()
                        .storageMode(MetadataStorageMode.COMBINED_JSONB)
                        .columnDefinitions(Collections.singletonList("metadata JSONB NULL"))
                        .build())
                .build();
    }

    public EmbeddingStore<TextSegment> get(int dimension) {
        return stores.computeIfAbsent(dimension, this::build);
    }

    @Override
    public void upsert(EmbeddingModel model, List<EmbeddingEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return;
        }
        List<EmbeddingEntity> validEntities = entities.stream()
                .filter(e -> e != null && StringUtils.isNotBlank(e.getContent()))
                .toList();
        if (validEntities.isEmpty()) {
            return;
        }
        EmbeddingStore<TextSegment> store = get(model.dimension());
        log.debug("Processing {} valid entities for embedding", validEntities.size());
        List<EmbeddingEntity> failedEntities = new CopyOnWriteArrayList<>();
        BatchUtil.protectBach(validEntities, batchSize, batch -> {
            try {
                processBatchWithRetry(model, store, batch);
            } catch (Exception e) {
                log.error("Failed to process batch after retries: {}", e.getMessage(), e);
                failedEntities.addAll(batch);
            }
        });
        if (!failedEntities.isEmpty()) {
            throw new RuntimeException("向量写入失败，共 " + failedEntities.size() + " 条数据未成功索引，请检查 embedding 服务后重试");
        }
    }

    /**
     * 分批处理并在失败时按指数退避重试。
     */
    private void processBatchWithRetry(EmbeddingModel model, EmbeddingStore<TextSegment> store, List<EmbeddingEntity> batch) {
        Exception lastException = null;
        Set<ContentType> supportedContentTypes=model.supportedContentTypes();
        for (int attempt = 1; attempt <= retryTimes; attempt++) {
            try {
                List<TextSegment> textSegments = batch.stream().map(this::toTextSegment).toList();
                if(supportedContentTypes.contains(ContentType.IMAGE)){
                    List<EmbeddingInput> inputs = new ArrayList<>();
                    textSegments.forEach(segment -> {
                        String text = segment.text();
                        List<Content> contents = new ArrayList<>();
                        contents.add(TextContent.from(text));
                        List<String> imageUrls = extractImageUrls(text);
                        for (String imageUrl : imageUrls) {
                            ImageContent imageContent = toBase64ImageContent(imageUrl);
                            if (imageContent != null) {
                                contents.add(imageContent);
                            }
                        }
                        inputs.add(EmbeddingInput.from(contents));
                    });
                    EmbeddingResponse res = model.embed(EmbeddingRequest.builder().inputs(inputs).build());
                    store.addAll(res.embeddings(), textSegments);
                }else {
                    Response<List<Embedding>> res= model.embedAll(textSegments);
                    store.addAll(res.content(), textSegments);
                }
                return;
            } catch (Exception e) {
                lastException = e;
                log.warn("Batch processing attempt {} failed: {}", attempt, e.getMessage());
                if (attempt < retryTimes && !backoff(attempt)) {
                    break;
                }
            }
        }
        if (lastException != null) {
            log.error("All {} retry attempts failed for batch of size {}", retryTimes, batch.size());
            throw new RuntimeException("Batch processing failed after retries", lastException);
        }
    }

    private List<String> extractImageUrls(String markdownText) {
        List<String> urls = new ArrayList<>();
        if (markdownText == null || markdownText.isEmpty()) {
            return urls;
        }
        Matcher matcher = MD_IMAGE_PATTERN.matcher(markdownText);
        while (matcher.find()) {
            String url = matcher.group(1).trim();
            // 去除可能包裹的尖括号 <url>
            if (url.startsWith("<") && url.endsWith(">")) {
                url = url.substring(1, url.length() - 1);
            }
            urls.add(url);
        }
        return urls;
    }

    /**
     * 下载图片并将其转换为 Base64 编码的 Data URI，封装进 {@link ImageContent}。
     * <p>{@link ImageContent#from(String, String)} 会以 {@code data:<mimeType>;base64,<data>}
     * 的形式保存，从而避免下游模型直接访问外链图片。</p>
     * <p>SSRF 加固：绝对地址仅接受解析到公网的 http/https 主机（拒绝回环/内网/云元数据端点），
     * 重定向手动跟随并在每一跳重新校验；读取按 {@value #MAX_IMAGE_BYTES} 字节硬上限截断，防止 OOM。</p>
     *
     * @param imageUrl 图片地址（http/https 外链，或已经是 data: 形式的 Data URI）
     * @return 转换后的 {@link ImageContent}；地址被拦截、下载或解析失败时返回 {@code null}
     */
    ImageContent toBase64ImageContent(String imageUrl) {
        if (StringUtils.isBlank(imageUrl)) {
            return null;
        }
        // 已经是 Base64 Data URI，直接解析复用
        if (imageUrl.startsWith("data:")) {
            return parseDataUri(imageUrl);
        }
        URI uri = buildFetchUri(imageUrl, serverPort);
        if (uri == null) {
            log.warn("Rejected unsafe or invalid image url: {}", imageUrl);
            return null;
        }
        try {
            return fetchAsImageContent(uri, MAX_REDIRECT_HOPS);
        } catch (Exception e) {
            log.warn("Failed to download image for base64 encoding: {}, cause: {}", uri, e.getMessage());
            return null;
        }
    }

    /**
     * 抓取图片并转 Base64。关闭 JDK 的自动重定向（它不校验跳转目标），改为手动跟随：
     * 301/302/303/307/308 每跳都经 {@link #resolveRedirectTarget} 重新校验 scheme 与主机。
     */
    private ImageContent fetchAsImageContent(URI uri, int remainingRedirects) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setConnectTimeout(IMAGE_CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(IMAGE_READ_TIMEOUT_MS);
        connection.setInstanceFollowRedirects(false);
        int status = connection.getResponseCode();
        if (isRedirect(status)) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            URI next = resolveRedirectTarget(uri, location, remainingRedirects);
            if (next == null) {
                return null;
            }
            return fetchAsImageContent(next, remainingRedirects - 1);
        }
        if (status < HttpURLConnection.HTTP_OK || status >= HttpURLConnection.HTTP_MULT_CHOICE) {
            connection.disconnect();
            log.debug("Image url returned http status {}: {}", status, uri);
            return null;
        }
        try {
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_IMAGE_BYTES) {
                log.warn("Image exceeds size limit ({} bytes): {}", contentLength, uri);
                return null;
            }
            byte[] bytes;
            String contentType;
            try (InputStream in = connection.getInputStream()) {
                // 多读 1 字节探测超限：恰好读满 MAX_IMAGE_BYTES 时无法区分“正好”与“被截断”
                bytes = in.readNBytes((int) MAX_IMAGE_BYTES + 1);
                contentType = connection.getContentType();
            }
            if (bytes.length > MAX_IMAGE_BYTES) {
                log.warn("Image exceeds size limit ({} bytes max): {}", MAX_IMAGE_BYTES, uri);
                return null;
            }
            String mimeType = resolveMimeType(contentType, uri.toString());
            return ImageContent.from(Base64.getEncoder().encodeToString(bytes), mimeType);
        } finally {
            connection.disconnect();
        }
    }

    private static boolean isRedirect(int status) {
        return status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_SEE_OTHER
                || status == 307
                || status == 308;
    }

    /**
     * 解析重定向目标：仅接受 http/https；跳转同主机沿用首轮校验结论，
     * 跨主机则必须通过公网地址校验（拒绝公网地址 302 转向内网/元数据端点）。
     *
     * @param remainingRedirects 剩余可跳次数，耗尽即拒绝
     */
    static URI resolveRedirectTarget(URI current, String location, int remainingRedirects) {
        if (StringUtils.isBlank(location) || remainingRedirects <= 0) {
            return null;
        }
        URI next;
        try {
            next = current.resolve(location.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
        String scheme = next.getScheme();
        if (scheme == null || !ALLOWED_URL_SCHEMES.contains(scheme.toLowerCase(Locale.ROOT))
                || StringUtils.isBlank(next.getHost())) {
            return null;
        }
        if (!StringUtils.equalsIgnoreCase(current.getHost(), next.getHost())
                && !isAllowedPublicHost(next)) {
            return null;
        }
        return next;
    }

    /**
     * 将 markdown 图片地址解析为受控抓取地址：
     * <ul>
     *   <li>相对路径：用多参 {@link URI} 构造器拼接本机服务地址——路径被 percent-encoding，
     *       相对内容中的 {@code @}、{@code ?} 等字符无法篡改 host/端口语义（旧实现的字符串
     *       拼接可被 {@code /@evil.com} 之类注入 userinfo 逃逸到外部主机）；本机地址视为可信。</li>
     *   <li>绝对地址：仅接受 http/https，且主机必须解析为公网地址（拒绝回环/内网/链路本地/
     *       CGNAT 等保留网段，阻断对 169.254.169.254 等云元数据端点的探测）。</li>
     * </ul>
     * 不满足条件的地址返回 {@code null}。
     */
    static URI buildFetchUri(String imageUrl, int serverPort) {
        String trimmed = imageUrl.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            URI uri = parseUriQuietly(trimmed);
            if (uri == null
                    || !ALLOWED_URL_SCHEMES.contains(uri.getScheme())
                    || StringUtils.isBlank(uri.getHost())
                    || !isAllowedPublicHost(uri)) {
                return null;
            }
            return uri;
        }
        String path = trimmed.startsWith("./") ? trimmed.substring(1) : trimmed;
        String query = null;
        int separatorIdx = path.indexOf('?');
        if (separatorIdx >= 0) {
            query = path.substring(separatorIdx + 1);
            path = path.substring(0, separatorIdx);
        }
        // fragment 不参与服务端请求，直接丢弃
        separatorIdx = path.indexOf('#');
        if (separatorIdx >= 0) {
            path = path.substring(0, separatorIdx);
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        try {
            return new URI("http", null, "127.0.0.1", serverPort, path, query, null);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static URI parseUriQuietly(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            return null;
        }
    }

    /**
     * 判断 URI 主机是否可安全抓取：解析全部 IP（防止内网域名绕过），
     * 任一 IP 落在保留网段即拒绝。解析失败视为不安全。
     */
    static boolean isAllowedPublicHost(URI uri) {
        String host = uri.getHost();
        if (StringUtils.isBlank(host)) {
            return false;
        }
        // URI.getHost() 对 IPv6 字面量返回带方括号的形式，InetAddress 需要去括号后的字面量
        if (host.startsWith("[") && host.endsWith("]")) {
            host = host.substring(1, host.length() - 1);
        }
        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            if (addresses.length == 0) {
                return false;
            }
            for (InetAddress address : addresses) {
                if (!isPublicAddress(address)) {
                    return false;
                }
            }
            return true;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 判断地址是否为可公网路由的安全地址：回环/内网/链路本地（含云元数据端点
     * 169.254.0.0/16）/CGNAT/组播等保留网段一律拒绝。
     */
    static boolean isPublicAddress(InetAddress address) {
        if (address instanceof Inet4Address ipv4) {
            return isPublicIpv4(ipv4.getAddress());
        }
        if (address instanceof Inet6Address ipv6) {
            byte[] bytes = ipv6.getAddress();
            // IPv4-mapped（::ffff:0:0/96）按内嵌 IPv4 判定，防止 [::ffff:10.0.0.1] 绕过网段检查
            boolean ipv4Mapped = true;
            for (int i = 0; i < 12; i++) {
                if (i < 10 ? bytes[i] != 0 : (bytes[i] & 0xFF) != 0xFF) {
                    ipv4Mapped = false;
                    break;
                }
            }
            if (ipv4Mapped) {
                return isPublicIpv4(new byte[]{bytes[12], bytes[13], bytes[14], bytes[15]});
            }
            if (ipv6.isAnyLocalAddress() || ipv6.isLoopbackAddress()
                    || ipv6.isLinkLocalAddress() || ipv6.isSiteLocalAddress()
                    || ipv6.isMulticastAddress()) {
                return false;
            }
            // fc00::/7 唯一本地地址（ULA）不在标准 flag 方法覆盖范围内
            int first = bytes[0] & 0xFF;
            return first < 0xFC || first > 0xFD;
        }
        return false;
    }

    private static boolean isPublicIpv4(byte[] octets) {
        int o0 = octets[0] & 0xFF;
        int o1 = octets[1] & 0xFF;
        int o2 = octets[2] & 0xFF;
        return !(o0 == 0                                        // 0.0.0.0/8 “本网络”
                || o0 == 10                                     // 10.0.0.0/8 内网
                || (o0 == 100 && o1 >= 64 && o1 <= 127)         // 100.64.0.0/10 CGNAT
                || o0 == 127                                    // 127.0.0.0/8 回环
                || (o0 == 169 && o1 == 254)                     // 169.254.0.0/16 链路本地（含云元数据端点）
                || (o0 == 172 && o1 >= 16 && o1 <= 31)          // 172.16.0.0/12 内网
                || (o0 == 192 && o1 == 168)                     // 192.168.0.0/16 内网
                || (o0 == 192 && o1 == 0 && o2 == 2)            // 192.0.2.0/24 TEST-NET-1
                || (o0 == 198 && (o1 == 18 || o1 == 19))        // 198.18.0.0/15 基准测试
                || o0 >= 224);                                  // 组播 224/4 与保留 240/4、255.255.255.255
    }

    /**
     * 解析形如 {@code data:image/png;base64,xxxx} 的 Data URI 为 {@link ImageContent}。
     */
    private ImageContent parseDataUri(String dataUri) {
        try {
            int commaIdx = dataUri.indexOf(',');
            if (commaIdx < 0) {
                return null;
            }
            String meta = dataUri.substring(5, commaIdx); // 去掉 "data:" 前缀
            String base64Data = dataUri.substring(commaIdx + 1);
            String mimeType = "image/png";
            int semicolonIdx = meta.indexOf(';');
            if (semicolonIdx > 0) {
                mimeType = meta.substring(0, semicolonIdx);
            } else if (!meta.isBlank()) {
                mimeType = meta;
            }
            return ImageContent.from(base64Data, mimeType);
        } catch (Exception e) {
            log.warn("Failed to parse data uri for image content, cause: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 优先使用响应头 Content-Type，缺失或为通用二进制流时回退到按 URL 扩展名猜测。
     */
    private String resolveMimeType(String contentType, String imageUrl) {
        if (StringUtils.isNotBlank(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType.trim())) {
            int semicolonIdx = contentType.indexOf(';');
            return semicolonIdx > 0 ? contentType.substring(0, semicolonIdx).trim() : contentType.trim();
        }
        return guessMimeTypeFromUrl(imageUrl);
    }

    /**
     * 根据 URL 路径扩展名猜测图片 MIME 类型，无法识别时默认 {@code image/png}。
     */
    private String guessMimeTypeFromUrl(String imageUrl) {
        String path = imageUrl;
        int queryIdx = path.indexOf('?');
        if (queryIdx > 0) {
            path = path.substring(0, queryIdx);
        }
        int dotIdx = path.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < path.length() - 1) {
            String ext = path.substring(dotIdx + 1).toLowerCase(Locale.ROOT);
            switch (ext) {
                case "jpg":
                case "jpeg":
                    return "image/jpeg";
                case "png":
                    return "image/png";
                case "gif":
                    return "image/gif";
                case "webp":
                    return "image/webp";
                case "bmp":
                    return "image/bmp";
                case "svg":
                    return "image/svg+xml";
                default:
                    break;
            }
        }
        return "image/png";
    }

    /**
     * 将 {@link EmbeddingEntity} 转换为带 metadata 的 {@link TextSegment}。
     */
    private TextSegment toTextSegment(EmbeddingEntity entity) {
        Metadata metadata = new Metadata();
        metadata.put(META_KNOWLEDGE_ID, entity.getKnowledgeId());
        if (entity.getDocumentId() != null) {
            metadata.put(META_DOCUMENT_ID, entity.getDocumentId());
        }
        metadata.put(META_SOURCE_ID, entity.getSourceId());
        metadata.put(META_SOURCE_TYPE, entity.getSourceType());
        return TextSegment.from(entity.getContent().trim(), metadata);
    }

    /**
     * 重试间隔（指数退避：retryDelayMs * 2^(attempt-1)）。
     * <p>抽成独立方法既消除 IDEA "Thread.sleep in a loop" 警告，也把中断处理收敛到一处。</p>
     *
     * @param attempt 当前已失败的尝试次数（从 1 开始）
     * @return true 表示正常等待结束，可继续下一次重试；false 表示被中断，调用方应立刻终止循环
     */
    private boolean backoff(int attempt) {
        try {
            Thread.sleep(retryDelayMs * (1L << (attempt - 1)));
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void deleteByProblemIds(String knowledgeId, List<String> problemIds) {
        if (problemIds == null || problemIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isEqualTo(knowledgeId)
                .and(metadataKey(META_SOURCE_TYPE).isEqualTo(String.valueOf(SourceType.PROBLEM)))
                .and(metadataKey(META_SOURCE_ID).isIn(problemIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByParagraphIds(String knowledgeId, List<String> paragraphIds) {
        if (paragraphIds == null || paragraphIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isEqualTo(knowledgeId)
                .and(metadataKey(META_SOURCE_TYPE).isEqualTo(String.valueOf(SourceType.PARAGRAPH)))
                .and(metadataKey(META_SOURCE_ID).isIn(paragraphIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByDocumentIds(String knowledgeId, List<String> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return;
        }
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isEqualTo(knowledgeId)
                .and(metadataKey(META_DOCUMENT_ID).isIn(documentIds));
        removeAllStores(filter);
    }

    @Override
    public void deleteByKnowledgeId(String knowledgeId) {
        if (knowledgeId == null) {
            return;
        }
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isEqualTo(knowledgeId);
        removeAllStores(filter);
    }

    /**
     * 按来源类型做向量检索：仅依据 {@link SearchRequest} 入参构造 langchain4j {@link Filter}，
     * 不再自行解析排除段落（该策略已上移到 {@link com.maxkb4j.knowledge.retriever.SearchOrchestrator}）。
     * <p>段落路按 excludeDocumentIds / excludeParagraphIds 过滤；问题路不应用这两类排除。</p>
     */
    @Override
    public List<TextChunkVO> searchBySource(SearchRequest request, int sourceType) {
        if (shouldShortCircuit(request)) {
            return Collections.emptyList();
        }
        EmbeddingModel embeddingModel = knowledgeModelService.getEmbeddingModel(request.getKnowledgeIds().getFirst());
        if (embeddingModel == null) {
            log.warn("No embedding model found for knowledge: {}", request.getKnowledgeIds().getFirst());
            return Collections.emptyList();
        }
        Embedding queryEmbedding = embeddingModel.embed(request.getQuery().trim()).content();
        EmbeddingStore<TextSegment> store = get(queryEmbedding.dimension());
        EmbeddingSearchResult<TextSegment> searchResult = store.search(buildSearchRequest(request, queryEmbedding, sourceType));
        return toTextChunkVOs(searchResult);
    }

    /**
     * 构造 langchain4j 检索请求：按 sourceType 组装过滤条件。minScore 阈值与检索结果得分
     * 统一采用 langchain4j 的 {@code [0,1]} 归一化区间，无需额外换算。
     */
    private EmbeddingSearchRequest buildSearchRequest(SearchRequest request, Embedding queryEmbedding, int sourceType) {
        Filter filter = metadataKey(META_KNOWLEDGE_ID).isIn(request.getKnowledgeIds());
        filter = filter.and(metadataKey(META_SOURCE_TYPE).isEqualTo(String.valueOf(sourceType)));
        if (sourceType == SourceType.PARAGRAPH) {
            if (!CollectionUtils.isEmpty(request.getExcludeDocumentIds())) {
                filter = filter.and(metadataKey(META_DOCUMENT_ID).isNotIn(request.getExcludeDocumentIds()));
            }
            if (!CollectionUtils.isEmpty(request.getExcludeParagraphIds())) {
                filter = filter.and(metadataKey(META_SOURCE_ID).isNotIn(request.getExcludeParagraphIds()));
            }
        }
        return EmbeddingSearchRequest.builder()
                .filter(filter)
                .query(request.getQuery().trim())
                .queryEmbedding(queryEmbedding)
                .maxResults(request.getTopK())
                .minScore(normalizeScore(request.getMinScore()))
                .build();
    }

    /**
     * 将检索命中统一转换为 {@link TextChunkVO}，score 直接采用 langchain4j 的 {@code [0,1]} 归一化得分，
     * 与全文库保持同一量纲。
     */
    private List<TextChunkVO> toTextChunkVOs(EmbeddingSearchResult<TextSegment> searchResult) {
        return searchResult.matches().stream().map(match -> {
            TextSegment segment = match.embedded();
            return new TextChunkVO(segment.metadata().getString(META_SOURCE_ID), denormalizeScore(match.score()));
        }).toList();
    }

    private void removeAllStores(Filter filter) {
        for (Integer dimension : discoverPersistedDimensions()) {
            get(dimension).removeAll(filter);
        }
    }

    private Set<Integer> discoverPersistedDimensions() {
        Set<Integer> dimensions = new HashSet<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_EMBEDDING_TABLES_SQL);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                Matcher matcher = EMBEDDING_TABLE_PATTERN.matcher(resultSet.getString(1));
                if (!matcher.matches()) {
                    continue;
                }
                try {
                    dimensions.add(Integer.valueOf(matcher.group(1)));
                } catch (NumberFormatException e) {
                    log.warn("Ignore embedding table with invalid dimension: {}", resultSet.getString(1));
                }
            }
            return dimensions;
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to discover persisted embedding tables", e);
        }
    }
}