package com.maxkb4j.tool.sandbox;

import groovy.lang.Script;
import org.kohsuke.groovy.sandbox.GroovyInterceptor;

/**
 * 基于白名单的 Groovy 沙箱拦截器。
 * 配合 SandboxTransformer 使用，在运行期拦截所有方法调用、静态调用、构造函数调用、属性访问和数组访问。
 * <p>
 * 所有安全判定（类/方法/属性白名单、危险类型识别、取值校验）统一委托
 * {@link GroovySandboxPolicy}，本类只负责把各拦截点接入策略。
 * 安全模型：默认拒绝（deny-by-default），只允许白名单中的类和方法。
 * </p>
 */
public class GroovySandboxInterceptor extends GroovyInterceptor {

    @Override
    public Object onMethodCall(Invoker invoker, Object receiver, String method, Object... args) throws Throwable {
        if (GroovySandboxPolicy.isDangerousMethod(method)) {
            throw new SecurityException("不允许调用危险方法: " + method);
        }
        GroovySandboxPolicy.validateArguments(args);
        if (receiver == null) {
            return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, method, args));
        }

        if (receiver instanceof Script) {
            // 脚本自身实例：方法体已通过编译期 SecureAST 校验并被沙箱转换，
            // 对绑定变量与脚本内自定义函数的访问直接放行，其发起的外部调用仍会被逐层拦截
            return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, method, args));
        }

        Class<?> receiverClass = receiver.getClass();
        String className = GroovySandboxPolicy.normalizeClassName(receiverClass);
        if (!GroovySandboxPolicy.isAllowedReceiver(receiverClass, method)) {
            throw new SecurityException("不允许在类 " + className + " 上调用方法: " + method);
        }
        if (!GroovySandboxPolicy.isMethodAllowed(method)) {
            throw new SecurityException("不允许在类 " + className + " 上调用方法: " + method);
        }
        return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, method, args));
    }

    @Override
    public Object onStaticCall(Invoker invoker, Class sender, String method, Object... args) throws Throwable {
        if (GroovySandboxPolicy.isDangerousMethod(method) || GroovySandboxPolicy.isDangerousClass(sender)) {
            throw new SecurityException("不允许调用静态方法: " + sender.getName() + "." + method);
        }
        GroovySandboxPolicy.validateArguments(args);
        String className = GroovySandboxPolicy.normalizeClassName(sender);
        if (!GroovySandboxPolicy.isStaticCallAllowed(className, method)) {
            throw new SecurityException("不允许调用静态方法: " + className + "." + method);
        }
        return GroovySandboxPolicy.validateReturnValue(invoker.call(sender, method, args));
    }

    @Override
    public Object onNewInstance(Invoker invoker, Class sender, Object... args) throws Throwable {
        if (GroovySandboxPolicy.isDangerousClass(sender)) {
            throw new SecurityException("不允许实例化类: " + sender.getName());
        }
        GroovySandboxPolicy.validateArguments(args);
        String className = GroovySandboxPolicy.normalizeClassName(sender);
        if (!GroovySandboxPolicy.isConstructorAllowed(className)) {
            throw new SecurityException("不允许实例化类: " + className);
        }
        // 构造函数：Invoker 需要方法名参数，"<init>" 是标准构造调用标记
        return GroovySandboxPolicy.validateReturnValue(invoker.call(sender, "<init>", args));
    }

    @Override
    public Object onGetProperty(Invoker invoker, Object receiver, String property) throws Throwable {
        if (GroovySandboxPolicy.isDangerousProperty(property)) {
            throw new SecurityException("不允许访问属性: " + property);
        }
        if (receiver == null) {
            return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, property));
        }

        if (receiver instanceof Script) {
            // 脚本读取自身绑定变量（Script.getProperty 会回退到 Binding）
            return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, property));
        }

        if (GroovySandboxPolicy.isAllowedClassReference(receiver)) {
            // 类引用对象：允许读取白名单类的静态属性（如枚举常量 Model.ONNX_PPOCR_V4），
            // 危险属性（class/classLoader/metaClass 等）已在入口处被拒绝
            return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, property));
        }

        Class<?> receiverClass = receiver.getClass();
        String className = GroovySandboxPolicy.normalizeClassName(receiverClass);
        if (!GroovySandboxPolicy.isAllowedType(receiverClass)) {
            throw new SecurityException("不允许在类 " + className + " 上访问属性: " + property);
        }
        return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, property));
    }

    @Override
    public Object onSetProperty(Invoker invoker, Object receiver, String property, Object value) throws Throwable {
        if (GroovySandboxPolicy.isDangerousProperty(property)) {
            throw new SecurityException("不允许设置属性: " + property);
        }
        GroovySandboxPolicy.validateValue(value);
        if (receiver == null) {
            return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, property, value));
        }

        if (receiver instanceof Script) {
            // 脚本写入自身绑定变量（Script.setProperty 会回退到 Binding）
            return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, property, value));
        }

        Class<?> receiverClass = receiver.getClass();
        String className = GroovySandboxPolicy.normalizeClassName(receiverClass);
        if (!GroovySandboxPolicy.isAllowedType(receiverClass)) {
            throw new SecurityException("不允许在类 " + className + " 上设置属性: " + property);
        }
        return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, property, value));
    }

    @Override
    public Object onGetArray(Invoker invoker, Object receiver, Object index) throws Throwable {
        GroovySandboxPolicy.validateArrayAccess(receiver, index);
        return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, "getAt", index));
    }

    @Override
    public Object onSetArray(Invoker invoker, Object receiver, Object index, Object value) throws Throwable {
        GroovySandboxPolicy.validateArrayAccess(receiver, index);
        GroovySandboxPolicy.validateValue(value);
        return GroovySandboxPolicy.validateReturnValue(invoker.call(receiver, "putAt", index, value));
    }
}
