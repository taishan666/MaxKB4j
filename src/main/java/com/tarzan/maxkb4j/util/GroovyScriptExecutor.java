package com.tarzan.maxkb4j.util;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.internal.Json;
import dev.langchain4j.internal.Utils;
import dev.langchain4j.service.tool.ToolExecutor;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroovyScriptExecutor implements ToolExecutor {

    private final String code;

    public GroovyScriptExecutor(String code) {
        this.code = code;
    }

    @Override
    public String execute(ToolExecutionRequest toolExecutionRequest,  Object memoryId) {
        System.out.println("执行工具：" + toolExecutionRequest.name());
        System.out.println("arguments：" + toolExecutionRequest.arguments());
        Object result="";
        if(StringUtil.isNotBlank(code)){
            Map<String, Object> argumentsMap = argumentsAsMap(toolExecutionRequest.arguments());
            Binding binding = new Binding();
            StringBuilder sb=new StringBuilder(code);
            sb.append("\n").append("main(");
            if (!argumentsMap.isEmpty()){
                argumentsMap.forEach((k, v) -> {
                    binding.setVariable(k, v);
                    sb.append(k).append(",");
                });
                sb.deleteCharAt(sb.length()-1);
            }
            sb.append(")");
            System.out.println("执行脚本：" + sb);
            // 创建 GroovyShell 并运行脚本
            GroovyShell shell = new GroovyShell(binding);
            result = shell.evaluate(sb.toString());
        }
        return result.toString();
    }

    private static final Type MAP_TYPE = new ParameterizedType() {
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class, Object.class};
        }

        public Type getRawType() {
            return Map.class;
        }

        public Type getOwnerType() {
            return null;
        }
    };

    static Map<String, Object> argumentsAsMap(String arguments) {
        if (Utils.isNullOrBlank(arguments)) {
            return Map.of();
        } else {
            try {
                return (Map) Json.fromJson(arguments, MAP_TYPE);
            } catch (Exception var3) {
                String normalizedArguments = removeTrailingComma(normalizeJsonString(arguments));
                return (Map)Json.fromJson(normalizedArguments, MAP_TYPE);
            }
        }
    }

    private static final Pattern TRAILING_COMMA_PATTERN = Pattern.compile(",(\\s*[}\\]])");

    static String removeTrailingComma(String json) {
        if (json != null && !json.isEmpty()) {
            Matcher matcher = TRAILING_COMMA_PATTERN.matcher(json);
            return matcher.replaceAll("$1");
        } else {
            return json;
        }
    }

    private static final Pattern LEADING_TRAILING_QUOTE_PATTERN = Pattern.compile("^\"|\"$");
    private static final Pattern ESCAPED_QUOTE_PATTERN = Pattern.compile("\\\\\"");

    static String normalizeJsonString(String arguments) {
        if (arguments != null && !arguments.isEmpty()) {
            Matcher leadingTrailingMatcher = LEADING_TRAILING_QUOTE_PATTERN.matcher(arguments);
            String normalizedJson = leadingTrailingMatcher.replaceAll("");
            Matcher escapedQuoteMatcher = ESCAPED_QUOTE_PATTERN.matcher(normalizedJson);
            return escapedQuoteMatcher.replaceAll("\"");
        } else {
            return arguments;
        }
    }
}
