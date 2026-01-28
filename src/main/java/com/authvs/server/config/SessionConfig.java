package com.authvs.server.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieHttpSessionIdResolver;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

import java.util.List;

@Configuration
public class SessionConfig {

    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return new CustomHttpSessionIdResolver();
    }

    /**
     * 自定义 Session ID 解析器
     * 优先尝试从 Header 获取 (支持 API 调用)
     * 如果 Header 没有，尝试从 Cookie 获取 (支持浏览器 OIDC 流程)
     */
    static class CustomHttpSessionIdResolver implements HttpSessionIdResolver {

        private final HeaderHttpSessionIdResolver headerResolver = HeaderHttpSessionIdResolver.xAuthToken();
        private final CookieHttpSessionIdResolver cookieResolver = new CookieHttpSessionIdResolver();

        @Override
        public List<String> resolveSessionIds(HttpServletRequest request) {
            List<String> headerIds = headerResolver.resolveSessionIds(request);
            if (!headerIds.isEmpty()) {
                return headerIds;
            }
            return cookieResolver.resolveSessionIds(request);
        }

        @Override
        public void setSessionId(HttpServletRequest request, HttpServletResponse response, String sessionId) {
            headerResolver.setSessionId(request, response, sessionId);
            cookieResolver.setSessionId(request, response, sessionId);
        }

        @Override
        public void expireSession(HttpServletRequest request, HttpServletResponse response) {
            headerResolver.expireSession(request, response);
            cookieResolver.expireSession(request, response);
        }
    }
}
