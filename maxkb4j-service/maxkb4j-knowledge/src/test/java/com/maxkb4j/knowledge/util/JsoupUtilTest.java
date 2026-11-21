package com.maxkb4j.knowledge.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetAddress;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsoupUtilTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd",
            "ftp://8.8.8.8/resource",
            "gopher://8.8.8.8/resource",
            "http://127.0.0.1:8080/admin",
            "http://10.0.0.1/internal",
            "http://172.16.0.1/internal",
            "http://192.168.1.1/internal",
            "http://169.254.169.254/latest/meta-data/",
            "http://100.100.100.200/latest/meta-data/",
            "http://[::1]/",
            "http://[fd00::1]/",
            "http://localhost:8080/",
            "http://user:pass@8.8.8.8/"
    })
    void getDocumentRejectsUnsafeUrlsBeforeNetworkIo(String url) {
        assertThrows(IllegalArgumentException.class, () -> JsoupUtil.getDocument(url));
    }

    @Test
    void validateUrlAcceptsPublicHttpAndHttpsUrls() {
        assertEquals(URI.create("http://8.8.8.8/docs"), JsoupUtil.validateUrl("http://8.8.8.8/docs"));
        assertEquals(URI.create("https://1.1.1.1/docs"), JsoupUtil.validateUrl("https://1.1.1.1/docs"));
    }

    @Test
    void resolveRedirectValidatesEveryTarget() {
        URI current = URI.create("https://8.8.8.8/docs/index.html");

        assertEquals(URI.create("https://8.8.8.8/docs/next.html"),
                JsoupUtil.resolveRedirect(current, "next.html"));
        assertThrows(IllegalArgumentException.class, () -> JsoupUtil.resolveRedirect(
                current, "http://169.254.169.254/latest/meta-data/"));
        assertThrows(IllegalArgumentException.class, () -> JsoupUtil.resolveRedirect(
                current, "http://127.0.0.1:8080/admin"));
        assertThrows(IllegalArgumentException.class, () -> JsoupUtil.resolveRedirect(
                current, "file:///etc/passwd"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.0.0.0", "127.0.0.1", "169.254.169.254", "10.0.0.1",
            "172.16.0.1", "192.168.1.1", "100.64.0.1", "198.51.100.1",
            "203.0.113.1", "224.0.0.1", "::1", "fe80::1", "fd00::1",
            "::ffff:10.0.0.1"
    })
    void isPublicAddressRejectsNonPublicAddresses(String literal) throws Exception {
        assertFalse(JsoupUtil.isPublicAddress(InetAddress.getByName(literal)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"8.8.8.8", "1.1.1.1", "172.32.0.1", "100.128.0.1", "2606:4700::1111"})
    void isPublicAddressAcceptsPublicAddresses(String literal) throws Exception {
        assertTrue(JsoupUtil.isPublicAddress(InetAddress.getByName(literal)));
    }
}
