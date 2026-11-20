package com.freshmart.smartstock.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshmart.smartstock.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

/** Writes the unified JSON envelope from servlet-level handlers (outside Spring MVC). */
final class SecurityResponseWriter {

    private SecurityResponseWriter() {
    }

    static void write(ObjectMapper mapper, HttpServletResponse response, int status, String message)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), ApiResponse.error(status, message, null));
    }
}
