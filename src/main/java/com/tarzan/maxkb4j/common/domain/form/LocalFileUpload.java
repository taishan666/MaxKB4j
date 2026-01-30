package com.tarzan.maxkb4j.common.domain.form;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class LocalFileUpload extends BaseField {

    public LocalFileUpload(int fileCountLimit, int fileSizeLimit,List<String> fileTypeList) {
        super.setInput_type("LocalFileUpload");
        super.setField("fileList");
        JSONObject attrs =new JSONObject();
        attrs.put("file_count_limit",fileCountLimit);
        attrs.put("file_size_limit",fileSizeLimit);
        attrs.put("fileTypeList", fileTypeList);
        super.setAttrs(attrs);
        super.setLabel("");
    }
}
