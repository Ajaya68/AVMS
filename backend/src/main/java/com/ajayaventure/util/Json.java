package com.ajayaventure.util;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Shared JSON read/write helpers. All API payloads go through here so that
 * date formats and casing stay consistent across the platform.
 */
public final class Json {

    public static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    public static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .disableHtmlEscaping()
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonSerializer<LocalDateTime>) (src, type, ctx) ->
                            src == null ? com.google.gson.JsonNull.INSTANCE
                                    : new com.google.gson.JsonPrimitive(src.format(DATETIME_FMT)))
            .registerTypeAdapter(LocalDateTime.class,
                    (com.google.gson.JsonDeserializer<LocalDateTime>) (json, type, ctx) ->
                            json.getAsJsonPrimitive().isString()
                                    ? LocalDateTime.parse(json.getAsString(),
                                    DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    : null)
            .registerTypeAdapter(LocalDate.class,
                    (com.google.gson.JsonSerializer<LocalDate>) (src, type, ctx) ->
                            src == null ? com.google.gson.JsonNull.INSTANCE
                                    : new com.google.gson.JsonPrimitive(src.format(DATE_FMT)))
            .registerTypeAdapter(LocalDate.class,
                    (com.google.gson.JsonDeserializer<LocalDate>) (json, type, ctx) ->
                            json.getAsJsonPrimitive().isString()
                                    ? LocalDate.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE)
                                    : null)
            .create();

    private Json() {
    }

    /**
     * Serializes with field-name convention snake_case for API contracts.
     */
    public static String toApiJson(Object value) {
        Gson apiGson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .serializeNulls()
                .disableHtmlEscaping()
                .create();
        return apiGson.toJson(value);
    }

    public static <T> T fromJson(String json, Class<T> type) {
        Objects.requireNonNull(json, "json");
        return GSON.fromJson(json, type);
    }
}