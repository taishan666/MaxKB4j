package com.tarzan.maxkb4j.module.application.workflow;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Data
public class NodeChunk {
    private long id;
    private Integer status;
    private List<JSONObject> chunkList;

    public NodeChunk() {
        this.id = System.currentTimeMillis();
        this.status = 0;
        this.chunkList = new ArrayList<>();
    }

    public void addChunk(JSONObject chunk) {
        log.info("chunkList={}", this.chunkList);
        this.chunkList.add(chunk);
    }

    public void end(JSONObject chunk){
        if(Objects.nonNull(chunk)){
            this.chunkList.add(chunk);
        }
        this.status = 200;
        System.out.println("end  id="+this.id+" status="+this.status);
    }

    public boolean isEnd() {
        System.out.println("isEnd  id="+this.id+" status="+this.status);
        return this.status == 200;
    }
}
