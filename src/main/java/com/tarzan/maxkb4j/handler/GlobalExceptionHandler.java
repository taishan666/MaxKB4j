
package com.tarzan.maxkb4j.handler;

import cn.dev33.satoken.exception.NotLoginException;
import com.tarzan.maxkb4j.tool.api.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;


/**
 * 异常统一处理
 *
 * @author tarzan liu
 * @date 2021年5月11日
 * @since JDK1.8
 */

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler{

    // 捕获未登录异常
    @ExceptionHandler(NotLoginException.class)
    @ResponseBody
    public R<String> handleNotLogin(NotLoginException e) {
        log.error("未登录异常: {}", e.getMessage(), e);
        return R.fail(401, e.getMessage());
    }


    @ExceptionHandler(Exception.class)
    @ResponseBody
    public R<String>  handleException(Exception e) {
        log.error("异常: {}", e.getMessage(), e);
        return R.fail(500, e.getMessage());
    }

    @ExceptionHandler(NullPointerException.class)
    @ResponseBody
    public R<String>  handleException(NullPointerException e) {
        log.error("空指针异常: {}", e.getMessage(), e);
        return R.fail(500, e.getMessage());
    }


}

