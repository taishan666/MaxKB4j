package com.maxkb4j.knowledge.consts;

public interface TaskStatus {

    // 等待
   int PENDING=0;
    // 执行中
    int STARTED=1;
    // 成功
    int SUCCESS=2;
    // 失败
    int FAILURE=3;
    // 取消任务
    int REVOKE=4;
    // 取消成功
    int REVOKED=5;
  //  int IGNORED=n;
}
