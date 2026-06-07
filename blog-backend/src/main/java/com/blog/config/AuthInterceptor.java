package com.blog.config;

import com.blog.service.impl.UserServiceImpl;
import com.blog.util.AuthContext;
import com.blog.util.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserServiceImpl userService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/")) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");

        // GET requests: optional auth (allow anonymous browsing)
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    if (!userService.isTokenBlacklisted(token)) {
                        Claims claims = jwtUtil.parseToken(token);
                        Long userId = jwtUtil.getUserId(claims);
                        String username = jwtUtil.getUsername(claims);
                        String role = jwtUtil.getRole(claims);
                        AuthContext.set(userId, username, role);
                    }
                } catch (Exception ignored) {
                    // Token invalid → proceed as anonymous
                }
            }
            return true;
        }

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Missing or invalid Authorization header\",\"data\":null}");
            return false;
        }

        String token = authHeader.substring(7);

        try {
            if (userService.isTokenBlacklisted(token)) {
                response.setStatus(401);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":401,\"message\":\"Token has been revoked\",\"data\":null}");
                return false;
            }

            Claims claims = jwtUtil.parseToken(token);
            Long userId = jwtUtil.getUserId(claims);
            String username = jwtUtil.getUsername(claims);
            String role = jwtUtil.getRole(claims);

            AuthContext.set(userId, username, role);
            return true;
        } catch (ExpiredJwtException e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Token expired\",\"data\":null}");
            return false;
        } catch (JwtException e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid token\",\"data\":null}");
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
