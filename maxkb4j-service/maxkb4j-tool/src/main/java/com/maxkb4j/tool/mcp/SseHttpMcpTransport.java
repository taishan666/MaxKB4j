package com.maxkb4j.tool.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.mcp.client.McpCallContext;
import dev.langchain4j.mcp.client.McpHeadersSupplier;
import dev.langchain4j.mcp.client.logging.McpLoggers;
import dev.langchain4j.mcp.client.transport.McpJson;
import dev.langchain4j.mcp.client.transport.McpOperationHandler;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.protocol.McpClientMessage;
import dev.langchain4j.mcp.protocol.McpInitializationNotification;
import dev.langchain4j.mcp.protocol.McpInitializeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Custom MCP transport implementing the legacy HTTP+SSE protocol
 * (MCP 2024-11-05 / 2025-03-26 spec).
 * <p>
 * langchain4j-mcp removed the SSE-based HttpMcpTransport in 1.20.0-beta30
 * (only Streamable HTTP / Stdio / WebSocket remain). This class re-implements
 * it on top of the JDK {@link HttpClient}, mirroring the removed upstream
 * implementation:
 * <ol>
 *   <li>the client opens a long-lived SSE channel by GETting the sseUrl;</li>
 *   <li>the server delivers the message endpoint (POST URL) via an
 *       {@code endpoint} event;</li>
 *   <li>all JSON-RPC messages are then POSTed to that endpoint; the server
 *       answers with HTTP 202 and the actual response arrives as a
 *       {@code message} event on the SSE channel;</li>
 *   <li>the {@code Mcp-Session-Id} response header of initialize (if present)
 *       is sent on subsequent requests;</li>
 *   <li>when the channel dies the {@code onFailure} callback is triggered so
 *       that {@code DefaultMcpClient} re-invokes {@code start()} according to
 *       its {@code reconnectInterval}; reconnection resumes from
 *       {@code Last-Event-ID}. A channel that accepts the GET but never
 *       announces its endpoint within {@code endpointTimeout} is torn down
 *       and reported the same way, so the client keeps retrying instead of
 *       staying wedged on a half-dead channel.</li>
 * </ol>
 */
public class SseHttpMcpTransport implements McpTransport {

    private static final Logger LOG = LoggerFactory.getLogger(SseHttpMcpTransport.class);

    private final String sseUrl;
    private final McpHeadersSupplier customHeadersSupplier;
    private final boolean logRequests;
    private final boolean logResponses;
    private final Logger trafficLog;
    private final Duration endpointTimeout;
    private final HttpClient httpClient;

    private volatile McpOperationHandler operationHandler;
    private volatile Runnable onFailureCallback;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private volatile boolean sseChannelEstablished;

    /**
     * POST endpoint delivered by the server's "endpoint" event; no request may
     * be sent before it is known.
     */
    private volatile String postUrl;
    private volatile CompletableFuture<String> postUrlFuture = new CompletableFuture<>();
    private final AtomicReference<String> mcpSessionId = new AtomicReference<>();
    private final AtomicReference<String> lastEventId = new AtomicReference<>();
    /** whether an SSE channel was ever established (distinguishes first-connect from reconnect failures) */
    private final AtomicBoolean everConnected = new AtomicBoolean(false);
    /** subscriber of the currently active SSE channel */
    private volatile SseChannelSubscriber activeSubscriber;

    public SseHttpMcpTransport(Builder builder) {
        this.sseUrl = ValidationUtils.ensureNotNull(builder.sseUrl, "Missing SSE endpoint URL");
        this.logRequests = builder.logRequests;
        this.logResponses = builder.logResponses;
        this.trafficLog = Utils.getOrDefault(builder.logger, McpLoggers.traffic());
        this.endpointTimeout = Utils.getOrDefault(builder.endpointTimeout, Duration.ofSeconds(20));
        this.customHeadersSupplier = Utils.getOrDefault(builder.customHeadersSupplier, i -> Map.of());
        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(Utils.getOrDefault(builder.timeout, Duration.ofSeconds(30)))
                .version(HttpClient.Version.HTTP_1_1);
        if (builder.sslContext != null) {
            clientBuilder.sslContext(builder.sslContext);
        }
        this.httpClient = clientBuilder.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void start(McpOperationHandler operationHandler) {
        this.operationHandler = operationHandler;
        openSseChannel();
    }

    /**
     * Opens the long-lived SSE channel. The SSE protocol requires the server's
     * "endpoint" event (the POST URL) before any request can be sent, so this
     * method does not block; {@link #awaitPostUrl()} does the waiting.
     */
    private void openSseChannel() {
        if (closed.get()) {
            return;
        }
        // reset the endpoint wait state on every (re)connection attempt: after a
        // reconnect the server will announce a fresh endpoint
        this.postUrl = null;
        this.postUrlFuture = new CompletableFuture<>();
        this.sseChannelEstablished = false;

        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(sseUrl))
                .header("Accept", "text/event-stream")
                .GET();
        String lastId = lastEventId.get();
        if (lastId != null) {
            requestBuilder.header("Last-Event-ID", lastId);
        }
        Map<String, String> headers = customHeadersSupplier.apply(null);
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }
        SseChannelSubscriber subscriber = new SseChannelSubscriber();
        // cancel the channel of a superseded attempt (if any) so its
        // connection does not linger server-side
        SseChannelSubscriber obsolete = this.activeSubscriber;
        if (obsolete != null) {
            obsolete.cancel();
        }
        this.activeSubscriber = subscriber;
        httpClient.sendAsync(requestBuilder.build(), responseInfo -> {
            int statusCode = responseInfo.statusCode();
            Optional<String> contentType = responseInfo.headers().firstValue("Content-Type");
            if (statusCode >= 200 && statusCode < 300
                    && contentType.isPresent()
                    && contentType.get().contains("text/event-stream")) {
                sseChannelEstablished = true;
                everConnected.set(true);
                LOG.debug("SSE channel established at {}", sseUrl);
                return HttpResponse.BodySubscribers.fromLineSubscriber(subscriber);
            }
            // first connect failed: fail fast so callers don't wait for the
            // "endpoint" event forever; reconnect failed: trigger onFailure
            // again so the client keeps retrying at its own pace
            handleConnectFailure(new HttpException(statusCode,
                    "Failed to open SSE channel: unexpected status code " + statusCode
                            + " (Content-Type: " + contentType.orElse("absent") + ")"));
            return HttpResponse.BodySubscribers.discarding();
        }).exceptionally(t -> {
            handleConnectFailure(t);
            return null;
        });
    }

    private void handleConnectFailure(Throwable t) {
        postUrlFuture.completeExceptionally(t);
        if (everConnected.get()) {
            notifyFailure();
        }
    }

    /** channel died: reset session state and let DefaultMcpClient drive the reconnection */
    private void handleChannelEnd() {
        sseChannelEstablished = false;
        mcpSessionId.set(null);
        if (closed.get()) {
            return;
        }
        notifyFailure();
    }

    private void notifyFailure() {
        Runnable callback = this.onFailureCallback;
        if (callback != null) {
            callback.run();
        }
    }

    private CompletableFuture<String> awaitPostUrl() {
        String url = postUrl;
        if (url != null) {
            return CompletableFuture.completedFuture(url);
        }
        CompletableFuture<String> source = postUrlFuture;
        CompletableFuture<String> result = new CompletableFuture<>();
        source.whenComplete((u, t) -> {
            if (u != null) {
                result.complete(u);
            } else {
                result.completeExceptionally(t);
            }
        });
        result.orTimeout(endpointTimeout.toMillis(), TimeUnit.MILLISECONDS);
        result.whenComplete((u, t) -> {
            if (t instanceof TimeoutException) {
                handleEndpointTimeout(source);
            }
        });
        return result;
    }

    /**
     * The freshly opened channel did not deliver its "endpoint" event within
     * {@code endpointTimeout} (a half-dead server or a stalled proxy that
     * answered the SSE GET with 200 but never announces the POST URL).
     * Failing the initialize request alone is not enough: the half-open
     * channel would linger and no further failure would ever be reported,
     * so with the client's health check disabled the transport would stay
     * wedged on the dead channel forever. Tear the channel down and, once a
     * channel was established before, let {@code DefaultMcpClient} retry at
     * its own {@code reconnectInterval} — exactly like any other connection
     * failure.
     */
    private void handleEndpointTimeout(CompletableFuture<String> awaitedFuture) {
        if (closed.get()) {
            return;
        }
        // act only while this attempt is still the current one: a newer
        // (re)connection attempt may already have replaced the channel
        if (awaitedFuture != postUrlFuture || postUrl != null) {
            return;
        }
        LOG.warn("Timed out after {} waiting for the server's 'endpoint' event on {}",
                endpointTimeout, sseUrl);
        sseChannelEstablished = false;
        mcpSessionId.set(null);
        SseChannelSubscriber subscriber = this.activeSubscriber;
        this.activeSubscriber = null;
        if (subscriber != null) {
            subscriber.cancel();
        }
        // fail concurrent waiters instead of letting each hit its own timeout
        postUrlFuture.completeExceptionally(new TimeoutException(
                "Timed out waiting for the server's 'endpoint' event"));
        if (everConnected.get()) {
            notifyFailure();
        }
    }

    @Override
    public CompletableFuture<String> sendInitializeRequest(McpInitializeRequest operation) {
        return awaitPostUrl()
                .thenCompose(url -> postMessage(operation, url))
                .thenCompose(originalResponse -> postMessage(new McpInitializationNotification(), postUrl)
                        .thenApply(ignored -> originalResponse));
    }

    @Override
    public CompletableFuture<String> sendRequest(McpClientMessage operation) {
        return awaitPostUrl().thenCompose(url -> postMessage(operation, url));
    }

    @Override
    public CompletableFuture<String> sendRequest(McpCallContext context) {
        return sendRequest(context.message());
    }

    @Override
    public void sendMessage(McpClientMessage operation) {
        CompletableFuture<String> sent = sendRequest(operation);
        // notifications carry no id and get no response; failures are only logged
        sent.exceptionally(t -> {
            LOG.warn("Failed to send MCP notification: {}", operation.method, t);
            return null;
        });
    }

    @Override
    public void sendMessage(McpCallContext context) {
        sendMessage(context.message());
    }

    /**
     * POSTs a JSON-RPC message to the server-announced endpoint. The response
     * does not appear in the POST result (the server returns 202 with an empty
     * body); it arrives on the SSE channel and completes the future registered
     * via {@code expectResponse}.
     */
    private CompletableFuture<String> postMessage(McpClientMessage message, String url) {
        Long id = message.getId();
        String body = McpJson.serialize(message);
        if (logRequests) {
            trafficLog.info("Request: {}", body);
        }
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json,text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        String sessionId = mcpSessionId.get();
        if (sessionId != null && !(message instanceof McpInitializeRequest)) {
            requestBuilder.header("Mcp-Session-Id", sessionId);
        }
        Map<String, String> headers = customHeadersSupplier.apply(null);
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }

        CompletableFuture<String> future = new CompletableFuture<>();
        if (id != null) {
            operationHandler.expectResponse(id, future);
        }
        httpClient.sendAsync(requestBuilder.build(), responseInfo -> {
            int statusCode = responseInfo.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                future.completeExceptionally(new HttpException(statusCode,
                        "Unexpected status code: " + statusCode));
                return HttpResponse.BodySubscribers.discarding();
            }
            // spec 2025-03-25+: the initialize response carries Mcp-Session-Id
            if (message instanceof McpInitializeRequest) {
                Optional<String> sessionIdHeader = responseInfo.headers().firstValue("Mcp-Session-Id");
                if (sessionIdHeader.isPresent()) {
                    LOG.debug("Assigned MCP session ID: {}", sessionIdHeader.get());
                    mcpSessionId.set(sessionIdHeader.get());
                }
            }
            // notifications (no id) get no response; the accepted POST completes them
            if (id == null) {
                future.complete(null);
            }
            return HttpResponse.BodySubscribers.discarding();
        }).exceptionally(t -> {
            future.completeExceptionally(t);
            return null;
        });
        return future;
    }

    @Override
    public void checkHealth() {
        if (closed.get()) {
            throw new IllegalStateException("SSE transport is closed");
        }
        if (!sseChannelEstablished) {
            throw new IllegalStateException("SSE channel is not connected");
        }
    }

    @Override
    public void onFailure(Runnable actionOnFailure) {
        this.onFailureCallback = actionOnFailure;
    }

    @Override
    public void close() throws IOException {
        if (closed.getAndSet(true)) {
            return;
        }
        postUrlFuture.cancel(false);
        SseChannelSubscriber subscriber = this.activeSubscriber;
        if (subscriber != null) {
            subscriber.cancel();
        }
        httpClient.close();
    }

    /**
     * Line subscriber of the SSE channel: parses SSE event frames line by
     * line. The {@code endpoint} event yields the POST URL; every other event
     * (default {@code message}) has its data handed to
     * {@link McpOperationHandler#onMessage(String)}.
     */
    private class SseChannelSubscriber implements Flow.Subscriber<String> {

        private Flow.Subscription subscription;
        private final StringBuilder dataBuffer = new StringBuilder();
        private String eventType;
        private boolean hasData;

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }

        void cancel() {
            Flow.Subscription s = this.subscription;
            if (s != null) {
                s.cancel();
            }
        }

        @Override
        public void onNext(String line) {
            subscription.request(1);
            processLine(line);
        }

        private void processLine(String line) {
            if (line == null) {
                return;
            }
            // tolerate \r\n line endings
            if (line.endsWith("\r")) {
                line = line.substring(0, line.length() - 1);
            }
            // empty line: end of the event frame, dispatch it
            if (line.isEmpty()) {
                dispatchEvent();
                return;
            }
            // lines starting with a colon are comments
            if (line.startsWith(":")) {
                return;
            }
            int colonIndex = line.indexOf(':');
            String field = colonIndex < 0 ? line : line.substring(0, colonIndex);
            // SSE spec: strip at most one leading space after the colon
            String value = colonIndex < 0 ? "" : line.substring(colonIndex + 1);
            if (value.startsWith(" ")) {
                value = value.substring(1);
            }
            switch (field) {
                case "data" -> {
                    if (hasData) {
                        dataBuffer.append('\n');
                    }
                    dataBuffer.append(value);
                    hasData = true;
                }
                case "event" -> eventType = value;
                case "id" -> lastEventId.set(value);
                default -> {
                }
            }
        }

        private void dispatchEvent() {
            if (!hasData) {
                eventType = null;
                return;
            }
            String data = dataBuffer.toString();
            String event = eventType == null ? "message" : eventType;
            dataBuffer.setLength(0);
            hasData = false;
            eventType = null;

            if ("endpoint".equals(event)) {
                String absoluteUrl = buildAbsolutePostUrl(data.trim());
                LOG.debug("Received the server's POST URL: {}", absoluteUrl);
                postUrl = absoluteUrl;
                postUrlFuture.complete(absoluteUrl);
            } else if ("message".equals(event)) {
                if (logResponses) {
                    trafficLog.info("SSE message received: {}", data);
                }
                try {
                    operationHandler.onMessage(data);
                } catch (RuntimeException e) {
                    LOG.warn("Failed to handle SSE event: {}", data, e);
                }
            } else {
                LOG.debug("Ignoring SSE event of type '{}'", event);
            }
        }

        /** resolves the endpoint URL (absolute or relative) against the sseUrl */
        private String buildAbsolutePostUrl(String endpointUrl) {
            try {
                URI endpoint = URI.create(endpointUrl);
                if (endpoint.isAbsolute()) {
                    return endpoint.toString();
                }
                URI base = URI.create(sseUrl);
                String basePath = base.getRawPath();
                // relative paths resolve against the parent path: /mcp/sse + messages -> /mcp/messages
                if (basePath != null && !basePath.endsWith("/")) {
                    int lastSlash = basePath.lastIndexOf('/');
                    String parent = lastSlash >= 0 ? basePath.substring(0, lastSlash + 1) : "/";
                    base = new URI(base.getScheme(), base.getRawAuthority(), parent,
                            base.getRawQuery(), base.getRawFragment());
                }
                return base.resolve(endpoint).toString();
            } catch (Exception e) {
                throw new IllegalArgumentException(
                        "Failed to resolve the POST URL received in the 'endpoint' event: " + endpointUrl, e);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            // a channel superseded by a newer (re)connection attempt must not
            // be reported as the current channel's failure
            if (SseHttpMcpTransport.this.activeSubscriber != this) {
                return;
            }
            LOG.warn("SSE channel failure", throwable);
            handleChannelEnd();
        }

        @Override
        public void onComplete() {
            if (SseHttpMcpTransport.this.activeSubscriber != this) {
                return;
            }
            LOG.debug("SSE channel closed by the server");
            handleChannelEnd();
        }
    }

    public static class Builder {

        private String sseUrl;
        private McpHeadersSupplier customHeadersSupplier;
        private Duration timeout;
        private Duration endpointTimeout;
        private boolean logRequests = false;
        private boolean logResponses = false;
        private Logger logger;
        private SSLContext sslContext;

        /**
         * The SSE endpoint URL (the client GETs it to open the SSE channel).
         */
        public Builder url(String sseUrl) {
            this.sseUrl = sseUrl;
            return this;
        }

        /**
         * Custom headers sent with every request.
         */
        public Builder customHeaders(Map<String, String> customHeaders) {
            this.customHeadersSupplier = i -> customHeaders;
            return this;
        }

        /**
         * Dynamic custom headers supplier, invoked for every request.
         */
        public Builder customHeaders(McpHeadersSupplier customHeadersSupplier) {
            this.customHeadersSupplier = customHeadersSupplier;
            return this;
        }

        /**
         * HTTP connect timeout.
         */
        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        /**
         * How long to wait for the server's "endpoint" event (the POST URL),
         * defaults to 20 seconds.
         */
        public Builder endpointTimeout(Duration endpointTimeout) {
            this.endpointTimeout = endpointTimeout;
            return this;
        }

        /**
         * Whether to log every request sent over this transport.
         */
        public Builder logRequests(boolean logRequests) {
            this.logRequests = logRequests;
            return this;
        }

        /**
         * Whether to log every response received over this transport.
         */
        public Builder logResponses(boolean logResponses) {
            this.logResponses = logResponses;
            return this;
        }

        /**
         * Custom logger for traffic logging (requests and responses).
         */
        public Builder logger(Logger logger) {
            this.logger = logger;
            return this;
        }

        /**
         * Custom SSLContext for HTTPS connections (e.g. private CAs).
         */
        public Builder sslContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public SseHttpMcpTransport build() {
            return new SseHttpMcpTransport(this);
        }
    }

    /**
     * @deprecated use {@link #sendInitializeRequest(McpInitializeRequest)}
     */
    @Deprecated(since = "1.20.0", forRemoval = true)
    @Override
    public CompletableFuture<JsonNode> initialize(McpInitializeRequest request) {
        return McpJson.map(sendInitializeRequest(request), McpJson::parse);
    }

    /**
     * @deprecated use {@link #sendRequest(McpClientMessage)}
     */
    @Deprecated(since = "1.20.0", forRemoval = true)
    @Override
    public CompletableFuture<JsonNode> executeOperationWithResponse(McpClientMessage request) {
        return McpJson.map(sendRequest(request), McpJson::parse);
    }

    /**
     * @deprecated use {@link #sendRequest(McpCallContext)}
     */
    @Deprecated(since = "1.20.0", forRemoval = true)
    @Override
    public CompletableFuture<JsonNode> executeOperationWithResponse(McpCallContext context) {
        return McpJson.map(sendRequest(context), McpJson::parse);
    }

    /**
     * @deprecated use {@link #sendMessage(McpClientMessage)}
     */
    @Deprecated(since = "1.20.0", forRemoval = true)
    @Override
    public void executeOperationWithoutResponse(McpClientMessage request) {
        sendMessage(request);
    }

    /**
     * @deprecated use {@link #sendMessage(McpCallContext)}
     */
    @Deprecated(since = "1.20.0", forRemoval = true)
    @Override
    public void executeOperationWithoutResponse(McpCallContext context) {
        sendMessage(context);
    }
}
