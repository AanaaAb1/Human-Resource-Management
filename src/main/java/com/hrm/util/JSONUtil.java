package com.hrm.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Utility class for JSON operations
 */
public class JSONUtil {
    
    private static final Gson gson = new GsonBuilder()
        .registerTypeAdapter(LocalDate.class, (com.google.gson.JsonSerializer<LocalDate>) (src, type, context) -> {
            return new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE));
        })
        .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, context) -> {
            return new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        })
        .registerTypeAdapter(LocalDate.class, (com.google.gson.JsonDeserializer<LocalDate>) (json, type, context) -> {
            return LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE);
        })
        .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, context) -> {
            return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        })
        .setPrettyPrinting()
        .create();
    
    /**
     * Convert object to JSON string
     * @param obj Object to convert
     * @return JSON string
     */
    public static String toJSON(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * Convert list to JSON array
     * @param list List to convert
     * @return JSON array string
     */
    public static String toJSONArray(List<?> list) {
        return gson.toJson(list);
    }
    
    /**
     * Parse JSON string to JsonObject
     * @param jsonStr JSON string
     * @return JsonObject
     */
    public static JsonObject parseJSON(String jsonStr) {
        try {
            return JsonParser.parseString(jsonStr).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Send success response
     * @param response HttpServletResponse
     * @param message Success message
     * @param data Response data
     * @throws IOException if response fails
     */
    public static void sendSuccess(HttpServletResponse response, String message, Object data) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", true);
        jsonResponse.addProperty("message", message);
        if (data != null) {
            jsonResponse.add("data", gson.toJsonTree(data));
        }
        sendJSON(response, jsonResponse, HttpServletResponse.SC_OK);
    }
    
    /**
     * Send success response with created status
     * @param response HttpServletResponse
     * @param message Success message
     * @param data Response data
     * @throws IOException if response fails
     */
    public static void sendCreated(HttpServletResponse response, String message, Object data) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", true);
        jsonResponse.addProperty("message", message);
        if (data != null) {
            jsonResponse.add("data", gson.toJsonTree(data));
        }
        sendJSON(response, jsonResponse, HttpServletResponse.SC_CREATED);
    }
    
    /**
     * Send error response
     * @param response HttpServletResponse
     * @param message Error message
     * @param statusCode HTTP status code
     * @throws IOException if response fails
     */
    public static void sendError(HttpServletResponse response, String message, int statusCode) throws IOException {
        JsonObject jsonResponse = new JsonObject();
        jsonResponse.addProperty("success", false);
        jsonResponse.addProperty("message", message);
        jsonResponse.addProperty("statusCode", statusCode);
        sendJSON(response, jsonResponse, statusCode);
    }
    
    /**
     * Send bad request response
     * @param response HttpServletResponse
     * @param message Error message
     * @throws IOException if response fails
     */
    public static void sendBadRequest(HttpServletResponse response, String message) throws IOException {
        sendError(response, message, HttpServletResponse.SC_BAD_REQUEST);
    }
    
    /**
     * Send unauthorized response
     * @param response HttpServletResponse
     * @param message Error message
     * @throws IOException if response fails
     */
    public static void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        sendError(response, message, HttpServletResponse.SC_UNAUTHORIZED);
    }
    
    /**
     * Send forbidden response
     * @param response HttpServletResponse
     * @param message Error message
     * @throws IOException if response fails
     */
    public static void sendForbidden(HttpServletResponse response, String message) throws IOException {
        sendError(response, message, HttpServletResponse.SC_FORBIDDEN);
    }
    
    /**
     * Send not found response
     * @param response HttpServletResponse
     * @param message Error message
     * @throws IOException if response fails
     */
    public static void sendNotFound(HttpServletResponse response, String message) throws IOException {
        sendError(response, message, HttpServletResponse.SC_NOT_FOUND);
    }
    
    /**
     * Send internal server error response
     * @param response HttpServletResponse
     * @param message Error message
     * @throws IOException if response fails
     */
    public static void sendInternalError(HttpServletResponse response, String message) throws IOException {
        sendError(response, message, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }
    
    /**
     * Send JSON response
     * @param response HttpServletResponse
     * @param jsonResponse JsonObject to send
     * @param statusCode HTTP status code
     * @throws IOException if response fails
     */
    private static void sendJSON(HttpServletResponse response, JsonObject jsonResponse, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(jsonResponse.toString());
    }
    
    /**
     * Send JSON array response
     * @param response HttpServletResponse
     * @param list List to send
     * @param statusCode HTTP status code
     * @throws IOException if response fails
     */
    public static void sendJSONArray(HttpServletResponse response, List<?> list, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(toJSONArray(list));
    }
    
    /**
     * Create paginated response
     * @param list List of items
     * @param page Current page
     * @param pageSize Page size
     * @param totalItems Total number of items
     * @return JsonObject with pagination info
     */
    public static JsonObject createPaginatedResponse(List<?> list, int page, int pageSize, long totalItems) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", gson.toJsonTree(list));
        
        JsonObject pagination = new JsonObject();
        pagination.addProperty("currentPage", page);
        pagination.addProperty("pageSize", pageSize);
        pagination.addProperty("totalItems", totalItems);
        pagination.addProperty("totalPages", (int) Math.ceil((double) totalItems / pageSize));
        
        response.add("pagination", pagination);
        return response;
    }
    
    /**
     * Create response with metadata
     * @param data Main data
     * @param metadata Additional metadata
     * @return JsonObject with metadata
     */
    public static JsonObject createResponseWithMetadata(Object data, Map<String, Object> metadata) {
        JsonObject response = new JsonObject();
        response.addProperty("success", true);
        response.add("data", gson.toJsonTree(data));
        
        if (metadata != null) {
            JsonObject metaJson = new JsonObject();
            metadata.forEach(metaJson::addProperty);
            response.add("metadata", metaJson);
        }
        
        return response;
    }
}

