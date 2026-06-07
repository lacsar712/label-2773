package com.example.employee.filter;

import com.example.employee.common.Result;
import com.example.employee.config.JwtConfig;
import com.example.employee.context.UserContext;
import com.example.employee.dto.UserInfoDTO;
import com.example.employee.exception.BusinessException;
import com.example.employee.service.AuthService;
import com.example.employee.utils.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JwtConfig jwtConfig;

    @Autowired
    private AuthService authService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final List<String> WHITE_LIST = List.of(
            "/api/auth/captcha",
            "/api/auth/login",
            "/api/auth/refresh-token",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/doc.html",
            "/webjars",
            "/favicon.ico"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String requestUri = request.getRequestURI();
            boolean isWhitelisted = WHITE_LIST.stream().anyMatch(requestUri::startsWith);

            String header = request.getHeader(jwtConfig.getHeader());
            if (header != null && header.startsWith(jwtConfig.getPrefix())) {
                String token = header.substring(jwtConfig.getPrefix().length());
                try {
                    authService.validateTokenAndSetContext(token);

                    UserInfoDTO userInfo = UserContext.getCurrentUser();
                    if (userInfo != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userInfo.getUsername(),
                                        null,
                                        AuthorityUtils.createAuthorityList("ROLE_" + userInfo.getRoleCode())
                                );
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                } catch (BusinessException e) {
                    if (!isWhitelisted) {
                        response.setStatus(HttpServletResponse.SC_OK);
                        response.setContentType("application/json;charset=UTF-8");
                        response.getWriter().write(objectMapper.writeValueAsString(
                                Result.error(e.getCode(), e.getMessage())
                        ));
                        return;
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
