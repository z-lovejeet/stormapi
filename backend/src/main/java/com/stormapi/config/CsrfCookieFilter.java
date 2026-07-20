package com.stormapi.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Forces eager resolution of the CSRF token on every request.
 *
 * Spring Security 6 lazily generates the CSRF token — the XSRF-TOKEN cookie
 * is only written when the token is actually accessed. With stateless sessions
 * (JWT), this means the cookie may not exist when the frontend makes its first
 * POST request, causing a 403 Forbidden.
 *
 * This filter resolves the token on every request so the cookie is always set
 * before the frontend needs it.
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            // Force the token to be rendered — this triggers the cookie to be written
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
