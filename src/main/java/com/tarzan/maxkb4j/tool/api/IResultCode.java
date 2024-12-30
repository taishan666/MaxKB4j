package com.tarzan.maxkb4j.tool.api;


import java.io.Serializable;
/**
 * @author tarzan
 * @date 2024-12-25 10:20:33
 */
public interface IResultCode extends Serializable {
    String getMessage();

    int getCode();
}
