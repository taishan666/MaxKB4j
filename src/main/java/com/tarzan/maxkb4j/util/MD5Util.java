package com.tarzan.maxkb4j.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Util {


    public static String encrypt(String text){
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        byte[] hashInBytes = md.digest(text.getBytes());

        // 将字节数组转换成16进制表示的字符串
        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String encrypt(String text,int beginIndex, int endIndex){
        return encrypt(text).substring(beginIndex, endIndex);
    }
}
