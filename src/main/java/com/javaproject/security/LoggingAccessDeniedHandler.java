package com.javaproject.security;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Handles access denied events by logging the attempt
 * and redirecting to the permission-denied page.
 */
@Component
public class LoggingAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(LoggingAccessDeniedHandler.class);

    private static final String PERMISSION_DENIED_URL = "/permission-denied";

    @Override
    public void handle(HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        logger.warn("Access denied: {}", accessDeniedException.getMessage());

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            logger.warn("Unauthorised access — user: {} tried to access: {}",
                    auth.getName(), request.getRequestURI());
        }

        response.sendRedirect(PERMISSION_DENIED_URL);
    }
}
