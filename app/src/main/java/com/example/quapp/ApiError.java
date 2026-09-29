package com.example.quapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Why a request didn't work: the server's error (MODELS.md "Errors") or no connection at all.
 *
 *     {"error": "DEVICE_LIMIT", "message": "This phone already has 2 accounts."}
 *
 * Screens switch on {@link #code} to pick what to show; {@link #message} is plain English they
 * may show as-is. Some errors carry more fields ({@link #extra}): SUSPENDED has the reason,
 * INVALID_INPUT has one message per field.
 */
public final class ApiError {

    /** Not a server code: the request never got an answer (no network, server stopped). */
    public static final String OFFLINE = "OFFLINE";

    public final int httpStatus;
    public final String code;
    public final String message;
    /** The whole error body, for the extra fields some errors carry. */
    @NonNull
    public final JsonObject extra;

    private ApiError(int httpStatus, String code, String message, @NonNull JsonObject extra) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.extra = extra;
    }

    public static ApiError offline() {
        return new ApiError(0, OFFLINE, "Can't reach Quapp.", new JsonObject());
    }

    /** Reads an error body. Anything that isn't our JSON shape still becomes an ApiError. */
    public static ApiError parse(int httpStatus, @Nullable String body) {
        try {
            JsonObject json = JsonParser.parseString(body == null ? "" : body).getAsJsonObject();
            return new ApiError(httpStatus, string(json, "error", "HTTP_" + httpStatus),
                    string(json, "message", "Something went wrong."), json);
        } catch (RuntimeException notOurJson) {
            return new ApiError(httpStatus, "HTTP_" + httpStatus, "Something went wrong.",
                    new JsonObject());
        }
    }

    public boolean is(String code) {
        return this.code.equals(code);
    }

    /** One of the extra fields as text, or null ("suspended_reason"). */
    @Nullable
    public String extraString(String name) {
        return string(extra, name, null);
    }

    /** INVALID_INPUT's message for one field ("phone"), or null. */
    @Nullable
    public String fieldMessage(String field) {
        JsonElement fields = extra.get("fields");
        return fields != null && fields.isJsonObject() ? string(fields.getAsJsonObject(), field, null)
                : null;
    }

    private static String string(JsonObject json, String name, String fallback) {
        JsonElement value = json.get(name);
        return value == null || value.isJsonNull() ? fallback : value.getAsString();
    }
}
