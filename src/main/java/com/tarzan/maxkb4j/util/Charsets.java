package com.tarzan.maxkb4j.util;


import org.apache.poi.util.StringUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;

public class Charsets {
    public static final Charset ISO_8859_1;
    public static final String ISO_8859_1_NAME;
    public static final Charset GBK;
    public static final String GBK_NAME;
    public static final Charset UTF_8;
    public static final String UTF_8_NAME;

    public Charsets() {
    }

    public static Charset charset(String charsetName) throws UnsupportedCharsetException {
        return StringUtil.isBlank(charsetName) ? Charset.defaultCharset() : Charset.forName(charsetName);
    }

    static {
        ISO_8859_1 = StandardCharsets.ISO_8859_1;
        ISO_8859_1_NAME = ISO_8859_1.name();
        GBK = Charset.forName("GBK");
        GBK_NAME = GBK.name();
        UTF_8 = StandardCharsets.UTF_8;
        UTF_8_NAME = UTF_8.name();
    }
}

