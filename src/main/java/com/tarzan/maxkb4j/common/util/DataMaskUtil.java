package com.tarzan.maxkb4j.common.util;

public class DataMaskUtil {

    /**
     * 手机号脱敏：138****1234
     */
    public static String maskMobile(String mobile) {
        if (mobile == null || mobile.length() != 11) {
            return mobile;
        }
        return mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
    }

    /**
     * 身份证号脱敏：前3位 + **** + 后4位
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        int length = idCard.length();
        return idCard.substring(0, 3) + "****" + idCard.substring(length - 4);
    }

    /**
     * 邮箱脱敏：user****@domain.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];
        if (username.length() <= 2) {
            return "*@" + domain;
        }
        String maskedUsername = username.substring(0, 2) + "****" + username.substring(username.length() - 1);
        return maskedUsername + "@" + domain;
    }

    /**
     * 银行卡号脱敏：保留前6后4，中间用 **** 替代
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 10) {
            return bankCard;
        }
        int length = bankCard.length();
        return bankCard.substring(0, 6) + "****" + bankCard.substring(length - 4);
    }

    public static String maskApiKey(String apiKey) {
        return maskString(apiKey, 4, 4);
    }

    /**
     * 通用字符串脱敏：保留前 n 位和后 m 位，中间用 * 替代
     */
    public static String maskString(String str, int prefix, int suffix) {
        if (str == null) return null;
        if (str.length() <= prefix + suffix) {
            return str.replaceAll("\\.", "*");
        }
        String start = str.substring(0, prefix);
        String end = str.substring(str.length() - suffix);
        return start + "*".repeat(Math.max(0, str.length() - prefix - suffix)) + end;
    }
}