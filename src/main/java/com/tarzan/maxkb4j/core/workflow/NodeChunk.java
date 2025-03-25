package com.tarzan.maxkb4j.core.workflow;

import com.tarzan.maxkb4j.module.application.vo.ChatMessageVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Data
public class NodeChunk {
    private String id;
    private Integer status;
    private List<ChatMessageVO> chunkList;

    public NodeChunk() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.status = 0;
        this.chunkList =new CopyOnWriteArrayList<>();
    }

    public NodeChunk(String id) {
        this.id = id;
        this.status = 0;
        this.chunkList =new CopyOnWriteArrayList<>();
    }

    public void addChunk(ChatMessageVO chunk) {
        this.chunkList.add(chunk);
    }

    public void end(ChatMessageVO chunk){
        if(Objects.nonNull(chunk)){
            this.chunkList.add(chunk);
        }
        this.status = 200;
    }

    public boolean isEnd() {
        return this.status == 200;
    }
}
