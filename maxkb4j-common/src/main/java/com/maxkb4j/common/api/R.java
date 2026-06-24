package com.maxkb4j.common.api;


import com.maxkb4j.common.util.I18nUtil;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
/**
 * @author tarzan
 * @date 2024-12-25 10:20:33
 */
@Schema(description = "返回信息")
public class R<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Schema(description = "状态码")
    private int code;
    @Schema(description = "承载数据")
    private T data;
    @Schema(description = "返回消息")
    private String message;

    private R(IResultCode resultCode) {
        this(resultCode, (T) null, resultCode.getMessage());
    }

    private R(IResultCode resultCode, String message) {
        this(resultCode, (T) null, message);
    }

    private R(IResultCode resultCode, T data) {
        this(resultCode, data, resultCode.getMessage());
    }

    private R(IResultCode resultCode, T data, String message) {
        this(resultCode.getCode(), data, message);
    }

    private R(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }


    public static <T> R<T> data(T data) {
        return data(data, I18nUtil.get("common.success"));
    }

    public static <T> R<T> data(T data, String message) {
        return data(200, data, message);
    }

    public static <T> R<T> data(int code, T data, String message) {
        return new R(code, data, data == null ? I18nUtil.get("common.no.data") : message);
    }


    public static <T> R<T> success(String data) {
        return new R(ResultCode.SUCCESS, data, I18nUtil.get("common.success"));
    }

    public static <T> R<T> success() {
        return new R(ResultCode.SUCCESS, I18nUtil.get("common.success"));
    }

    public static <T> R<T> success(IResultCode resultCode) {
        return new R(resultCode);
    }

    public static <T> R<T> success(IResultCode resultCode, String message) {
        return new R(resultCode, message);
    }

    public static <T> R<T> fail(String message) {
        return new R(ResultCode.FAILURE, message);
    }

    public static <T> R<T> fail(int code, String message) {
        return new R(code, (Object)null, message);
    }

    public static <T> R<T> fail(IResultCode resultCode) {
        return new R(resultCode);
    }

    public static <T> R<T> fail(IResultCode resultCode, String message) {
        return new R(resultCode, message);
    }

    public static <T> R<T> status(boolean flag) {
        return flag ?  success(I18nUtil.get("common.success")) : fail(I18nUtil.get("common.fail"));
    }

    public static <T> R<T> pkIsNull() {
        return new R(ResultCode.FAILURE, I18nUtil.get("common.pk.empty"));
    }

    public static <T> R<T> notExists() {
        return new R(ResultCode.FAILURE, I18nUtil.get("common.record.not.exists"));
    }

    public int getCode() {
        return this.code;
    }

    public T getData() {
        return this.data;
    }

    public String getMessage() {
        return this.message;
    }

    public void setCode(final int code) {
        this.code = code;
    }


    public void setData(final T data) {
        this.data = data;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String toString() {
        return "R(code=" + this.getCode() + ", data=" + this.getData() + ", message=" + this.getMessage() + ")";
    }

    public R() {
    }
}
