
package com.tarzan.maxkb4j.handler;

import cn.dev33.satoken.util.SaResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


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

    @ExceptionHandler(Exception.class)
    public SaResult handleException(Exception e) {
        log.error("异常: {}", e.getMessage(), e);
        return SaResult.error(e.getMessage());
    }


}

