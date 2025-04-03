
package com.tarzan.maxkb4j.core.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.tarzan.maxkb4j.core.exception.ApiException;
import com.tarzan.maxkb4j.core.api.R;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.crypto.BadPaddingException;


/**
 * 异常统一处理
 *
 * @author tarzan liu
 * @date 2025年3月11日
 * @since JDK17
 */

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler{

    // 捕获未登录异常
    @ExceptionHandler(NotLoginException.class)
    @ResponseBody
    public R<String> handleNotLogin(NotLoginException e, HttpServletResponse response) {
        log.error("未登录异常: {}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 设置HTTP状态码为401
        return R.fail(401, e.getMessage());
    }


    /*@ExceptionHandler(Exception.class)
    @ResponseBody
    public R<String>  handleException(Exception e) {
        log.error("异常: {}", e.getMessage(), e);
        return R.fail(500, e.getMessage());
    }*/

    @ExceptionHandler(BadPaddingException.class)
    @ResponseBody
    public R<String>  handleException(BadPaddingException e) {
        log.error("RSA解密异常: {}", e.getMessage(), e);
        return R.fail(500, e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public R<String>  handleException(NullPointerException e) {
        log.error("空指针异常: {}", e.getMessage(), e);
        return R.fail(500, e.getMessage());
    }

 /*   @ExceptionHandler(NoResourceFoundException.class)
    @ResponseBody
    public R<String>  handleException(NoResourceFoundException e) {
        log.error("未发现资源异常: {}", e.getMessage(), e);
        return R.fail(404, e.getMessage());
    }*/

    @ExceptionHandler(ApiException.class)
    @ResponseBody
    public R<String>  handleException(ApiException e) {
        log.error("Api异常: {}", e.getMessage(), e);
        return R.fail(400, e.getMessage());
    }


}

