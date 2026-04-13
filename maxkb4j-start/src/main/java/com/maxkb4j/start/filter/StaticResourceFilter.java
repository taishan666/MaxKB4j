package com.maxkb4j.start.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class StaticResourceFilter implements Filter {

    private static final String ADMIN_PATH = "/admin/";
    private static final Set<String> ALLOWED_FOLDERS = Set.of("assets", "tool", "app");

    /**
     * 匹配 /admin/.../{folder}/.../file.ext 路径，其中 ext 是指定的静态资源扩展名
     */
    public static boolean matches(String path, String folder) {
        // 构建正则：^/admin/.+/{folder}/.+\.(png|jpg|...|woff2?|...)$
        String extensions = "png|jpg|jpeg|gif|svg|ico|css|js|md|txt|woff2?|ttf|eot";
        String regex = "^/admin/.+/" + Pattern.quote(folder) + "/.+\\.(?i)(" + extensions + ")$";
        return Pattern.compile(regex).matcher(path).matches();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        for (String folder : ALLOWED_FOLDERS) {
            if (matches(path, folder)) {
                int folderIndex = path.indexOf("/" + folder + "/");
                if (folderIndex != -1) {
                    // 提取 folder 之后的完整子路径（包含子目录和文件名）
                    String subPath = path.substring(folderIndex + folder.length() + 1);
                    String targetPath = ADMIN_PATH + folder + subPath;
                    RequestDispatcher dispatcher = request.getRequestDispatcher(targetPath);
                    dispatcher.forward(request, response);
                    return;
                }
            }
        }
        chain.doFilter(request, response);
    }
}