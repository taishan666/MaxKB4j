package com.tarzan.maxkb4j.module.model.provider;

import java.util.List;
import java.util.Map;

public abstract class BaseReranker {

    public abstract List<Map<String,Object>> textReRank(String query, List<String> documents);
}
