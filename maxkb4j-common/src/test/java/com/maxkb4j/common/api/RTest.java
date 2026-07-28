package com.maxkb4j.common.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归测试：统一返回结构 R 的工厂方法。
 * <p>无 Spring 上下文时 {@link com.maxkb4j.common.util.I18nUtil#get(String)} 回退为返回 key 本身，
 * 故此处 message 断言为对应的 i18n key。
 */
class RTest {

    @Test
    void success_defaultHasSuccessCodeAndKeyMessage() {
        R<String> r = R.success();
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getData()).isNull();
        assertThat(r.getMessage()).isEqualTo("common.success");
    }

    @Test
    void success_withDataCarriesPayload() {
        R<String> r = R.success("payload");
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getData()).isEqualTo("payload");
        assertThat(r.getMessage()).isEqualTo("common.success");
    }

    @Test
    void success_byResultCodeUsesItsCodeAndMessage() {
        R<Object> r = R.success(ResultCode.RECORD_NOT_EXIST);
        assertThat(r.getCode()).isEqualTo(600);
        assertThat(r.getMessage()).isEqualTo("记录不存在");
    }

    @Test
    void success_byResultCodeOverridesMessage() {
        R<Object> r = R.success(ResultCode.RECORD_NOT_EXIST, "自定义");
        assertThat(r.getCode()).isEqualTo(600);
        assertThat(r.getMessage()).isEqualTo("自定义");
    }

    @Test
    void data_nonNullKeepsGivenMessage() {
        R<String> r = R.data("x", "msg");
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getData()).isEqualTo("x");
        assertThat(r.getMessage()).isEqualTo("msg");
    }

    @Test
    void data_nullFallsBackToNoDataMessage() {
        R<Object> r = R.data(null);
        assertThat(r.getCode()).isEqualTo(200);
        assertThat(r.getData()).isNull();
        assertThat(r.getMessage()).isEqualTo("common.no.data");
    }

    @Test
    void fail_byMessageUsesFailureCode() {
        R<Object> r = R.fail("出错了");
        assertThat(r.getCode()).isEqualTo(400);
        assertThat(r.getMessage()).isEqualTo("出错了");
    }

    @Test
    void fail_byCodeAndMessage() {
        R<Object> r = R.fail(500, "内部错误");
        assertThat(r.getCode()).isEqualTo(500);
        assertThat(r.getMessage()).isEqualTo("内部错误");
    }

    @Test
    void fail_byResultCodeUsesItsCodeAndMessage() {
        R<Object> r = R.fail(ResultCode.UN_AUTHORIZED);
        assertThat(r.getCode()).isEqualTo(401);
        assertThat(r.getMessage()).isEqualTo("请求未授权");
    }

    @Test
    void status_trueIsSuccess_falseIsFailure() {
        assertThat(R.status(true).getCode()).isEqualTo(200);
        assertThat(R.status(true).getMessage()).isEqualTo("common.success");
        assertThat(R.status(false).getCode()).isEqualTo(400);
        assertThat(R.status(false).getMessage()).isEqualTo("common.fail");
    }

    @Test
    void pkIsNullAndNotExistsUseFailureCodeWithKeyMessage() {
        assertThat(R.pkIsNull().getCode()).isEqualTo(400);
        assertThat(R.pkIsNull().getMessage()).isEqualTo("common.pk.empty");
        assertThat(R.notExists().getCode()).isEqualTo(400);
        assertThat(R.notExists().getMessage()).isEqualTo("common.record.not.exists");
    }

    @Test
    void gettersAndSettersRoundTrip() {
        R<String> r = R.success();
        r.setCode(418);
        r.setData("teapot");
        r.setMessage("im a teapot");
        assertThat(r.getCode()).isEqualTo(418);
        assertThat(r.getData()).isEqualTo("teapot");
        assertThat(r.getMessage()).isEqualTo("im a teapot");
    }

    @Test
    void toStringContainsCodeAndData() {
        assertThat(R.success("hi").toString()).contains("R(code=", "data=hi");
    }
}