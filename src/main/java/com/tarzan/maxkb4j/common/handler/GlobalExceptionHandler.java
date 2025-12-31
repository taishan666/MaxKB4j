
package com.tarzan.maxkb4j.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.jwt.exception.SaJwtException;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.exception.*;
import com.tarzan.maxkb4j.common.util.StpKit;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import javax.crypto.BadPaddingException;


/**
 * 异常统一处理
 *
 * @author tarzan liu
 * @date 2025年3月11日
 * @since JDK17
 */
@Hidden
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // 捕获未登录异常
    @ExceptionHandler(NotLoginException.class)
    @ResponseBody
    public R<String> handleNotLogin(NotLoginException e, HttpServletResponse response) {
        log.error("未登录异常: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置HTTP状态码为401
        return R.fail(401, e.getMessage());
    }

    @ExceptionHandler(NotPermissionException.class)
    @ResponseBody
    public R<String> handleException(NotPermissionException e, HttpServletResponse response) {
        log.error("无此权限异常: {}", e.getMessage(), e);
        return R.fail(500, e.getMessage());
    }

    @ExceptionHandler(SaJwtException.class)
    @ResponseBody
    public R<String> handleSaJwtException(SaJwtException e, HttpServletResponse response) {
        log.error("SaJwt 异常: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置HTTP状态码为401
        return R.fail(401, e.getMessage());
    }


    @ExceptionHandler(BadPaddingException.class)
    @ResponseBody
    public R<String> handleException(BadPaddingException e) {
        log.error("RSA解密异常: {}", e.getMessage(), e);
        return R.fail(500, e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public R<String> handleException(NullPointerException e) {
        log.error("空指针异常: {}", e.getMessage(), e);
        return R.fail(500, e.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleException(NoResourceFoundException e) {
        log.warn(e.getMessage());
        // 判断是否已登录
        if (StpKit.ADMIN.isLogin()) {
            return "redirect:/admin/application";
        } else {
            return "redirect:/admin/login";
        }
    }

    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public R<String> handleException(ApiException e) {
        log.error("Api异常: {}", e.getMessage());
        return R.fail(500, e.getMessage());
    }

    @ExceptionHandler(LoginException.class)
    @ResponseBody
    public R<String> handleException(LoginException e) {
        log.error("登录异常: {}", e.getMessage());
        return R.fail(500, e.getMessage());
    }

    @ExceptionHandler(AccessException.class)
    @ResponseBody
    public R<String> handleException(AccessException e) {
        log.error("禁止访问异常: {}", e.getMessage(), e);
        return R.fail(403, e.getMessage());
    }

    @ExceptionHandler(AccessNumLimitException.class)
    @ResponseBody
    public R<String> handleException(AccessNumLimitException e, HttpServletResponse response) {
        response.setStatus(461); // 设置HTTP状态码为461
        return R.fail(1002, e.getMessage());
    }

    @ExceptionHandler(UserIdentityException.class)
    @ResponseBody
    public R<String> handleException(UserIdentityException e, HttpServletResponse response) {
        response.setStatus(460); // 设置HTTP状态码为461
        return R.fail(1002, e.getMessage());
    }


}

