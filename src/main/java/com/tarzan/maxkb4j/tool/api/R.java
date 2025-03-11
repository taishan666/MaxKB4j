package com.tarzan.maxkb4j.tool.api;


import com.tarzan.maxkb4j.util.ObjectUtil;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.lang.Nullable;

import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
/**
 * @author tarzan
 * @date 2024-12-25 10:20:33
 */
@ApiModel(description = "返回信息")
public class R<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @ApiModelProperty(value = "状态码", required = true)
    private int code;
    @ApiModelProperty("承载数据")
    private T data;
    @ApiModelProperty(value = "返回消息", required = true)
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

    public static boolean isSuccess(@Nullable R<?> result) {
        return (Boolean)Optional.ofNullable(result).map((x) -> {
            return ObjectUtil.nullSafeEquals(ResultCode.SUCCESS.code, x.code);
        }).orElse(Boolean.FALSE);
    }

    public static boolean isNotSuccess(@Nullable R<?> result) {
        return !isSuccess(result);
    }

    public static <T> R<T> data(T data) {
        return data(data, "操作成功");
    }

    public static <T> R<T> data(T data, String message) {
        return data(200, data, message);
    }

    public static <T> R<T> data(int code, T data, String message) {
        return new R(code, data, data == null ? "暂无承载数据" : message);
    }

 /*   public static <T> R<T> success(String message) {
        return new R(ResultCode.SUCCESS, message);
    }*/

    public static <T> R<T> success(T data) {
        return new R(ResultCode.SUCCESS,data, "操作成功");
    }

    public static <T> R<T> success() {
        return new R(ResultCode.SUCCESS, "操作成功");
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
        return flag ? (R<T>) success("操作成功") : fail("操作失败");
    }

    public static <T> R<T> pkIsNull() {
        return new R(ResultCode.FAILURE, "主键不能为空");
    }

    public static <T> R<T> notExists() {
        return new R(ResultCode.FAILURE, "记录不存在");
    }

    public int getCode() {
        return this.code;
    }

    public T getData() {
        return this.data;
    }

    public String getmessage() {
        return this.message;
    }

    public void setCode(final int code) {
        this.code = code;
    }


    public void setData(final T data) {
        this.data = data;
    }

    public void setmessage(final String message) {
        this.message = message;
    }

    public String toString() {
        return "R(code=" + this.getCode() + ", data=" + this.getData() + ", message=" + this.getmessage() + ")";
    }

    public R() {
    }
}
