package com.maxkb4j.tool.sandbox;

import groovy.lang.Closure;
import org.kohsuke.groovy.sandbox.GroovyInterceptor;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
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
            "groovy.json.JsonSlurper",
            "groovy.json.JsonOutput",
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
            // ===== JSON =====
            "parseText", "toJson", "prettyPrint",
            // ===== 闭包 =====
            "call", "doCall", "isCase"
    );

    private static final Set<String> DANGEROUS_METHODS = Set.of(
            "exec", "execute", "start", "getRuntime",
            "forName", "loadClass", "newInstance",
            "invoke", "invokeMethod", "getMethod", "getDeclaredMethod", "getMethods", "getDeclaredMethods",
            "getField", "getDeclaredField", "getFields", "getDeclaredFields",
            "getConstructor", "getDeclaredConstructor", "getConstructors", "getDeclaredConstructors",
            "setAccessible", "getClass", "getClassLoader", "getMetaClass", "setMetaClass",
            "parseClass", "evaluate"
    );

    /**
     * 禁止访问/设置的属性名。
     * 这些属性可用于操控 Groovy 运行时行为，即使在 ALLOWED_CLASSES 中的类上也不允许操作。
     */
    private static final Set<String> BLOCKED_PROPERTIES = Set.of(
            "metaClass", "class", "classLoader", "declaringClass", "protectionDomain",
            "methods", "declaredMethods", "fields", "declaredFields",
            "constructors", "declaredConstructors", "this", "super"
    );

    private static final Set<String> ALLOWED_CONSTRUCTOR_CLASSES = Set.of(
            "java.util.ArrayList",
            "java.util.LinkedList",
            "java.util.HashSet",
            "java.util.LinkedHashSet",
            "java.util.TreeSet",
            "java.util.HashMap",
            "java.util.LinkedHashMap",
            "java.util.TreeMap",
            "java.math.BigDecimal",
            "java.math.BigInteger",
            "java.lang.StringBuilder",
            "java.lang.StringBuffer",
            "java.text.SimpleDateFormat",
            "java.text.DecimalFormat",
            "groovy.json.JsonSlurper"
    );

    private static final Map<String, Set<String>> ALLOWED_STATIC_METHODS = Map.ofEntries(
            Map.entry("java.lang.Math", Set.of(
                    "abs", "acos", "asin", "atan", "atan2", "ceil", "cos", "cosh", "exp", "floor",
                    "log", "log10", "max", "min", "pow", "random", "round", "signum", "sin", "sinh",
                    "sqrt", "tan", "tanh", "toDegrees", "toRadians")),
            Map.entry("java.lang.Integer", Set.of("parseInt", "valueOf", "toString", "compare", "sum", "max", "min")),
            Map.entry("java.lang.Long", Set.of("parseLong", "valueOf", "toString", "compare", "sum", "max", "min")),
            Map.entry("java.lang.Double", Set.of("parseDouble", "valueOf", "toString", "compare", "sum", "max", "min", "isNaN", "isInfinite")),
            Map.entry("java.lang.Float", Set.of("parseFloat", "valueOf", "toString", "compare", "sum", "max", "min", "isNaN", "isInfinite")),
            Map.entry("java.lang.Boolean", Set.of("parseBoolean", "valueOf", "toString", "logicalAnd", "logicalOr", "logicalXor")),
            Map.entry("java.math.BigDecimal", Set.of("valueOf")),
            Map.entry("java.math.BigInteger", Set.of("valueOf")),
            Map.entry("java.util.Arrays", Set.of("asList", "copyOf", "copyOfRange", "equals", "deepEquals", "sort", "toString", "deepToString")),
            Map.entry("java.util.Collections", Set.of(
                    "emptyList", "emptyMap", "emptySet", "singletonList", "singletonMap", "singleton",
                    "unmodifiableList", "unmodifiableMap", "unmodifiableSet", "sort", "reverse", "min", "max", "frequency")),
            Map.entry("java.util.Objects", Set.of("equals", "deepEquals", "hash", "hashCode", "isNull", "nonNull", "toString", "compare")),
            Map.entry("java.util.UUID", Set.of("randomUUID", "fromString", "nameUUIDFromBytes")),
            Map.entry("java.util.stream.Stream", Set.of("of", "empty", "concat")),
            Map.entry("java.util.stream.StreamSupport", Set.of("stream")),
            Map.entry("java.util.stream.Collectors", Set.of("toList", "toSet", "toMap", "joining", "counting", "groupingBy")),
            Map.entry("java.time.LocalDate", Set.of("now", "of", "parse")),
            Map.entry("java.time.LocalDateTime", Set.of("now", "of", "parse")),
            Map.entry("java.time.LocalTime", Set.of("now", "of", "parse")),
            Map.entry("java.time.ZonedDateTime", Set.of("now", "of", "parse")),
            Map.entry("java.time.Instant", Set.of("now", "ofEpochMilli", "ofEpochSecond", "parse")),
            Map.entry("java.time.Duration", Set.of("ofDays", "ofHours", "ofMinutes", "ofSeconds", "ofMillis", "between", "parse")),
            Map.entry("java.time.Period", Set.of("of", "ofDays", "ofMonths", "ofYears", "between", "parse")),
            Map.entry("groovy.json.JsonOutput", Set.of("toJson", "prettyPrint")),
            Map.entry("org.codehaus.groovy.runtime.DefaultGroovyMethods", ALLOWED_METHODS),
            Map.entry("org.codehaus.groovy.runtime.StringGroovyMethods", ALLOWED_METHODS)
    );

    @Override
    public Object onMethodCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
        if (isDangerousMethod(method)) {
            throw new SecurityException("不允许调用危险方法: " + method);
        }
        validateArguments(args);
        if (receiver == null) {
            return validateReturnValue(invoker.call(receiver, method, args));
        }

        Class<?> receiverClass = receiver.getClass();
        String className = normalizeClassName(receiverClass);
        if (!isAllowedReceiver(receiverClass, method)) {
            throw new SecurityException("不允许在类 " + className + " 上调用方法: " + method);
        }
        if (!ALLOWED_METHODS.contains(method)) {
            throw new SecurityException("不允许在类 " + className + " 上调用方法: " + method);
        }
        return validateReturnValue(invoker.call(receiver, method, args));
    }

    @Override
    public Object onStaticCall(Invoker invoker, Class sender, String method, Object... args) throws Throwable {
        if (isDangerousMethod(method) || isDangerousClass(sender)) {
            throw new SecurityException("不允许调用静态方法: " + sender.getName() + "." + method);
        }
        validateArguments(args);
        String className = normalizeClassName(sender);
        Set<String> methods = ALLOWED_STATIC_METHODS.get(className);
        if (methods == null || !methods.contains(method)) {
            throw new SecurityException("不允许调用静态方法: " + className + "." + method);
        }
        return validateReturnValue(invoker.call(sender, method, args));
    }

    @Override
    public Object onNewInstance(Invoker invoker, Class sender, Object... args) throws Throwable {
        if (isDangerousClass(sender)) {
            throw new SecurityException("不允许实例化类: " + sender.getName());
        }
        validateArguments(args);
        String className = normalizeClassName(sender);
        if (!ALLOWED_CONSTRUCTOR_CLASSES.contains(className)) {
            throw new SecurityException("不允许实例化类: " + className);
        }
        // 构造函数：Invoker 需要方法名参数，"<init>" 是标准构造调用标记
        return validateReturnValue(invoker.call(sender, "<init>", args));
    }

    @Override
    public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
        if (isDangerousProperty(property)) {
            throw new SecurityException("不允许访问属性: " + property);
        }
        if (receiver == null) {
            return validateReturnValue(invoker.call(receiver, property));
        }

        Class<?> receiverClass = receiver.getClass();
        String className = normalizeClassName(receiverClass);
        if (!isAllowedType(receiverClass)) {
            throw new SecurityException("不允许在类 " + className + " 上访问属性: " + property);
        }
        return validateReturnValue(invoker.call(receiver, property));
    }

    @Override
    public Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
        if (isDangerousProperty(property)) {
            throw new SecurityException("不允许设置属性: " + property);
        }
        validateValue(value);
        if (receiver == null) {
            return validateReturnValue(invoker.call(receiver, property, value));
        }

        Class<?> receiverClass = receiver.getClass();
        String className = normalizeClassName(receiverClass);
        if (!isAllowedType(receiverClass)) {
            throw new SecurityException("不允许在类 " + className + " 上设置属性: " + property);
        }
        return validateReturnValue(invoker.call(receiver, property, value));
    }

    @Override
    public Object onGetArray(Invoker invoker, Object receiver, Object index) throws Throwable {
        validateArrayAccess(receiver, index);
        return validateReturnValue(invoker.call(receiver, "getAt", index));
    }

    @Override
    public Object onSetArray(Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
        validateArrayAccess(receiver, index);
        validateValue(value);
        return validateReturnValue(invoker.call(receiver, "putAt", index, value));
    }

    private static boolean isAllowedReceiver(Class<?> receiverClass, String method) {
        if (Closure.class.isAssignableFrom(receiverClass)) {
            return Set.of("call", "doCall", "isCase").contains(method);
        }
        return isAllowedType(receiverClass);
    }

    private static boolean isAllowedType(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (type.isArray()) {
            return isSafeArrayType(type);
        }
        if (isDangerousClass(type)) {
            return false;
        }
        if (ALLOWED_CLASSES.contains(normalizeClassName(type))) {
            return true;
        }
        for (Class<?> iface : type.getInterfaces()) {
            if (isAllowedType(iface)) {
                return true;
            }
        }
        Class<?> superclass = type.getSuperclass();
        return superclass != null && isAllowedType(superclass);
    }

    private static boolean isDangerousMethod(String method) {
        return DANGEROUS_METHODS.contains(method);
    }

    private static boolean isDangerousProperty(String property) {
        return BLOCKED_PROPERTIES.contains(property);
    }

    private static boolean isDangerousClass(Class<?> type) {
        if (type == null) {
            return false;
        }
        if (type.isArray()) {
            return isDangerousClass(type.getComponentType());
        }
        if (Class.class.equals(type)
                || ClassLoader.class.isAssignableFrom(type)
                || Runtime.class.isAssignableFrom(type)
                || Process.class.isAssignableFrom(type)
                || ProcessBuilder.class.isAssignableFrom(type)
                || AccessibleObject.class.isAssignableFrom(type)
                || Method.class.isAssignableFrom(type)
                || Field.class.isAssignableFrom(type)
                || Constructor.class.isAssignableFrom(type)) {
            return true;
        }
        String className = normalizeClassName(type);
        return className.startsWith("java.lang.reflect.")
                || className.startsWith("java.lang.invoke.")
                || className.startsWith("java.io.")
                || className.startsWith("java.nio.file.")
                || className.startsWith("java.net.")
                || className.equals("java.lang.System")
                || className.equals("groovy.lang.GroovyShell")
                || className.equals("groovy.lang.GroovyClassLoader")
                || className.equals("groovy.lang.MetaClass")
                || className.equals("groovy.lang.MetaMethod")
                || className.equals("groovy.lang.ExpandoMetaClass")
                || className.equals("org.codehaus.groovy.runtime.InvokerHelper");
    }

    private static boolean isSafeArrayType(Class<?> type) {
        Class<?> componentType = type.getComponentType();
        while (componentType != null && componentType.isArray()) {
            componentType = componentType.getComponentType();
        }
        if (componentType == null || Object.class.equals(componentType) || isDangerousClass(componentType)) {
            return false;
        }
        return componentType.isPrimitive()
                || String.class.equals(componentType)
                || Number.class.isAssignableFrom(componentType)
                || Boolean.class.equals(componentType)
                || Character.class.equals(componentType)
                || componentType.isEnum();
    }

    private static void validateArrayAccess(Object receiver, Object index) {
        if (receiver == null) {
            throw new SecurityException("不允许访问空对象数组");
        }
        validateValue(index);
        Class<?> receiverClass = receiver.getClass();
        String className = normalizeClassName(receiverClass);
        if (receiverClass.isArray()) {
            if (!isSafeArrayType(receiverClass)) {
                throw new SecurityException("不允许访问数组类型: " + className);
            }
            return;
        }
        if (!isAllowedType(receiverClass)) {
            throw new SecurityException("不允许在类 " + className + " 上访问数组");
        }
    }

    private static void validateArguments(Object... args) {
        if (args == null) {
            return;
        }
        for (Object arg : args) {
            validateValue(arg);
        }
    }

    private static Object validateReturnValue(Object value) {
        validateValue(value);
        return value;
    }

    private static void validateValue(Object value) {
        if (value == null) {
            return;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray()) {
            if (!isSafeArrayType(valueClass)) {
                throw new SecurityException("不允许使用数组类型: " + normalizeClassName(valueClass));
            }
            return;
        }
        if (isDangerousClass(valueClass)) {
            throw new SecurityException("不允许使用危险类型: " + normalizeClassName(valueClass));
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                validateValue(item);
            }
        } else if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                validateValue(entry.getKey());
                validateValue(entry.getValue());
            }
        }
    }

    private static String normalizeClassName(Class<?> type) {
        String className = type.getName();
        if (className.contains("$$")) {
            return className.substring(0, className.indexOf("$$"));
        }
        return className;
    }
}
