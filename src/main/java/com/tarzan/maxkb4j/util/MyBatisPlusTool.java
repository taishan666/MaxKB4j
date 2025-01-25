package com.tarzan.maxkb4j.util;


import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * mybatis plus通用生成工具
 * 可生成业务接口、mapper接口、实体类
 *
 * @author tarzan Liu
 * @date 2021/4/10 19:44
 */

public class MyBatisPlusTool {
    /**
     * 驱动
     */
    private static final String driver = "org.postgresql.Driver";
    private static final String user = "username";
    private static final String pwd = "password";
    private static final String host = "127.0.0.1:5432";
    private static final String dbName = "maxkb";
    private static final String url = "jdbc:postgresql://" + host + "/" + dbName + "?currentSchema=public";
    /**
     * 数据库表名
     */
    private static String tableName = "function_lib";
    /**
     * 数据库别名,可以与数据库表名相同,用于生成实体类
     */
    private static String aliasName = "functionLib";
    /**
     * mapper.xml命名空间路径
     */
    private static final String packagePath = "com/tarzan/maxkb4j/"+aliasName;
    /**
     * mapper.xml命名空间路径
     */
    private static final String packageName = "com.tarzan.maxkb4j."+aliasName;
    /**
     * 作者
     */
    private static final String author = "tarzan";
    /**
     * 默认生成主文件夹路径
     */
    private static final String rootPathName = "src/main/java/";
    private static Connection getConnection = null;
    static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * 链接数据库
     */
    private static Connection getConnections() {
        try {
            Class.forName(driver);
            getConnection = DriverManager.getConnection(url, user, pwd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return getConnection;
    }

    /**
     * 格式化默认值
     */
    private static String defaultValue(String value) {
        if (StringUtils.isNotBlank(value)) {
            return "；默认值：" + value;
        }
        {
            return "";
        }
    }

    private static String getAliasName(String tableName, String prefix) {
        return tableName.substring(prefix.length());
    }

    /**
     * 格式化数据类型
     * 返回的是基本类型的包装类
     * 如果使用基本数据类型long
     */
    private static String formatType(String typeValue) {
        if ("bit".equalsIgnoreCase(typeValue) || "bool".equalsIgnoreCase(typeValue)) {
            return "Boolean";
        } else if ("int2".equalsIgnoreCase(typeValue) || "int4".equalsIgnoreCase(typeValue)) {
            return "Integer";
        } else if ("int8".equalsIgnoreCase(typeValue)) {
            return "Long";
        } else if ("float4".equalsIgnoreCase(typeValue)) {
            return "Float";
        } else if ("float8".equalsIgnoreCase(typeValue)) {
            return "Double";
        } else if ("decimal".equalsIgnoreCase(typeValue)) {
            return "BigDecimal";
        } else if ("varchar".equalsIgnoreCase(typeValue) || "char".equalsIgnoreCase(typeValue) || "text".equalsIgnoreCase(typeValue)||"uuid".equalsIgnoreCase(typeValue)) {
            return "String";
        } else if ("datetime".equalsIgnoreCase(typeValue) || "timestamptz".equalsIgnoreCase(typeValue)) {
            return "Date";
        } else if ("image".equalsIgnoreCase(typeValue)) {
            return "Blob";
        } else if ("jsonb".equalsIgnoreCase(typeValue)) {
            return "JSONObject";
        } else {
            return "Long";
        }

    }


    /**
     * 驼峰转换
     */
    private static String columnToProperty(String column) {
        StringBuilder result = new StringBuilder();
        // 快速检查
        if (column == null || column.isEmpty()) {
            // 没必要转换
            return "";
        } else {
            column = column.toLowerCase();
        }
        if (!column.contains("_")) {
            // 不含下划线，仅将首字母小写
            return column.substring(0, 1).toLowerCase() + column.substring(1);
        } else {
            // 用下划线将原始字符串分割
            String[] columns = column.split("_");
            for (String columnSplit : columns) {
                // 跳过原始字符串中开头、结尾的下换线或双重下划线
                if (columnSplit.isEmpty()) {
                    continue;
                }
                // 处理真正的驼峰片段
                if (result.length() == 0) {
                    // 第一个驼峰片段，全部字母都小写
                    result.append(columnSplit.toLowerCase());
                } else {
                    // 其他的驼峰片段，首字母大写
                    result.append(columnSplit.substring(0, 1).toUpperCase()).append(columnSplit.substring(1).toLowerCase());
                }
            }
            return result.toString();
        }
    }

    /**
     * 实体名称转换
     */
    private static String formatBeanName(String column) {
        StringBuilder result = new StringBuilder();
        // 快速检查
        if (column == null || column.isEmpty()) {
            // 没必要转换
            return "";
        } else if (!column.contains("_")) {
            // 不含下划线，仅将首字母大写
            return column.substring(0, 1).toUpperCase() + column.substring(1);
        } else {
            // 用下划线将原始字符串分割
            String[] columns = column.split("_");
            for (String columnSplit : columns) {
                // 跳过原始字符串中开头、结尾的下换线或双重下划线
                if (columnSplit.isEmpty()) {
                    continue;
                }
                // 处理真正的驼峰片段
                result.append(columnSplit.substring(0, 1).toUpperCase()).append(columnSplit.substring(1).toLowerCase());
            }
            return result.toString();
        }
    }


    /**
     * 实体类字段
     */
    private static void getBean(String tableName, String aliasName) {
        getConnection = getConnections();
        StringBuilder sb = new StringBuilder();
        try {
            DatabaseMetaData dbmd = getConnection.getMetaData();
            ResultSet rs = dbmd.getColumns(null, "%", tableName, "%");
            String beanName = formatBeanName(aliasName);
            sb.append("package ").append(packageName).append(".entity;\n\n");
            sb.append("import com.baomidou.mybatisplus.annotation.TableName;\n");
            sb.append("import lombok.Data;\n");
            int length = sb.length();
            boolean dateFlag = false;
            boolean jsonFlag = false;
            sb.append(" /**\n" + "  * @author ").append(author).append("\n").append("  * @date ").append(format.format(new Date())).append("\n").append("  */\n").append("@Data\n").append("@TableName(\"").append(tableName).append("\")\n").append("public class ").append(beanName).append("Entity {\n");
            while (rs.next()) {
                if ("Date".equals(formatType(rs.getString("TYPE_NAME")))) {
                    dateFlag = true;
                }
                if ("JSONObject".equals(formatType(rs.getString("TYPE_NAME")))) {
                    jsonFlag = true;
                }
                sb.append("\t//").append(rs.getString("REMARKS")).append(defaultValue(rs.getString("COLUMN_DEF"))).append("\n");
                sb.append("\tprivate ").append(formatType(rs.getString("TYPE_NAME"))).append(" ").append(columnToProperty(rs.getString("COLUMN_NAME"))).append(";\n");
            }
            sb.append("} ");
            if (dateFlag) {
                sb.insert(length, "import java.util.Date;\n");
            }
            if (jsonFlag) {
                sb.insert(length, "import com.alibaba.fastjson.JSONObject;\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        write(sb.toString(), "Entity.java", "entity");
        System.err.println("\n类型：JAVA数据层实体类（bean.java）" + "\n状态：成功" + "\n时间：" + format.format(new Date()) + "\n");
    }


    /**
     * 生成DAO层接口
     */
    private static void getMapper(String aliasName) {
        StringBuilder sb = new StringBuilder();
        try {
            String beanName = formatBeanName(aliasName);
            sb.append("package ").append(packageName).append(".mapper;\n\n");
            sb.append("import com.baomidou.mybatisplus.core.mapper.BaseMapper;\n");
            sb.append("import org.apache.ibatis.annotations.Mapper;\n");
            sb.append("import ").append(packageName).append(".entity.").append(beanName).append("Entity;\n");
            sb.append("/**\n" + " * @author ").append(author).append("\n").append(" * @date ").append(format.format(new Date())).append("\n").append(" */\n").append("public interface ").append(beanName).append("Mapper extends BaseMapper<").append(beanName).append("Entity>{\n").append(" \n").append("}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        write(sb.toString(), "Mapper.java", "mapper");
        System.err.println("\n类型：JAVA数据持久层接口（dao.java）" + "\n状态：成功" + "\n时间：" + format.format(new Date()) + "\n");
    }


    /**
     * 生成SERVICE层接口
     */
    private static void getService(String aliasName) {
        StringBuilder sb = new StringBuilder();
        try {
            String beanName = formatBeanName(aliasName);
            sb.append("package ").append(packageName).append(".service;\n\n");
            sb.append("import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;\n");
            sb.append("import org.springframework.stereotype.Service;\n");
            sb.append("import ").append(packageName).append(".mapper.").append(beanName).append("Mapper;\n");
            sb.append("import ").append(packageName).append(".entity.").append(beanName).append("Entity;\n");
            sb.append("/**\n" + " * @author ").append(author).append("\n").append(" * @date ").append(format.format(new Date())).append("\n").append(" */\n").append("@Service\n").append("public class ").append(beanName).append("Service extends ServiceImpl<").append(beanName).append("Mapper, ").append(beanName).append("Entity>{\n").append("\n").append("}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        write(sb.toString(), "Service.java", "service");
        System.err.println("\n类型：JAVA业务层接口（service.java）" + "\n状态：成功" + "\n时间：" + format.format(new Date()) + "\n");

    }

    /**
     * 生成Controller层接口
     */
    private static void getController(String aliasName) {
        StringBuilder sb = new StringBuilder();
        try {
            String beanName = formatBeanName(aliasName);
            sb.append("package ").append(packageName).append(".controller;\n\n");
            sb.append("import org.springframework.web.bind.annotation.RestController;\n");
            sb.append("import lombok.AllArgsConstructor;\n");
            sb.append("import ").append(packageName).append(".service.").append(beanName).append("Service;\n");
            sb.append("/**\n" + " * @author ").append(author).append("\n").append(" * @date ").append(format.format(new Date())).append("\n").append(" */\n").append("@RestController\n").append("@AllArgsConstructor\n").append("public class ").append(beanName).append("Controller{\n")
                    .append("\n")
                    .append("\tprivate\tfinal ").append(beanName).append("Service ").append(aliasName).append("Service;\n")
                    .append("}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        write(sb.toString(), "Controller.java", "controller");
        System.err.println("\n类型：JAVA业务层接口（service.java）" + "\n状态：成功" + "\n时间：" + format.format(new Date()) + "\n");

    }

    /**
     * 写文件，支持中文字符，在linux redhad下测试过
     *
     * @param str  文本内容
     * @param name 文本名称
     */
    private static void write(String str, String name, String type) {
        try {
            File dir = new File(rootPathName + packagePath + "/" + type);
            dir.mkdirs();
            String path = dir.getPath() + "/" + formatBeanName(aliasName) + name;
            File file = new File(path);
            if (!file.exists()) {
                file.createNewFile();
            }
            //如果追加方式用true
            FileOutputStream out = new FileOutputStream(file, false);
            //注意需要转换对应的字符集
            out.write((str + "\n").getBytes(StandardCharsets.UTF_8));
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 一次生产所有表:
     */
    private static void tableNames() {
        getConnection = getConnections();
        try {
            DatabaseMetaData metaData = getConnection.getMetaData();
            ResultSet rs = metaData.getTables(getConnection.getCatalog(), getConnection.getSchema(), null, new String[]{"TABLE"});
            while (rs.next()) {
                tableName = rs.getString("TABLE_NAME");
                aliasName = getAliasName(tableName, "of_");
                //实体
                getBean(tableName, aliasName);
                //dao层接口
                getMapper(aliasName);
                //业务类接口
                getService(aliasName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        //  tableNames();
        //实体
        getBean(tableName, aliasName);
        //mapper接口
        getMapper(aliasName);
        //业务类接口
        getService(aliasName);
        //控制器
        getController(aliasName);
    }

}