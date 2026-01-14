package com.tarzan.maxkb4j.module.system.store;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.common.api.R;
import com.tarzan.maxkb4j.common.constant.AppConst;
import com.tarzan.maxkb4j.common.util.IoUtil;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author tarzan
 * @date 2024-12-31 17:33:32
 */
@RestController
@RequestMapping(AppConst.ADMIN_API)
public class StoreController {

    @GetMapping("/workspace/store/application_template")
    public R<JSONObject> applicationTemplate(String name) {
        ClassLoader classLoader = StoreController.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream("templates/maxkb.json");
        String text = IoUtil.readToString(inputStream);
        JSONObject result = JSON.parseObject(text);
        JSONArray apps = result.getJSONArray("apps");
        JSONObject additionalProperties = result.getJSONObject("additionalProperties");
        JSONArray tags = additionalProperties.getJSONArray("tags");
        Map<String, Object> tagsMap = tags.stream().collect(Collectors.toMap(tag -> ((JSONObject)tag).getString("name"), tag -> ((JSONObject)tag).getString("key")));
        JSONArray finalApps = new JSONArray();
        for (int i = 0; i < apps.size(); i++) {
            JSONObject app = apps.getJSONObject(i);
            String downloadUrl = app.getString("downloadUrl");
            JSONArray appTags = app.getJSONArray("tags");
            app.put("label",tagsMap.get(appTags.getString(0)));
            if (downloadUrl != null&& downloadUrl.endsWith(".mk")){
                finalApps.add(app);
            }
        }
        result.put("apps", finalApps);
        return R.success(result);
    }

}
