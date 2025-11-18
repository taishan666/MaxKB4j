package com.tarzan.maxkb4j.common.handler;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;


@Slf4j
@Component
public class FieldMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if(isInsert("isActive", metaObject)){
            this.setFieldValByName("isActive",true , metaObject);
        }
        if(isInsert("statusMeta", metaObject)){
            this.setFieldValByName("statusMeta",defaultStatusMeta() , metaObject);
        }
        if(isInsert("meta", metaObject)){
            this.setFieldValByName("meta",new JSONObject() , metaObject);
        }
        this.setFieldValByName("createTime", new Date(), metaObject);
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.setFieldValByName("updateTime", new Date(), metaObject);
    }



    public boolean isInsert(String fieldName, MetaObject metaObject) {
        if(metaObject.hasGetter(fieldName)){
            return metaObject.getValue(fieldName)==null;
        }
        return false;
    }

    public JSONObject defaultStatusMeta() {
        JSONObject statusMeta = new JSONObject();
        statusMeta.put("state_time", new JSONObject());
        return statusMeta;
    }

}


