package com.ajayaventure.util;

import com.ajayaventure.exception.ApiException;
import com.ajayaventure.exception.FieldError;
import com.ajayaventure.exception.FieldError.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Standardizes all JSON API responses.
 *
 * <p>Successful payload: {@code {"success":true,"data":...,"requestId":"..."}}</p>
 * <p>Error payload:    {@code {"success":false,"error":{"code","message","fieldErrors"},"requestId":"..."}}</p>
 */
public final class Responses {

    private Responses() {
    }

    /** Returns the request id present on every request (set by RequestIdFilter). */
    public static String requestId(HttpServletRequest request) {
        Object rid = request.getAttribute("requestId");
        return rid == null ? "" : rid.toString();
    }

    public static void send(HttpServletResponse response, int status, Object data)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        response.getWriter().write(Json.toApiJson(body));
    }

    public static void send(HttpServletResponse response, int status, Object data, String requestId)
            throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", true);
        body.put("data", data);
        body.put("requestId", requestId);
        response.getWriter().write(Json.toApiJson(body));
    }

    public static void sendError(HttpServletResponse response, HttpServletRequest request, Throwable error)
            throws IOException {
        int status;
        String code;
        String message;
        List<FieldError> fieldErrors = null;

        if (error instanceof ApiException api) {
            status = api.getStatus();
            code = api.getCode();
            message = api.getMessage();
        } else if (error instanceof ValidationException ve) {
            status = 422;
            code = "VALIDATION_ERROR";
            message = "Request validation failed.";
            fieldErrors = ve.getErrors();
        } else {
            status = 500;
            code = "INTERNAL_ERROR";
            message = "An unexpected error occurred.";
        }

        Map<String, Object> errorBody = new LinkedHashMap<>();
        errorBody.put("code", code);
        errorBody.put("message", message);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            errorBody.put("fieldErrors", fieldErrors);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", false);
        body.put("error", errorBody);
        body.put("requestId", requestId(request));

        response.setStatus(status);
        response.setContentType("application/json; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(Json.toApiJson(body));
    }

    /** Standard paginated envelope: {@code {items, total, page, page_size, total_pages}}. */
    public static Map<String, Object> page(List<?> items, long total, int page, int pageSize) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("total", total);
        body.put("page", page);
        body.put("page_size", pageSize);
        body.put("total_pages", pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0);
        return body;
    }
}