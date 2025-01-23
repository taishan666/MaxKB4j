package com.tarzan.maxkb4j.module.system.setting.cache;

import com.alibaba.fastjson.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class SystemCache {

    private static final Map<Integer, JSONObject> CACHE=new HashMap<>();

    public static void put(Integer type, JSONObject value) {
        CACHE.put(type,value);
    }

    public static Object get(Integer type) {
        return CACHE.get(type);
    }

    public static String getPrivateKey() {
        return CACHE.get(1).getString("value");
    }

    public static String getPublicKey() {
        return CACHE.get(1).getString("key");
    }
}
