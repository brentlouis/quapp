package com.example.quapp;

import android.content.Context;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * The one connection to the Quapp server (BACKEND.md step 10.1). Built once and shared:
 *
 *     ApiClient.api(context).login(body).enqueue(new ApiCallback<AuthResponse>(this) { … });
 *
 * Three things happen to every request here, so screens don't repeat them:
 * - the server's address comes from the build (BuildConfig.API_URL, set in local.properties);
 * - the signed-in user's token goes in the Authorization header;
 * - JSON is read with Gson: snake_case names map to the models' camelCase fields
 *   (holder_phone → holderPhone), and dates and times become java.time values.
 */
public final class ApiClient {

    private static QuappApi api;
    private static Gson gson;

    private ApiClient() {
    }

    /** The API, built on first use. `synchronized` so two threads can't build it twice. */
    public static synchronized QuappApi api(Context context) {
        if (api == null) {
            final Session session = new Session(context.getApplicationContext());

            OkHttpClient http = new OkHttpClient.Builder()
                    // Add "Authorization: Bearer <token>" when someone is signed in. The token is
                    // read on every request, so logging in or out takes effect immediately.
                    .addInterceptor(chain -> {
                        Request request = chain.request();
                        String token = session.getToken();
                        if (token != null) {
                            request = request.newBuilder()
                                    .header("Authorization", "Bearer " + token)
                                    .build();
                        }
                        return chain.proceed(request);
                    })
                    // Each request in Logcat (tag "okhttp"): method, address, status, time. Not
                    // the bodies: those include passwords and tokens.
                    .addInterceptor(new HttpLoggingInterceptor()
                            .setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC
                                    : HttpLoggingInterceptor.Level.NONE))
                    // A PC on the same network answers fast; give up after 10 s so a stopped
                    // server shows the offline message instead of a spinner forever
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .build();

            api = new Retrofit.Builder()
                    .baseUrl(BuildConfig.API_URL)
                    .client(http)
                    .addConverterFactory(GsonConverterFactory.create(gson()))
                    .build()
                    .create(QuappApi.class);
        }
        return api;
    }

    /**
     * How JSON becomes Java objects and back (MODELS.md conventions). Gson fills even the
     * models' final fields, so they stay immutable.
     */
    public static synchronized Gson gson() {
        if (gson == null) {
            gson = new GsonBuilder()
                    // "organizer_verified" ↔ organizerVerified, for every field
                    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                    // One adapter per type that both reads and writes it, so each format is
                    // defined in one place (the classes at the bottom of this file)
                    .registerTypeAdapter(Instant.class, new InstantAdapter())
                    .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                    .registerTypeAdapter(LocalTime.class, new LocalTimeAdapter())
                    .create();
        }
        return gson;
    }

    /** "2026-09-29T10:05:00+08:00": a moment, with its offset. Sent in Manila time. */
    private static final class InstantAdapter
            implements JsonSerializer<Instant>, JsonDeserializer<Instant> {
        @Override
        public JsonElement serialize(Instant value, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(value.atZone(Format.MANILA)
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }

        @Override
        public Instant deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return OffsetDateTime.parse(json.getAsString()).toInstant();
        }
    }

    /** "2026-09-29": a date. */
    private static final class LocalDateAdapter
            implements JsonSerializer<LocalDate>, JsonDeserializer<LocalDate> {
        @Override
        public JsonElement serialize(LocalDate value, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(value.toString());
        }

        @Override
        public LocalDate deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return LocalDate.parse(json.getAsString());
        }
    }

    /** "08:00": a time of day. */
    private static final class LocalTimeAdapter
            implements JsonSerializer<LocalTime>, JsonDeserializer<LocalTime> {
        @Override
        public JsonElement serialize(LocalTime value, Type type, JsonSerializationContext context) {
            return new JsonPrimitive(value.format(DateTimeFormatter.ofPattern("HH:mm")));
        }

        @Override
        public LocalTime deserialize(JsonElement json, Type type, JsonDeserializationContext context) {
            return LocalTime.parse(json.getAsString());
        }
    }
}
