/*
package com.tarzan.maxkb4j.handler;

import com.tarzan.maxkb4j.tool.api.R;
import com.tarzan.maxkb4j.tool.api.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

*/
/**
 * 异常统一处理
 *
 * @author tarzan liu
 * @since JDK1.8
 * @date 2021年5月11日
 *//*

@Slf4j
@ControllerAdvice
public class AppExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, HttpServletRequest request) {
        log.error("异常: {}", e.getMessage(), e);
        request.setAttribute("javax.servlet.error.status_code", ResultCode.NOT_FOUND.getCode());
      //  ErrorLogPublisher.publishEvent(e, UrlUtil.getPath(Objects.requireNonNull(WebUtil.getRequest()).getRequestURI()));

        return  R.fail("无效的access_toke").toString();
    }


}
*/
