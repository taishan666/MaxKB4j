package com.maxkb4j.tool.sandbox;

import groovy.lang.Closure;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Groovy 沙箱安全策略：编译期与运行期共用的唯一事实来源。
 * <p>
 * 集中维护类/方法/属性/构造器/静态调用白名单、危险调用黑名单，
 * 以及类型与取值的安全判定逻辑。运行期 {@link GroovySandboxInterceptor}
 * 与编译期 AST 校验（GroovySandboxCompilerConfigurer）均只依赖本类，
 * 策略数据不再在多个类之间重复维护。
 * </p>
 * <p>安全模型：默认拒绝（deny-by-default），只允许白名单中的类和方法。</p>
 */
public final class GroovySandboxPolicy {

    private GroovySandboxPolicy() {
    }

    // ==================================================================
    // 白名单数据
    // ==================================================================

    /**
     * 允许作为方法接收者的安全类（白名单）。
     * 不在白名单中的类，任何方法调用都会被拒绝。
     */
    private static final Set<String> ALLOWED_CLASSES = Set.of(
            // ===== 基础类型 =====
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
            "java.time.format.DateTimeFormatter",
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
            // ===== 文件操作（java.nio.file 白名单入口） =====
            "java.nio.file.Path",
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
            "org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation",
            "io.github.mymonstercat.ocr.InferenceEngine",
            "io.github.mymonstercat.Model",
            "com.benjaminwan.ocrlibrary.OcrResult",
            "com.maxkb4j.oss.service.IOssService",
            "com.maxkb4j.common.util.SpringUtil",
            // ===== 数学表达式求值引擎（内置工具「数学公式执行」） =====
            "net.objecthunter.exp4j.Expression",
            "net.objecthunter.exp4j.ExpressionBuilder",
            // ===== fastjson（内置工具 JSON 处理，如 web_search 结果解析） =====
            "com.alibaba.fastjson.JSON",
            "com.alibaba.fastjson.JSONObject",
            "com.alibaba.fastjson.JSONArray"
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
            "addAll", "remove", "removeAll", "clear",
            // ===== 时间 =====
            "atZone", "toInstant", "toEpochMilli",
            "withZone", "withLocale",
            "plusDays", "minusDays", "plusWeeks", "minusWeeks",
            "plusMonths", "minusMonths", "plusYears", "minusYears",
            "plusHours", "minusHours", "plusMinutes", "minusMinutes",
            "plusSeconds", "minusSeconds", "plusNanos", "minusNanos",
            "withYear", "withMonth", "withDayOfMonth",
            "withHour", "withMinute", "withSecond", "withNano",
            // ===== 字符串 =====
            "toString", "length", "charAt", "substring", "trim", "strip",
            "indexOf", "lastIndexOf",
            "toUpperCase", "toLowerCase",
            "replace", "replaceAll", "replaceFirst",
            "split",
            "format",
            "parse",
            "concat",
            "matches",
            "group", "groupCount",
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
            // ===== fastjson（JSONObject/JSONArray 类型化访问与序列化） =====
            "toJSONString",
            "getString", "getInteger", "getLong", "getDouble", "getFloat",
            "getBoolean", "getBigDecimal", "getBigInteger", "getDate",
            "getJSONObject", "getJSONArray", "getObject", "getInnerMap",
            "toJavaObject", "toJavaList",
            "fluentPut", "fluentAdd",
            // ===== Path 操作 =====
            "resolve", "resolveSibling", "relativize",
            "getFileName", "getParent", "getRoot", "getName", "getNameCount", "subpath",
            "normalize", "toAbsolutePath",
            // ===== 异常（只读访问器；接收者仍受异常类白名单约束） =====
            "getMessage", "getLocalizedMessage", "getCause", "printStackTrace",
            // DateTimeParseException 特有访问器（日期解析失败的 catch 场景）
            "getParsedString", "getErrorIndex",
            // ===== OCR =====
            "runOcr",
            // ===== exp4j 表达式求值（内置工具「数学公式执行」） =====
            "build", "evaluate", "setVariable", "setVariables", "variables",
            // ===== 闭包 =====
            "call", "doCall", "isCase"
    );

    /** 允许通过 new 实例化的类。 */
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
            "groovy.json.JsonSlurper",
            "java.lang.IllegalArgumentException",
            "net.objecthunter.exp4j.ExpressionBuilder",
            // fastjson：内置工具构造 JSON 对象（new JSONObject() / new JSONArray()）
            "com.alibaba.fastjson.JSONObject",
            "com.alibaba.fastjson.JSONArray"
    );

    /** 允许静态调用的类及其方法白名单。 */
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
            Map.entry("java.lang.String", Set.of("join", "valueOf", "format")),
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
            Map.entry("java.time.ZoneId", Set.of("of", "systemDefault", "ofOffset")),
            Map.entry("java.time.format.DateTimeFormatter", Set.of("ofPattern", "ofLocalizedDate", "ofLocalizedTime", "ofLocalizedDateTime")),
            Map.entry("groovy.json.JsonOutput", Set.of("toJson", "prettyPrint")),
            Map.entry("org.codehaus.groovy.runtime.DefaultGroovyMethods", ALLOWED_METHODS),
            Map.entry("org.codehaus.groovy.runtime.StringGroovyMethods", ALLOWED_METHODS),
            Map.entry("org.codehaus.groovy.runtime.ScriptBytecodeAdapter", Set.of("findRegex", "matchRegex")),
            Map.entry("java.nio.file.Path", Set.of("of")),
            // fastjson：内置工具 JSON 解析/序列化入口（JSON.parseObject / JSON.toJSONString 等）
            Map.entry("com.alibaba.fastjson.JSON", Set.of(
                    "parse", "parseObject", "parseArray",
                    "toJSONString", "toJSONBytes",
                    "isValid", "isValidArray", "isValidObject")),
            Map.entry("io.github.mymonstercat.ocr.InferenceEngine", Set.of("getInstance")),
            Map.entry("com.maxkb4j.common.util.SpringUtil", Set.of("getBean", "getBeansOfType")),
            Map.entry("java.nio.file.Files", Set.of(
                    "readString", "readAllLines", "readAllBytes",
                    "write", "writeString",
                    "exists", "notExists", "size",
                    "isRegularFile", "isDirectory", "isReadable", "isWritable",
                    "createDirectories", "createTempFile", "delete", "deleteIfExists"))
    );

    /**
     * java.nio.file 包内的白名单类：Files / Path 作为文件操作入口，
     * 不受 java.nio.file.* 危险类前缀限制；同包其它类仍被禁止。
     */
    private static final Set<String> ALLOWED_NIO_CLASSES = Set.of(
            "java.nio.file.Files",
            "java.nio.file.Path"
    );

    /** 基本类型名（编译期 ClassExpression 白名单校验用）。 */
    private static final Set<String> PRIMITIVE_TYPE_NAMES = Set.of(
            "int", "long", "double", "float", "boolean", "char", "byte", "short", "void"
    );

    /** 闭包接收者仅允许调用转发相关方法。 */
    private static final Set<String> CLOSURE_METHODS = Set.of("call", "doCall", "isCase");

    /** 允许作为接收者的异常类白名单（脚本可 catch 并读取其消息）。 */
    private static final Set<String> ALLOWED_EXCEPTION_CLASSES = Set.of(
            "java.lang.Throwable",
            "java.lang.Exception",
            "java.lang.RuntimeException",
            "java.lang.IllegalArgumentException",
            "java.lang.NumberFormatException",
            // java.time 日期解析/构造失败抛出的异常族（如 LocalDate.parse 的 catch 场景）
            "java.time.DateTimeException",
            "java.time.format.DateTimeParseException"
    );

    /**
     * 平台数据类（DTO/VO/实体/领域对象）的包名特征。
     * 工作流/工具引擎会把这类对象作为绑定参数传入脚本（如 imageList 中的 OssFile），
     * 允许对其做属性读取（由 getter 支撑）；方法调用仍受方法白名单约束。
     */
    private static final List<String> DATA_CLASS_PACKAGE_TOKENS = List.of(
            ".domain.", ".dto.", ".vo.", ".entity."
    );

    // ==================================================================
    // 黑名单数据
    // ==================================================================

    /** 危险方法名：任何情况下都不允许调用。 */
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
     * 危险方法名的受信例外：方法名命中危险名单，但接收者属于白名单安全类时放行。
     * 例如 exp4j Expression#evaluate 是纯数学表达式求值，
     * 与 GroovyShell#evaluate 这类任意脚本执行有本质区别。
     */
    private static final Map<String, Set<String>> DANGEROUS_METHOD_EXCEPTIONS = Map.of(
            "evaluate", Set.of("net.objecthunter.exp4j.Expression")
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

    /**
     * 脚本文本预检的危险标记（按匹配优先级排序，全部小写）。
     * 在编译前对脚本内容做粗粒度拦截，命中即拒绝。
     */
    private static final List<String> DANGEROUS_TOKENS = List.of(
            "runtime",
            "processbuilder",
            "java.lang.process", "java.lang.system",
            "system.getenv", "system.getproperty",
            "class.forname",
            "getruntime",
            ".exec",
            ".execute",
            ".start",
            "getclass",
            "getclassloader",
            "loadclass",
            "metaclass", "classloader", "java.lang.reflect", "java.lang.invoke", "setaccessible",
            "getmethod", "getdeclaredmethod", "invoke(", "new file", "java.io.",
            "java.net.",
            "groovyshell", "groovyclassloader"
    );

    // ==================================================================
    // 编译期 / 运行期共用的判定方法
    // ==================================================================

    /**
     * 编译期 ClassExpression 白名单校验：脚本中允许引用（类名）的类。
     * 与运行期白名单及可实例化类保持一致的单一事实来源，
     * 未在白名单中的类在编译期即被拒绝。
     */
    public static boolean isAllowedClassName(String className) {
        return "java.lang.Object".equals(className)
                || PRIMITIVE_TYPE_NAMES.contains(className)
                || ALLOWED_CLASSES.contains(className)
                || ALLOWED_STATIC_METHODS.containsKey(className)
                || ALLOWED_CONSTRUCTOR_CLASSES.contains(className)
                || ALLOWED_EXCEPTION_CLASSES.contains(className);
    }

    /** 方法名是否在实例方法白名单中。 */
    public static boolean isMethodAllowed(String method) {
        return ALLOWED_METHODS.contains(method);
    }

    /** 静态调用是否在白名单中（类 + 方法双重校验）。 */
    public static boolean isStaticCallAllowed(String className, String method) {
        Set<String> methods = ALLOWED_STATIC_METHODS.get(className);
        return methods != null && methods.contains(method);
    }

    /**
     * 运行期静态调用校验（含继承链）：静态方法可经子类名调用
     * （如 {@code JSONObject.parseArray} 实际声明于父类 {@code JSON}），
     * 精确类名未命中时沿父类链向上查找，仅当某级父类已显式登记静态方法白名单时才放行。
     */
    public static boolean isStaticCallAllowed(Class<?> sender, String method) {
        if (sender == null || method == null) {
            return false;
        }
        for (Class<?> clazz = sender; clazz != null && clazz != Object.class; clazz = clazz.getSuperclass()) {
            if (isStaticCallAllowed(normalizeClassName(clazz), method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 运行期静态调用空操作判定：命中时静默跳过，不执行真实方法。
     * <p>
     * 典型来源：脚本中的 {@code @Grab} 注解。编译期已按注册全限定名禁用
     * {@code groovy.grape.GrabAnnotationTransformation}，正常编译不会注入任何
     * Grape 调用；但若存在禁用未生效的环境或历史编译产物，{@code @Grab} 会被
     * 转换为运行期 {@code Grape.grab(...)} 静态调用。沙箱禁止联网下载依赖，
     * 此类调用按空操作忽略（绝不放行真实下载），脚本继续使用应用 classpath
     * 中已存在的依赖执行。
     * </p>
     */
    public static boolean isNoOpStaticCall(Class<?> sender, String method) {
        return sender != null
                && "groovy.grape.Grape".equals(sender.getName())
                && "grab".equals(method);
    }

    /** 类是否允许通过 new 实例化。 */
    public static boolean isConstructorAllowed(String className) {
        return ALLOWED_CONSTRUCTOR_CLASSES.contains(className);
    }

    public static boolean isDangerousMethod(String method) {
        return DANGEROUS_METHODS.contains(method);
    }

    /**
     * 带接收者类型的危险方法检查：命中危险方法名但属于受信例外组合（接收者类 + 方法名）时不视为危险。
     * 接收者类型未知（null）时退化为纯方法名校验，保持保守拦截。
     */
    public static boolean isDangerousMethod(Class<?> receiverClass, String method) {
        if (!isDangerousMethod(method)) {
            return false;
        }
        if (receiverClass == null) {
            return true;
        }
        Set<String> exceptions = DANGEROUS_METHOD_EXCEPTIONS.get(method);
        return exceptions == null || !exceptions.contains(normalizeClassName(receiverClass));
    }

    public static boolean isDangerousProperty(String property) {
        return BLOCKED_PROPERTIES.contains(property);
    }

    /**
     * 接收者是否为指向白名单类的类引用对象（Class 实例）。
     * 用于「类名.静态属性」场景（如枚举常量 Model.ONNX_PPOCR_V4、
     * 静态字段 Integer.MAX_VALUE）：Groovy 将其编译为对 Class 对象的属性读取，
     * 仅当 Class 指向的目标类在白名单中时放行，读取结果仍经过取值校验。
     */
    public static boolean isAllowedClassReference(Object receiver) {
        if (!(receiver instanceof Class<?> clazz)) {
            return false;
        }
        return isAllowedClassName(normalizeClassName(clazz)) && !isDangerousClass(clazz);
    }

    /**
     * 是否为平台数据类（包名含 dto/vo/entity/domain 段的 com.maxkb4j.* 类）。
     * 仅用于放开属性读取，不放开任意方法调用与属性写入。
     */
    public static boolean isReadableDataClass(Class<?> type) {
        if (type == null || isDangerousClass(type)) {
            return false;
        }
        String className = normalizeClassName(type);
        if (!className.startsWith("com.maxkb4j.")) {
            return false;
        }
        for (String token : DATA_CLASS_PACKAGE_TOKENS) {
            if (className.contains(token)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 脚本文本预检：返回脚本内容中命中的第一个危险标记，未命中返回 null。
     */
    public static String findDangerousToken(String script) {
        String normalized = script.toLowerCase();
        for (String token : DANGEROUS_TOKENS) {
            if (normalized.contains(token)) {
                return token;
            }
        }
        return null;
    }

    // ==================================================================
    // 运行期类型判定
    // ==================================================================

    /** 接收者类型 + 方法名的组合是否允许（闭包仅放行转发方法）。 */
    public static boolean isAllowedReceiver(Class<?> receiverClass, String method) {
        if (Closure.class.isAssignableFrom(receiverClass)) {
            return CLOSURE_METHODS.contains(method);
        }
        return isAllowedType(receiverClass);
    }

    /** 类型是否在白名单中（沿接口与父类链查找；Object 本身不放行）。 */
    public static boolean isAllowedType(Class<?> type) {
        if (type == null) {
            return false;
        }
        // Object 是所有类的祖先，若放行会导致类白名单对任意类失效
        if (Object.class.equals(type)) {
            return false;
        }
        if (type.isArray()) {
            return isSafeArrayType(type);
        }
        String className = normalizeClassName(type);
        if (ALLOWED_CLASSES.contains(className) || ALLOWED_EXCEPTION_CLASSES.contains(className)) {
            return true;
        }
        for (Class<?> iface : type.getInterfaces()) {
            if (isAllowedType(iface)) {
                return true;
            }
        }
        if (isDangerousClass(type)) {
            return false;
        }
        Class<?> superclass = type.getSuperclass();
        return isAllowedType(superclass);
    }

    /** 类型是否属于危险类（反射 / 进程 / 类加载 / IO / 网络 / Groovy 运行时入口等）。 */
    public static boolean isDangerousClass(Class<?> type) {
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
                || (className.startsWith("java.nio.file.") && !ALLOWED_NIO_CLASSES.contains(className))
                || className.startsWith("java.net.")
                || className.equals("java.lang.System")
                || className.equals("groovy.lang.GroovyShell")
                || className.equals("groovy.lang.GroovyClassLoader")
                || className.equals("groovy.lang.MetaClass")
                || className.equals("groovy.lang.MetaMethod")
                || className.equals("groovy.lang.ExpandoMetaClass")
                || className.equals("org.codehaus.groovy.runtime.InvokerHelper");
    }

    /** 数组类型是否安全：最终组件类型为基本类型、字符串、数字、布尔、字符或枚举。 */
    public static boolean isSafeArrayType(Class<?> type) {
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

    /** Groovy 生成的内部类名归一化（去掉 $$ 之后的部分）。 */
    public static String normalizeClassName(Class<?> type) {
        String className = type.getName();
        if (className.contains("$$")) {
            return className.substring(0, className.indexOf("$$"));
        }
        return className;
    }

    // ==================================================================
    // 运行期取值校验
    // ==================================================================

    /** 数组/下标访问校验：接收者与下标取值都必须安全。 */
    public static void validateArrayAccess(Object receiver, Object index) {
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

    public static void validateArguments(Object... args) {
        if (args == null) {
            return;
        }
        for (Object arg : args) {
            validateValue(arg);
        }
    }

    public static Object validateReturnValue(Object value) {
        validateValue(value);
        return value;
    }

    /** 递归校验取值类型：拒绝危险类型与不安全数组，深入集合与 Map 逐项校验。 */
    public static void validateValue(Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof Class<?> classValue) {
            // Class 字面量（如 SpringUtil.getBean(IOssService.class) 的参数）：
            // 仅当指向白名单类时放行，避免 Class 对象携带任意类型穿透沙箱
            if (!isAllowedClassReference(classValue)) {
                throw new SecurityException("不允许使用危险类型: " + classValue.getName());
            }
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

    // ==================================================================
    // 安全异常提取
    // ==================================================================

    /** 沿异常 cause 链查找 SecurityException（沙箱拒绝语义），未找到返回 null。 */
    public static SecurityException findSecurityException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof SecurityException securityException) {
                return securityException;
            }
            throwable = throwable.getCause();
        }
        return null;
    }
}
