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

/*    @GetMapping("/workspace/store/application_template")
    public R<List<MaxKb4J>> applicationTemplate(String name) throws IOException {
        List<MaxKb4J> list = new ArrayList<>();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:templates/app/*.mk4j");
        for (Resource resource : resources) {
            if (resource.isFile() && Objects.requireNonNull(resource.getFilename()).endsWith(".mk4j")) {
                String text = IoUtil.readToString(resource.getInputStream());
                MaxKb4J app = JSONObject.parseObject(text, MaxKb4J.class);
                if (app!=null){
                    list.add(app);
                }
            }
        }
        if(StringUtils.isNotBlank(name)) {
            list = list.stream().filter(app -> app.getApplication().getName().contains(name)).collect(Collectors.toList());
        }
        return R.data(list);
    }*/

    @GetMapping("/workspace/store/application_template")
    public R<JSONObject> applicationTemplate1(String name) {
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
