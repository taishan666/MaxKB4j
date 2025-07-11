package com.tarzan.maxkb4j.core.workflow.node.database.impl;

import com.alibaba.fastjson.JSONObject;
import com.tarzan.maxkb4j.core.workflow.INode;
import com.tarzan.maxkb4j.core.workflow.NodeResult;
import com.tarzan.maxkb4j.core.workflow.node.database.input.DatabaseParams;
import com.tarzan.maxkb4j.util.DatabaseUtil;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.tarzan.maxkb4j.core.workflow.enums.NodeType.DATABASE;

@Slf4j
public class DatabaseNode extends INode {
    @Override
    public NodeResult execute() throws Exception {
        System.out.println(DATABASE);
        DatabaseParams nodeParams= super.nodeParams.toJavaObject(DatabaseParams.class);
        String databaseType = (String) this.workflowManage.getFieldValue(nodeParams.getDatabaseTypeType(), nodeParams.getDatabaseType(), nodeParams.getDatabaseTypeReference());
        String host = (String) this.workflowManage.getFieldValue(nodeParams.getHostType(), nodeParams.getHost(), nodeParams.getHostReference());
        int port = (int) this.workflowManage.getFieldValue(nodeParams.getPortType(), nodeParams.getPort(), nodeParams.getPortReference());
        String databaseName = (String) this.workflowManage.getFieldValue(nodeParams.getDatabaseNameType(), nodeParams.getDatabaseName(), nodeParams.getDatabaseNameReference());
        String username = (String) this.workflowManage.getFieldValue(nodeParams.getUsernameType(), nodeParams.getUsername(), nodeParams.getUsernameReference());
        String password = (String) this.workflowManage.getFieldValue(nodeParams.getPasswordType(), nodeParams.getPassword(), nodeParams.getPasswordReference());
        String sql = (String) this.workflowManage.getFieldValue(nodeParams.getSqlType(), nodeParams.getSql(), nodeParams.getSqlReference());
        DataSource dataSource= DatabaseUtil.getDataSource(databaseType, host, port, databaseName, username, password);
        // 用于保存结果（如果是查询）
        List<Map<String, Object>> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            // 如果是查询语句
            if (sql.trim().toLowerCase().startsWith("select")) {
                try (ResultSet rs = ps.executeQuery()) {
                    int columnCount = rs.getMetaData().getColumnCount();
                    while (rs.next()) {
                        Map<String, Object> row = new HashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            String columnName = rs.getMetaData().getColumnName(i);
                            Object value = rs.getObject(i);
                            row.put(columnName, value);
                        }
                        result.add(row);
                    }
                    // 返回结果（如果是查询）
                    return new NodeResult(Map.of("answer",result),Map.of());
                }
            } else {
                // 如果是非查询语句（如 insert/update/delete）
                int rowsAffected = ps.executeUpdate();
                Map<String, Object> updateResult = new HashMap<>();
                updateResult.put("rowsAffected", rowsAffected);
                return new NodeResult(Map.of("answer",updateResult),Map.of());
            }

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new RuntimeException("数据库执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public JSONObject getDetail() {
        JSONObject detail = new JSONObject();
        detail.put("result",context.get("answer"));
        return detail;
    }
}
