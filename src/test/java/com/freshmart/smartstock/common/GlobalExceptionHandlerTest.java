package com.freshmart.smartstock.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.freshmart.smartstock.common.api.ApiResponse;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.common.exception.GlobalExceptionHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** One test per branch of the handler: status code, Vietnamese message, envelope, and no leaked internals. */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void apiExceptionKeepsStatusMessageAndData() {
        List<String> shortages = List.of("sp1");

        ResponseEntity<ApiResponse<Object>> response = handler.handleApi(ApiException.conflict("Thiếu tồn", shortages));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().status());
        assertEquals("Thiếu tồn", response.getBody().message());
        assertEquals(shortages, response.getBody().data());
    }

    @Test
    void validationErrorReturns400WithFirstMessagePerField() throws Exception {
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "request");
        binding.addError(new FieldError("request", "email", "Email sai"));
        binding.addError(new FieldError("request", "email", "Email trống"));
        binding.addError(new FieldError("request", "password", "Mật khẩu ngắn"));

        ResponseEntity<ApiResponse<Object>> response = handler.handleValidation(
                new MethodArgumentNotValidException(null, binding));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(Map.of("email", "Email sai", "password", "Mật khẩu ngắn"), response.getBody().data());
    }

    @Test
    void malformedJsonReturns400() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleBadRequest(
                new HttpMessageNotReadableException("bad json", new MockHttpInputMessage(new byte[0])));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNull(response.getBody().data());
    }

    @Test
    void missingParameterReturns400() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleBadRequest(
                new MissingServletRequestParameterException("from", "LocalDate"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void authenticationErrorReturns401() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleAuthentication(new BadCredentialsException("x"));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals(401, response.getBody().status());
    }

    @Test
    void accessDeniedReturns403() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleAccessDenied(new AccessDeniedException("no"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().status());
    }

    @Test
    void dataIntegrityViolationReturns409WithoutSqlDetails() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleConflict(
                new DataIntegrityViolationException("Duplicate entry 'x' for key 'uk_secret_index'"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertTrue(!response.getBody().message().contains("uk_secret_index"));
    }

    @Test
    void unknownRouteReturns404() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleNoResource(
                new NoResourceFoundException(HttpMethod.GET, "/api/nope"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    @Test
    void wrongHttpMethodReturns405() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleMethodNotAllowed(
                new HttpRequestMethodNotSupportedException("DELETE"));

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }

    @Test
    void unexpectedErrorReturns500WithoutLeakingInternals() {
        ResponseEntity<ApiResponse<Object>> response = handler.handleUnexpected(
                new IllegalStateException("jdbc:mysql://secret-host/db password=abc"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Lỗi hệ thống", response.getBody().message());
        assertNull(response.getBody().data());
    }
}
