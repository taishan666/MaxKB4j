package com.maxkb4j.common.cache;

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
        JSONObject json = CACHE.get(1);
        if (json == null){
            return null;
        }
        return json.getString("value");
    }

    public static String getPublicKey() {
        JSONObject json = CACHE.get(1);
        if (json == null){
            return null;
        }
        return json.getString("key");
    }
}
