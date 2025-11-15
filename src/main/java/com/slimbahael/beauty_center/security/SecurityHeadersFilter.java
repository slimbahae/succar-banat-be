package com.slimbahael.beauty_center.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Security Headers Filter
 * Adds security headers to all HTTP responses to protect against common web vulnerabilities
 * Addresses OWASP ZAP findings for missing security headers
 */
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Prevent MIME-sniffing attacks (CWE-693)
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // Prevent clickjacking attacks (CWE-1021)
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // Enable XSS protection in older browsers
        httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

        // Control referrer information
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Restrict browser features
        httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // Prevent caching of sensitive data on API endpoints
        String requestURI = ((jakarta.servlet.http.HttpServletRequest) request).getRequestURI();
        if (requestURI.startsWith("/api/")) {
            httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, private");
            httpResponse.setHeader("Pragma", "no-cache");
            httpResponse.setHeader("Expires", "0");
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // Initialization logic if needed
    }

    @Override
    public void destroy() {
        // Cleanup logic if needed
    }
}
