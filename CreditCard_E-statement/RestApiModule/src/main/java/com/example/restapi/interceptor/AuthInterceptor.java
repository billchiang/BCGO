package com.example.restapi.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
// import java.util.Arrays; // Not strictly needed for the revised logic
// import java.util.List; // Not strictly needed for the revised logic

@Component
public class AuthInterceptor implements HandlerInterceptor {

    // The VALID_DUMMY_TOKEN_PREFIX is now just "VALID_TOKEN_" as per task description's logic
    // Example: "Bearer VALID_TOKEN_OPERATOR_SCOPE_STATEMENT_READ_SCOPE_STATEMENT_DOWNLOAD"
    // Example: "Bearer VALID_TOKEN_AUDITOR_SCOPE_STATEMENT_READ"

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing or invalid Authorization Bearer token.");
            return false;
        }

        String tokenWithBearerPrefix = authHeader; // Keep full "Bearer <token>" for prefix check if needed
        String token = tokenWithBearerPrefix.substring(7); // Remove "Bearer "

        // Task description checks token directly, not prefix on header.
        if (!token.startsWith("VALID_TOKEN_")) { // Basic check for our dummy "valid" tokens
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid token."); // Changed from SC_UNAUTHORIZED if token is present but bad
            return false;
        }
        
        // Simulated scope/role check based on token content
        // String permissionPart = token.substring("VALID_TOKEN_".length());
        // List<String> permissions = Arrays.asList(permissionPart.split("_SCOPE_")); // This logic is from previous version

        String requestUri = request.getRequestURI();
        String method = request.getMethod();

        if (requestUri.contains("/api/v1/statements") && "GET".equals(method)) {
            if (requestUri.endsWith("/status")) {
                // The task description's check is `!token.contains("STATEMENT_READ")`
                if (!token.contains("STATEMENT_READ")) { 
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient scope: Requires STATEMENT_READ.");
                    return false;
                }
            } else if (requestUri.contains("/download")) { 
                 // The task description's check is `!token.contains("STATEMENT_DOWNLOAD")`
                 if (!token.contains("STATEMENT_DOWNLOAD")) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Insufficient scope: Requires STATEMENT_DOWNLOAD.");
                    return false;
                }
            }
            // Add more path/method specific checks here if needed for other endpoints
        }
        
        // If all checks pass
        // request.setAttribute("user_roles", extractedRoles); // Example: pass decoded info
        // request.setAttribute("user_scopes", extractedScopes);
        return true;
    }
}
