package com.example.quapp;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * The server's endpoints as Java methods (MODELS.md "Endpoints"). Retrofit writes the code
 * behind each one: the annotation says the HTTP method and path, @Body is sent as JSON, and the
 * return type is what the JSON answer is read into.
 *
 * Every method returns a Call, which does nothing until .enqueue(callback): the request then
 * runs on a background thread and the answer comes back to the callback on the main thread.
 *
 * Endpoints are added as the screens move from FakeData to the server (BACKEND.md phase 10).
 */
public interface QuappApi {

    // ---- Accounts ------------------------------------------------------------------

    @POST("auth/register")
    Call<AuthResponse> register(@Body RegisterBody body);

    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginBody body);

    /**
     * The token is passed in, not added by ApiClient: the app forgets its token right after
     * asking, and the request may leave after that.
     */
    @POST("auth/logout")
    Call<Void> logout(@Header("Authorization") String bearerToken);

    @GET("me")
    Call<User> me();

    // ---- Request and response bodies -----------------------------------------------
    // Plain holders for JSON. Gson writes and reads their fields by name (deviceInstallId ↔
    // "device_install_id"), so they need no getters.

    final class RegisterBody {
        final String name;
        final String phone;
        final String password;
        final String deviceInstallId;

        RegisterBody(String name, String phone, String password, String deviceInstallId) {
            this.name = name;
            this.phone = phone;
            this.password = password;
            this.deviceInstallId = deviceInstallId;
        }
    }

    final class LoginBody {
        final String phone;
        final String password;

        LoginBody(String phone, String password) {
            this.phone = phone;
            this.password = password;
        }
    }

    /** Register and login both answer with a token and the user. */
    final class AuthResponse {
        String token;
        User user;
    }
}
