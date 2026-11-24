package com.maxkb4j.system.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证码会话存储测试：多标签页共存、一次性消费、容量上限与旧格式兼容。
 */
class CaptchaSessionStoreTest {

    @Test
    void 多次取验证码不互相覆盖() {
        MockHttpSession session = new MockHttpSession();
        CaptchaSessionStore.store(session, "abcd");
        // 第二个标签页取新验证码后，第一个标签页的验证码仍待验证
        CaptchaSessionStore.store(session, "wxyz");

        assertThat(CaptchaSessionStore.drain(session)).containsExactly("abcd", "wxyz");
    }

    @Test
    void 登录尝试后验证码全部消费() {
        MockHttpSession session = new MockHttpSession();
        CaptchaSessionStore.store(session, "abcd");
        CaptchaSessionStore.store(session, "wxyz");

        CaptchaSessionStore.drain(session);

        assertThat(CaptchaSessionStore.drain(session)).isEmpty();
    }

    @Test
    void 超出上限时淘汰最旧验证码() {
        MockHttpSession session = new MockHttpSession();
        for (int i = 0; i < 6; i++) {
            CaptchaSessionStore.store(session, "code" + i);
        }

        assertThat(CaptchaSessionStore.drain(session)).containsExactly("code1", "code2", "code3", "code4", "code5");
    }

    @Test
    void 兼容旧版单字符串属性() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("captcha", "legacy");

        assertThat(CaptchaSessionStore.drain(session)).containsExactly("legacy");
    }

    @Test
    void 空会话取出为空() {
        MockHttpSession session = new MockHttpSession();

        assertThat(CaptchaSessionStore.drain(session)).isEmpty();
    }
}
