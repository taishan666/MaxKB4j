package com.maxkb4j.system.service;

import com.alibaba.fastjson.JSONObject;
import com.maxkb4j.common.enums.SettingType;

public interface ISystemSettingService {
    boolean saveOrUpdate(JSONObject meta, SettingType type);

    JSONObject getSettingMeta(SettingType settingType);
}
