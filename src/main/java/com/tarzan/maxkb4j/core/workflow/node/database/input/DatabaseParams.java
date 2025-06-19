package com.tarzan.maxkb4j.core.workflow.node.database.input;


import lombok.Data;

import java.util.List;

@Data
public class DatabaseParams {
    private String databaseTypeType;
    private String databaseType;
    private List<String> databaseTypeReference;
    private String hostType;
    private String host;
    private List<String> hostReference;
    private String portType;
    private Integer port;
    private List<String> portReference;
    private String databaseNameType;
    private String databaseName;
    private List<String> databaseNameReference;
    private String usernameType;
    private String username;
    private List<String> usernameReference;
    private String passwordType;
    private String password;
    private List<String> passwordReference;
    private String sqlType;
    private String sql;
    private List<String> sqlReference;
}
