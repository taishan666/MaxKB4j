package com.maxkb4j.knowledge.consts;

public interface TaskStatus {

    // 等待
   char PENDING=0;
    // 执行中
    char STARTED=1;
    // 成功
    char SUCCESS=2;
    // 失败
    char FAILURE=3;
    // 取消任务
    char REVOKE=4;
}
