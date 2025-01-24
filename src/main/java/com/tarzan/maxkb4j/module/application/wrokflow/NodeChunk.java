package com.tarzan.maxkb4j.module.application.wrokflow;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class NodeChunk {
    private Integer status;
    private List<String> chunkList;

    public NodeChunk() {
        this.status = 0;
        this.chunkList = new ArrayList<>();
    }

    public void addChunk(String chunk) {
        this.chunkList.add(chunk);
    }

    public void end(String chunk){
        if(Objects.nonNull(chunk)){
            this.chunkList.add(chunk);
        }
        this.status = 200;
    }

    public boolean isEnd() {
        return this.status == 200;
    }
}
