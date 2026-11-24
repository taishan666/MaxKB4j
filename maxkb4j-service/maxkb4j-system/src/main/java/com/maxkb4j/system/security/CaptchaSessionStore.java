package com.maxkb4j.system.security;

import jakarta.servlet.http.HttpSession;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 验证码会话存储：同一会话内允许多个待验证的验证码共存。
 * <p>登录页可能同时打开多个（多标签页共用同一 JSESSIONID），后取的验证码
 * 不应覆盖先取的，否则先打开的页面登录时必然校验失败。这里以有界队列保存
 * 最近 {@link #MAX_PENDING} 个验证码，登录校验时任一匹配即可，且任一次
 * 登录尝试都会清空全部待验证验证码，维持「一次性使用」语义。</p>
 *
 * @author tarzan
 */
public final class CaptchaSessionStore {

    /**
     * session 属性 key，沿用历史值。
     */
    private static final String SESSION_KEY = "captcha";

    /**
     * 每会话保留的待验证验证码上限，超出时淘汰最旧的，防止恶意刷取占用内存。
     */
    private static final int MAX_PENDING = 5;

    private CaptchaSessionStore() {
    }

    /**
     * 记录一个新生成的验证码。
     *
     * @param session 当前会话
     * @param code    验证码文本（小写）
     */
    public static void store(HttpSession session, String code) {
        // 同一会话并发请求时保护队列；以 session 为锁与容器会话管理的惯例一致
        synchronized (session) {
            Deque<String> pending = pendingCodes(session);
            if (pending == null) {
                pending = new ArrayDeque<>();
                session.setAttribute(SESSION_KEY, pending);
            }
            pending.addLast(code);
            while (pending.size() > MAX_PENDING) {
                pending.removeFirst();
            }
        }
    }

    /**
     * 取出并清空全部待验证验证码。任何登录尝试（无论成败）都消费掉它们，
     * 与旧实现「一次登录即作废」的行为一致。
     *
     * @param session 当前会话
     * @return 待验证验证码列表，可能为空
     */
    public static List<String> drain(HttpSession session) {
        synchronized (session) {
            Object attribute = session.getAttribute(SESSION_KEY);
            session.removeAttribute(SESSION_KEY);
            if (attribute instanceof Deque<?> deque) {
                return deque.stream().map(String::valueOf).toList();
            }
            // 滚动发布期间旧代码写入的是单个字符串，兼容按单元素处理
            if (attribute instanceof String code) {
                return List.of(code);
            }
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private static Deque<String> pendingCodes(HttpSession session) {
        Object attribute = session.getAttribute(SESSION_KEY);
        if (attribute instanceof Deque<?> deque) {
            return (Deque<String>) deque;
        }
        // 旧版本遗留的单个字符串已无对应页面在用，直接丢弃
        return null;
    }
}
