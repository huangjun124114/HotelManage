package com.linxi.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserDetailsService userDetailsService;

    /** 跳过JWT验证的路径 */
    private static final List<String> EXCLUDE_PATHS = Arrays.asList(
            "/api/auth/login",
            "/doc.html",
            "/swagger",
            "/v2/api-docs",
            "/v3/api-docs",
            "/webjars"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();

        // 跳过不需要认证的路径
        if (shouldSkip(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 从Header获取Token
        String authHeader = request.getHeader(jwtTokenUtil.getHeader());
        if (authHeader == null || !authHeader.startsWith(jwtTokenUtil.getTokenPrefix())) {
            // 没有token，继续过滤器链（Spring Security会处理）
            filterChain.doFilter(request, response);
            return;
        }

        String token = jwtTokenUtil.getTokenFromHeader(authHeader);

        // token存在但验证失败（过期或无效），返回401
        if (token != null && !jwtTokenUtil.validateToken(token)) {
            log.warn("Token验证失败，返回401: {}", requestPath);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"登录已过期，请重新登录\"}");
            return;
        }

        // token验证通过，设置认证信息
        if (token != null && jwtTokenUtil.validateToken(token)) {
            String username = jwtTokenUtil.getUsernameFromToken(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 从JWT token获取角色和权限
                List<String> roles = jwtTokenUtil.getRolesFromToken(token);
                List<String> permissions = jwtTokenUtil.getPermissionsFromToken(token);

                // 构建GrantedAuthority列表
                List<GrantedAuthority> authorities = new ArrayList<>();
                for (String role : roles) {
                    authorities.add(new SimpleGrantedAuthority(role));
                    // 为每个角色也添加基础USER角色
                    if (role.startsWith("ROLE_")) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                        break; // 只需要添加一次
                    }
                }
                for (String permission : permissions) {
                    authorities.add(new SimpleGrantedAuthority(permission));
                }

                // 创建Authentication对象，使用JWT中的用户信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        for (String excludePath : EXCLUDE_PATHS) {
            if (path.contains(excludePath)) {
                return true;
            }
        }
        return false;
    }
}
