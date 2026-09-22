package com.ajayaventure.servlet;

import com.ajayaventure.exception.ApiException;
import com.ajayaventure.exception.FieldError;
import com.ajayaventure.util.Json;
import com.ajayaventure.util.Responses;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Base API servlet: uniform JSON body reading and centralized exception →
 * standard error-envelope mapping. Subclasses implement handlers and delegate
 * to {@link #api(HttpServletRequest, HttpServletResponse, ApiAction)}.
 */
public abstract class ApiServlet extends HttpServlet {

    private static final Logger log = LoggerFactory.getLogger(ApiServlet.class);

    @FunctionalInterface
    protected interface ApiAction {
        void run(HttpServletRequest req, HttpServletResponse resp) throws Exception;
    }

    /** Executes an action and maps any thrown error to the standard envelope. */
    protected void api(HttpServletRequest req, HttpServletResponse resp, ApiAction action) throws IOException {
        try {
            action.run(req, resp);
        } catch (ApiException | FieldError.ValidationException e) {
            if (e instanceof ApiException api) {
                log.warn("Request {}{} failed ({}): {}",
                        req.getMethod(), req.getRequestURI(), api.getCode(), api.getMessage());
            } else {
                log.warn("Request {} {} failed validation", req.getMethod(), req.getRequestURI());
            }
            Responses.sendError(resp, req, e);
        } catch (Exception e) {
            log.error("Unexpected failure {} {}",
                    req.getMethod(), req.getRequestURI(), e);
            Responses.sendError(resp, req, e);
        }
    }

    protected <T> T readJson(HttpServletRequest req, Class<T> type) throws IOException {
        String body = new String(req.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        if (body.isBlank()) {
            return Json.GSON.fromJson("{}", type);
        }
        return Json.GSON.fromJson(body, type);
    }

    protected void ok(HttpServletRequest req, HttpServletResponse resp, Object data) throws IOException {
        Responses.send(resp, 200, data, Responses.requestId(req));
    }

    protected void created(HttpServletRequest req, HttpServletResponse resp, Object data) throws IOException {
        Responses.send(resp, 201, data, Responses.requestId(req));
    }

    protected int pageOf(HttpServletRequest req) {
        return parseInt(req.getParameter("page"), 1);
    }

    protected int pageSizeOf(HttpServletRequest req) {
        int size = parseInt(req.getParameter("page_size"), 20);
        return Math.min(Math.max(size, 1), 100);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}