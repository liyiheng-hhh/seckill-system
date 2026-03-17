package com.seckill.user.config;

import com.seckill.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.regex.Pattern;

@Component
@Order(1)
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Pattern AUTH_REQUIRED_PATTERN = Pattern.compile("^/users/\\d+$");
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if ("POST".equals(method) && ("/users/register".equals(path) || "/users/login".equals(path))) {
            return true;
        }
        if ("GET".equals(method) && AUTH_REQUIRED_PATTERN.matcher(path).matches()) {
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未提供认证信息\"}");
            return;
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtUtil.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"认证信息无效或已过期\"}");
            return;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        request.setAttribute("userId", userId);
        filterChain.doFilter(request, response);
    }
}
