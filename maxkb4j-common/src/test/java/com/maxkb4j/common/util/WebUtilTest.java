package com.maxkb4j.common.util;

import com.maxkb4j.common.props.SystemProperties;
import com.maxkb4j.common.util.SpringUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：客户端 IP 提取的防伪造逻辑（X-Forwarded-For 为追加式头部，链首可被客户端伪造）。
 */
class WebUtilTest {

    @AfterEach
    void resetSpringContext() {
        new SpringUtil().setApplicationContext(null);
    }

    @Test
    void nullRequest_returnsEmptyString() {
        assertThat(WebUtil.getIP(null)).isEqualTo("");
    }

    @Test
    void directClient_withForgedForwardHeaders_usesRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.7");
        request.addHeader("x-forwarded-for", "1.2.3.4");
        request.addHeader("x-real-ip", "1.2.3.4");
        assertThat(WebUtil.getIP(request)).isEqualTo("203.0.113.7");
    }

    @Test
    void trustedLoopbackProxy_picksAppendedHopNotForgedChainHead() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        // 客户端伪造链首 1.2.3.4，可信代理在链尾追加了它观测到的真实客户端 IP
        request.addHeader("x-forwarded-for", "1.2.3.4, 198.51.100.9");
        assertThat(WebUtil.getIP(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void trustedLoopbackProxy_joinsMultiLineForwardHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("x-forwarded-for", "1.2.3.4");
        request.addHeader("x-forwarded-for", "198.51.100.9");
        assertThat(WebUtil.getIP(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void trustedLoopbackProxy_fallsBackToRealIpHeader() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("x-real-ip", "198.51.100.9");
        assertThat(WebUtil.getIP(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void trustedProxy_withoutForwardHeaders_returnsRemoteAddr() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        assertThat(WebUtil.getIP(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void configuredTrustedProxies_skipTrustedHopsAndNoiseFromRight() {
        registerTrustedProxies(List.of("172.16.0.0/12", "10.0.0.2"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.16.0.5");
        // 右侧依次为：注入噪声（非法 IP）、真实客户端、可信代理 10.0.0.2；链首 1.2.3.4 为伪造值
        request.addHeader("x-forwarded-for", "1.2.3.4, 198.51.100.9, 10.0.0.2, <script>alert(1)</script>");
        assertThat(WebUtil.getIP(request)).isEqualTo("198.51.100.9");
    }

    @Test
    void configuredTrustedProxies_forgedHeaderFromUnmatchedPeer_ignored() {
        registerTrustedProxies(List.of("10.0.0.0/8"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 172.16.0.5 不在可信列表内，伪造头一律忽略
        request.setRemoteAddr("172.16.0.5");
        request.addHeader("x-forwarded-for", "1.2.3.4");
        assertThat(WebUtil.getIP(request)).isEqualTo("172.16.0.5");
    }

    @Test
    void isTrustedProxy_matchesLoopbackAndConfiguredEntries() {
        assertThat(WebUtil.isTrustedProxy("127.0.0.1")).isTrue();
        assertThat(WebUtil.isTrustedProxy("::1")).isTrue();
        assertThat(WebUtil.isTrustedProxy("0:0:0:0:0:0:0:1")).isTrue();
        assertThat(WebUtil.isTrustedProxy("198.51.100.9")).isFalse();

        List<String> config = List.of("172.16.0.0/12", "10.0.0.2", "fd00::1");
        assertThat(WebUtil.isTrustedProxy("172.16.0.5", config)).isTrue();
        assertThat(WebUtil.isTrustedProxy("172.31.255.255", config)).isTrue();
        assertThat(WebUtil.isTrustedProxy("172.32.0.1", config)).isFalse();
        assertThat(WebUtil.isTrustedProxy("10.0.0.2", config)).isTrue();
        assertThat(WebUtil.isTrustedProxy("10.0.0.3", config)).isFalse();
        assertThat(WebUtil.isTrustedProxy("FD00::1", config)).isTrue();
        assertThat(WebUtil.isTrustedProxy("", config)).isFalse();
        assertThat(WebUtil.isTrustedProxy(null, config)).isFalse();
    }

    @Test
    void isTrustedProxy_ignoresInvalidCidrEntries() {
        assertThat(WebUtil.isTrustedProxy("10.0.0.1", List.of("10.0.0.0/33"))).isFalse();
        assertThat(WebUtil.isTrustedProxy("10.0.0.1", List.of("10.0.0.0/abc"))).isFalse();
        assertThat(WebUtil.isTrustedProxy("10.0.0.1", List.of("10.0.0.0/"))).isFalse();
        assertThat(WebUtil.isTrustedProxy("10.0.0.1", List.of("0.0.0.0/0"))).isTrue();
    }

    @Test
    void isValidIp_acceptsIpv4AndIpv6_rejectsInjectedStrings() {
        assertThat(WebUtil.isValidIp("1.2.3.4")).isTrue();
        assertThat(WebUtil.isValidIp("255.255.255.255")).isTrue();
        assertThat(WebUtil.isValidIp("256.1.1.1")).isFalse();
        assertThat(WebUtil.isValidIp("1.2.3")).isFalse();
        assertThat(WebUtil.isValidIp("1.2.3.4.5")).isFalse();
        assertThat(WebUtil.isValidIp("::1")).isTrue();
        assertThat(WebUtil.isValidIp("2001:db8::1")).isTrue();
        assertThat(WebUtil.isValidIp("::ffff:192.0.2.1")).isTrue();
        assertThat(WebUtil.isValidIp("<script>alert(1)</script>")).isFalse();
        assertThat(WebUtil.isValidIp("unknown")).isFalse();
        assertThat(WebUtil.isValidIp("")).isFalse();
        assertThat(WebUtil.isValidIp(null)).isFalse();
    }

    private void registerTrustedProxies(List<String> proxies) {
        SystemProperties props = new SystemProperties();
        props.setTrustedProxies(proxies);
        ConfigurableApplicationContext context = Mockito.mock(ConfigurableApplicationContext.class);
        Mockito.when(context.getBean(SystemProperties.class)).thenReturn(props);
        new SpringUtil().setApplicationContext(context);
    }
}
