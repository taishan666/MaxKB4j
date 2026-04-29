package com.maxkb4j.application.sandbox;

import org.kohsuke.groovy.sandbox.GroovyInterceptor;

import java.util.Set;

/**
 * 基于白名单的 Groovy 沙箱拦截器。
 * 配合 SandboxTransformer 使用，在运行期拦截所有方法调用、静态调用、构造函数调用和属性访问。
 * <p>
 * 安全模型：默认拒绝（deny-by-default），只允许白名单中的类和方法。
 * </p>
 */
public class GroovySandboxInterceptor extends GroovyInterceptor {

    /**
     * 允许作为方法接收者的安全类（白名单）。
     * 不在白名单中的类，任何方法调用都会被拒绝。
     */
    private static final Set<String> ALLOWED_CLASSES = Set.of(
            // ===== 基础类型 =====
            "java.lang.Object",
            "java.lang.String",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Short",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Float",
            "java.lang.Double",
            "java.lang.Number",
            "java.math.BigDecimal",
            "java.math.BigInteger",
            "java.lang.Character",
            // ===== 时间 =====
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.time.ZonedDateTime",
            "java.time.Instant",
            "java.time.Duration",
            "java.time.Period",
            "java.time.ZoneId",
            "java.time.temporal.TemporalAccessor",
            "java.util.Date",
            "java.util.Calendar",
            "java.util.GregorianCalendar",
            // ===== 集合 =====
            "java.util.List",
            "java.util.ArrayList",
            "java.util.LinkedList",
            "java.util.Set",
            "java.util.HashSet",
            "java.util.LinkedHashSet",
            "java.util.TreeSet",
            "java.util.Collection",
            "java.util.Map",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.TreeMap",
            "java.util.Iterator",
            "java.util.ListIterator",
            "java.util.Spliterator",
            "java.util.stream.Stream",
            "java.util.stream.StreamSupport",
            "java.util.stream.Collectors",
            "java.util.Optional",
            "java.util.Arrays",
            "java.util.Collections",
            // ===== 常用工具 =====
            "java.lang.StringBuilder",
            "java.lang.StringBuffer",
            "java.util.regex.Pattern",
            "java.util.regex.Matcher",
            "java.text.SimpleDateFormat",
            "java.text.DecimalFormat",
            "java.util.UUID",
            "java.util.Locale",
            "java.util.TimeZone",
            "java.util.Currency",
            // ===== Groovy 运行时 =====
            "groovy.lang.Binding",
            "groovy.lang.Closure",
            "groovy.lang.GString",
            "groovy.lang.IntRange",
            "groovy.lang.Range",
            "org.codehaus.groovy.runtime.DefaultGroovyMethods",
            "org.codehaus.groovy.runtime.StringGroovyMethods",
            "org.codehaus.groovy.runtime.EncodingGroovyMethods",
            "org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation"
    );

    /**
     * 允许调用的方法名。
     * 即使接收者在 ALLOWED_CLASSES 中，也只有白名单中的方法名可以被调用。
     */
    private static final Set<String> ALLOWED_METHODS = Set.of(
            // ===== 比较与相等 =====
            "equals", "compareTo", "compareToIgnoreCase",
            "contains", "containsAll", "containsKey", "containsValue",
            "startsWith", "endsWith",
            // ===== 访问 =====
            "get", "getAt", "getKey", "getValue",
            "put", "putAt", "putIfAbsent",
            "first", "firstKey", "firstEntry",
            "last", "lastKey", "lastEntry",
            "head", "tail",
            "getOrDefault",
            // ===== 集合操作 =====
            "size", "isEmpty", "isBlank", "isNotBlank",
            "iterator", "listIterator", "spliterator", "stream", "parallelStream",
            "keySet", "values", "entrySet",
            "subList", "subMap", "subSet",
            // ===== 字符串 =====
            "toString", "length", "charAt", "substring", "trim", "strip",
            "indexOf", "lastIndexOf",
            "toUpperCase", "toLowerCase",
            "replace", "replaceAll", "replaceFirst",
            "split",
            "format",
            "concat",
            "matches",
            "repeat",
            "chars", "codePoints",
            "lines",
            // ===== 类型转换 =====
            "intValue", "longValue", "doubleValue", "floatValue",
            "byteValue", "shortValue", "charValue",
            "booleanValue",
            "toInteger", "toLong", "toDouble", "toFloat",
            "toBoolean", "toBigDecimal", "toBigInteger",
            "toSet", "toList", "toArray", "toMap",
            "asType",
            "toCharArray", "getBytes",
             "inspect",
            // ===== 数字运算 =====
            "abs", "ceil", "floor", "round", "truncate",
            "max", "min",
            "plus", "minus", "div", "mod",
            "add", "subtract", "multiply", "divide", "remainder",
            "pow", "sqrt", "cbrt",
            "negate", "signum",
            "increment", "decrement",
            "next", "previous",
            // ===== 哈希 =====
            "hashCode",
            // ===== 迭代 =====
            "each", "eachWithIndex",
            "collect", "collectEntries", "collectNested",
            "findAll", "find", "findIndexOf", "findLastIndexOf",
            "any", "every",
            "inject", "fold",
            "groupBy",
            "intersect", "disjoint",
            "join", "flatten",
            "reverse", "reverseEach",
            "sort", "unique",
            "count", "sum", "average",
            "take", "takeWhile",
            "drop", "dropWhile",
            // ===== 对象 =====
            "getClass",
            // ===== 闭包 =====
            "call", "doCall", "isCase"
    );

    /**
     * 允许调用静态方法的类。
     */
    private static final Set<String> ALLOWED_STATIC_CLASSES = Set.of(
            "java.lang.Math",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Boolean",
            "java.util.Arrays",
            "java.util.Collections",
            "java.util.Objects",
            "java.util.UUID",
            "java.util.stream.Stream",
            "java.util.stream.StreamSupport",
            "java.util.stream.Collectors",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.LocalTime",
            "java.time.ZonedDateTime",
            "java.time.Instant",
            "java.time.Duration",
            "java.time.Period",
            "java.math.BigDecimal",
            "java.math.BigInteger",
            "groovy.json.JsonOutput",
            "groovy.json.JsonSlurper",
            "org.codehaus.groovy.runtime.DefaultGroovyMethods",
            "org.codehaus.groovy.runtime.StringGroovyMethods"
    );

    @Override
    public Object onMethodCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
        if (receiver == null) {
            return invoker.call(receiver, method, args);
        }
        String className = receiver.getClass().getName();
        // 处理 Groovy 生成的代理类
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
        }
        // 处理 Closure 调用 target
        if (className.startsWith("com.maxkb4j") || className.startsWith("groovy.lang")) {
            return invoker.call(receiver, method, args);
        }
        if (!ALLOWED_CLASSES.contains(className)) {
            throw new SecurityException("不允许在类 " + className + " 上调用方法: " + method);
        }
        if (!ALLOWED_METHODS.contains(method)) {
            throw new SecurityException("不允许在类 " + className + " 上调用方法: " + method);
        }
        return invoker.call(receiver, method, args);
    }

    @Override
    public Object onStaticCall(Invoker invoker, Class sender, String method, Object... args) throws Throwable {
        String className = sender.getName();
        if (!ALLOWED_STATIC_CLASSES.contains(className)) {
            throw new SecurityException("不允许调用静态方法: " + className + "." + method);
        }
        return invoker.call(sender, method, args);
    }

    @Override
    public Object onNewInstance(Invoker invoker, Class sender, Object... args) throws Throwable {
        String className = sender.getName();
        if (!ALLOWED_CLASSES.contains(className)) {
            throw new SecurityException("不允许实例化类: " + className);
        }
        // 构造函数：Invoker 需要方法名参数，"<init>" 是标准构造调用标记
        return invoker.call(sender, "<init>", args);
    }

    /**
     * 禁止访问/设置的属性名。
     * 这些属性可用于操控 Groovy 运行时行为，即使在 ALLOWED_CLASSES 中的类上也不允许操作。
     */
    private static final Set<String> BLOCKED_PROPERTIES = Set.of(
            "metaClass", "class", "declaringClass",
            "this", "super"
    );

    @Override
    public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
        if (receiver == null) {
            return invoker.call(receiver, property);
        }
        String className = receiver.getClass().getName();
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
        }
        if (!ALLOWED_CLASSES.contains(className)) {
            throw new SecurityException("不允许在类 " + className + " 上访问属性: " + property);
        }
        if (BLOCKED_PROPERTIES.contains(property)) {
            throw new SecurityException("不允许访问属性: " + property);
        }
        return invoker.call(receiver, property);
    }

    @Override
    public Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
        if (receiver == null) {
            return invoker.call(receiver, property, value);
        }
        String className = receiver.getClass().getName();
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
        }
        if (!ALLOWED_CLASSES.contains(className)) {
            throw new SecurityException("不允许在类 " + className + " 上设置属性: " + property);
        }
        if (BLOCKED_PROPERTIES.contains(property)) {
            throw new SecurityException("不允许设置属性: " + property);
        }
        return invoker.call(receiver, property, value);
    }

    @Override
    public Object onGetArray(Invoker invoker, Object receiver, Object index) throws Throwable {
        if (receiver == null) {
            return invoker.call(receiver, null, index);
        }
        String className = receiver.getClass().getName();
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
        }
        if (!ALLOWED_CLASSES.contains(className)) {
            throw new SecurityException("不允许在类 " + className + " 上访问数组: " + index);
        }
        // 数组访问本质是 getAt 方法调用
        return invoker.call(receiver, "getAt", index);
    }

    @Override
    public Object onSetArray(Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
        if (receiver == null) {
            return invoker.call(receiver, null, index, value);
        }
        String className = receiver.getClass().getName();
        if (className.contains("$$")) {
            className = className.substring(0, className.indexOf("$$"));
        }
        if (!ALLOWED_CLASSES.contains(className)) {
            throw new SecurityException("不允许在类 " + className + " 上设置数组: " + index);
        }
        // 数组设置本质是 putAt 方法调用
        return invoker.call(receiver, "putAt", index, value);
    }
}
