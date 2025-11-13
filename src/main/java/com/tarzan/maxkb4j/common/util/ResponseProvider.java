package com.tarzan.maxkb4j.common.util;

import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.api.ResultCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;

@Slf4j
public class ResponseProvider {

    public static void write(HttpServletResponse response) {
        R result = R.fail(ResultCode.UN_AUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-type", "application/json;charset=UTF-8");
        response.setStatus(200);
        try {
            response.getWriter().write(Objects.requireNonNull(JsonUtil.toJson(result)));
        } catch (IOException e) {
            log.error(e.getMessage());
        }

    }
}
