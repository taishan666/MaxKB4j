package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Data
public class NodeChunk {
    private long id;
    private Integer status;
    private List<JSONObject> chunkList;

    public NodeChunk() {
        this.id = System.currentTimeMillis();
        this.status = 0;
        this.chunkList =new CopyOnWriteArrayList<>();
    }

    public void addChunk(JSONObject chunk) {
       // System.out.println("chunk="+chunk);
        this.chunkList.add(chunk);
    }

    public void end(JSONObject chunk){
        if(Objects.nonNull(chunk)){
            this.chunkList.add(chunk);
        }
        this.status = 200;
    }

    public boolean isEnd() {
        return this.status == 200;
    }
}
