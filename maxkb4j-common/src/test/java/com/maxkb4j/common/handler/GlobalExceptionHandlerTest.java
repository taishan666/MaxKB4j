package com.maxkb4j.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.jwt.error.SaJwtErrorCode;
import cn.dev33.satoken.jwt.exception.SaJwtException;
import com.maxkb4j.common.api.R;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleSaJwtException_expiredTokenReturnsExpiredMessage() {
        SaJwtException exception = (SaJwtException) new SaJwtException("token expired")
                .setCode(SaJwtErrorCode.CODE_30204);
        MockHttpServletResponse response = new MockHttpServletResponse();

        R<String> result = handler.handleSaJwtException(exception, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("auth.token.expired");
    }

    @Test
    void handleSaJwtException_invalidTokenReturnsInvalidMessage() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        R<String> result = handler.handleSaJwtException(new SaJwtException("invalid token"), response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("auth.token.invalid");
    }

    @Test
    void handleNotLogin_expiredTokenReturnsExpiredMessage() {
        NotLoginException exception = new NotLoginException(
                NotLoginException.TOKEN_TIMEOUT_MESSAGE, "admin", NotLoginException.TOKEN_TIMEOUT);
        MockHttpServletResponse response = new MockHttpServletResponse();

        R<String> result = handler.handleNotLogin(exception, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("auth.token.expired");
    }

    @Test
    void handleNotLogin_invalidTokenReturnsInvalidMessage() {
        NotLoginException exception = new NotLoginException(
                NotLoginException.INVALID_TOKEN_MESSAGE, "admin", NotLoginException.INVALID_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        R<String> result = handler.handleNotLogin(exception, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("auth.token.invalid");
    }

    @Test
    void handleNotLogin_missingTokenReturnsNotLoginMessage() {
        NotLoginException exception = new NotLoginException(
                NotLoginException.NOT_TOKEN_MESSAGE, "admin", NotLoginException.NOT_TOKEN);
        MockHttpServletResponse response = new MockHttpServletResponse();

        R<String> result = handler.handleNotLogin(exception, response);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(result.getCode()).isEqualTo(401);
        assertThat(result.getMessage()).isEqualTo("login.not.login");
    }
}
