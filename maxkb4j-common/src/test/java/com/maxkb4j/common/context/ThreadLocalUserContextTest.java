package com.maxkb4j.common.context;

import com.maxkb4j.common.exception.UserIdentityException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ThreadLocalUserContext} 单元测试。
 * <p>验证身份快照的写入/读取/清理,以及 ThreadLocal 的线程隔离语义。
 * 不依赖 sa-token,证明业务层身份抽象可脱离 Web 上下文进行测试。
 *
 * @author tarzan
 */
class ThreadLocalUserContextTest {

    private final ThreadLocalUserContext userContext = new ThreadLocalUserContext();

    @BeforeEach
    void setUp() {
        userContext.clear();
    }

    @AfterEach
    void tearDown() {
        userContext.clear();
    }

    @Test
    void shouldBeNotLoginWhenEmpty() {
        assertThat(userContext.isLogin()).isFalse();
        assertThat(userContext.getLoginType()).isNull();
        assertThat(userContext.getExtra("applicationId")).isNull();
    }

    @Test
    void shouldReadAdminIdentityAfterSet() {
        userContext.set(new UserIdentity("u-admin-1", "admin", Map.of()));

        assertThat(userContext.isLogin()).isTrue();
        assertThat(userContext.getUserId()).isEqualTo("u-admin-1");
        assertThat(userContext.getLoginType()).isEqualTo("admin");
    }

    @Test
    void shouldReadUserExtrasAfterSet() {
        userContext.set(new UserIdentity("u-chat-1", "user",
                Map.of("applicationId", "app-9", "chatUserType", "ANONYMOUS_USER")));

        assertThat(userContext.getUserId()).isEqualTo("u-chat-1");
        assertThat(userContext.getExtra("applicationId")).isEqualTo("app-9");
        assertThat(userContext.getExtra("chatUserType")).isEqualTo("ANONYMOUS_USER");
        assertThat(userContext.getExtra("accessToken")).isNull();
    }

    @Test
    void shouldThrowWhenGetUserIdWithoutIdentity() {
        assertThatThrownBy(userContext::getUserId)
                .isInstanceOf(UserIdentityException.class);
    }

    @Test
    void shouldBeNotLoginAfterClear() {
        userContext.set(new UserIdentity("u-1", "admin", Map.of()));
        assertThat(userContext.isLogin()).isTrue();

        userContext.clear();

        assertThat(userContext.isLogin()).isFalse();
        assertThat(userContext.getLoginType()).isNull();
    }

    @Test
    void shouldBeIsolatedPerThread() throws Exception {
        userContext.set(new UserIdentity("main-user", "admin", Map.of()));
        assertThat(userContext.getUserId()).isEqualTo("main-user");

        Thread t = new Thread(() -> {
            // 新线程不应看到主线程的 ThreadLocal 身份
            assertThat(userContext.isLogin()).isFalse();
            assertThat(userContext.getExtra("applicationId")).isNull();
        });
        t.start();
        t.join();

        // 主线程身份不受子线程影响
        assertThat(userContext.getUserId()).isEqualTo("main-user");
    }
}
