
package com.maxkb4j.common.handler;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotPermissionException;
import cn.dev33.satoken.jwt.exception.SaJwtException;
import com.maxkb4j.common.api.R;
import com.maxkb4j.common.exception.*;
import com.maxkb4j.common.util.I18nUtil;
import com.maxkb4j.common.util.StpKit;
import dev.langchain4j.exception.RateLimitException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import javax.crypto.BadPaddingException;
import java.io.IOException;


/**
 * 异常统一处理
 *
 * @author tarzan liu
 * @date 2025年3月11日
 * @since JDK17
 */
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
    public R<String> handleException(NotPermissionException e) {
        log.error("无此权限异常: {}", e.getMessage(), e);
        return R.fail(403, e.getMessage());
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

    /**
     * 处理异步请求不可用异常（客户端断开连接）
     * SSE 流式响应时客户端关闭连接会触发此异常，属于正常情况，静默处理
     */
    @ExceptionHandler(AsyncRequestNotUsableException.class)
    public void handleAsyncRequestNotUsable(AsyncRequestNotUsableException e) {
        // 仅记录 debug 级别日志，不抛出异常，避免干扰正常业务流程
        log.debug("Client disconnected during async request: {}", e.getMessage());
    }

    /**
     * 处理客户端断开连接导致的 IOException
     * 浏览器快速刷新/取消请求时，服务端仍在向 socket 写响应，会抛出：
     *   - "你的主机中的软件中止了一个已建立的连接" (Windows)
     *   - "Connection reset by peer" / "Broken pipe" (Linux)
     * 这些是正常的客户端行为，不应作为 ERROR 打印完整堆栈。
     */
    @ExceptionHandler(IOException.class)
    public void handleClientAbort(IOException e, HttpServletResponse response) throws IOException {
        if (isClientAbortException(e)) {
            log.debug("Client aborted connection: {}", e.getMessage());
            return;
        }
        // 非客户端断开的 IO 异常，仍按未知异常处理
        log.error("IO 异常", e);
        if (!response.isCommitted()) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private boolean isClientAbortException(IOException e) {
        String msg = e.getMessage();
        if (msg == null) {
            return false;
        }
        String lower = msg.toLowerCase();
        return lower.contains("broken pipe")
                || lower.contains("connection reset")
                || lower.contains("connection was aborted")
                || lower.contains("an established connection was aborted")
                || msg.contains("你的主机中的软件中止了一个已建立的连接")
                || msg.contains("远程主机强迫关闭了一个现有的连接");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public String handleException(NoResourceFoundException e, HttpServletResponse response) {
        // 如果响应已提交（如 SSE 流已开始发送），则无法 redirect，直接返回 null
        if (response.isCommitted()) {
            log.debug("Response already committed, cannot redirect: {}", e.getMessage());
            return null;
        }
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

    @ExceptionHandler(RateLimitException.class)
    @ResponseBody
    public R<String> handleException(RateLimitException e) {
        log.error("RateLimitException: {}", e.getMessage());
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
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleException(UserIdentityException e, HttpServletResponse response) {
        response.setStatus(460); // 设置HTTP状态码为461
        return R.fail(1002, e.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    @ResponseBody
    public R<String> handleException(SecurityException e) {
        return R.fail(500,e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<String> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("非法参数: {}", e.getMessage(), e);
        return R.fail(e.getMessage());
    }

    @ExceptionHandler(FileLimitExceededException.class)
    @ResponseBody
    public R<String> handleFileLimitExceededException(FileLimitExceededException e) {
        log.error("业务规则校验失败: {}", e.getMessage(), e);
        return R.fail(400, e.getMessage());
    }


    @ExceptionHandler(Exception.class)
    public R<String> handleException(Exception e) {
        log.error("未知异常", e);
        return R.fail(500, I18nUtil.get("common.system.error"));
    }


}

