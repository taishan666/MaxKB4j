package com.maxkb4j.system.security;

import com.maxkb4j.common.exception.LoginException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 回归测试：登录失败限流器的阈值、锁定与成功重置行为。
 */
class LoginAttemptLimiterTest {

    private final LoginAttemptLimiter limiter = new LoginAttemptLimiter();

    @Test
    void belowThreshold_notLocked() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("user|1.1.1.1");
        }
        assertThatCode(() -> limiter.checkLocked("user|1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void reachingThreshold_locks() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("user|1.1.1.1");
        }
        assertThatThrownBy(() -> limiter.checkLocked("user|1.1.1.1"))
                .isInstanceOf(LoginException.class);
    }

    @Test
    void successResetsFailureCount() {
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("user|1.1.1.1");
        }
        limiter.recordSuccess("user|1.1.1.1");
        for (int i = 0; i < 4; i++) {
            limiter.recordFailure("user|1.1.1.1");
        }
        assertThatCode(() -> limiter.checkLocked("user|1.1.1.1")).doesNotThrowAnyException();
    }

    @Test
    void differentKeys_areIsolated() {
        for (int i = 0; i < 5; i++) {
            limiter.recordFailure("a|1.1.1.1");
        }
        assertThatCode(() -> limiter.checkLocked("b|2.2.2.2")).doesNotThrowAnyException();
    }

    @Test
    void nullKey_neverLocked() {
        for (int i = 0; i < 10; i++) {
            limiter.recordFailure(null);
        }
        assertThatCode(() -> limiter.checkLocked(null)).doesNotThrowAnyException();
    }
}
