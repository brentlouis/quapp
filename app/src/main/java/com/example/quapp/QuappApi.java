package com.example.quapp;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    // ---- Queues --------------------------------------------------------------------

    /** Browse: every upcoming, open and paused queue. The app filters them itself. */
    @GET("queues")
    Call<List<Queue>> queues();

    @GET("queues/{id}")
    Call<Queue> queue(@Path("id") String queueId);

    // ---- The queuer's tickets ------------------------------------------------------

    /** Join. The location is only needed when the queue checks proximity. */
    @POST("queues/{id}/tickets")
    Call<Ticket> join(@Path("id") String queueId, @Body JoinBody body);

    /** live=true: My tickets; live=false: Queue history. */
    @GET("me/tickets")
    Call<List<Ticket>> myTickets(@Query("live") boolean live);

    @GET("tickets/{id}")
    Call<Ticket> ticket(@Path("id") String ticketId);

    @DELETE("tickets/{id}")
    Call<Void> leave(@Path("id") String ticketId);

    @POST("tickets/{id}/here")
    Call<Ticket> here(@Path("id") String ticketId);

    /** dryRun=true answers where the ticket would land without moving it (the sheet's preview). */
    @POST("tickets/{id}/move-back")
    Call<Ticket> moveBack(@Path("id") String ticketId, @Query("dry_run") boolean dryRun,
                          @Body MoveBackBody body);

    @GET("me/cooldown")
    Call<CooldownState> cooldown();

    @POST("queues/{id}/reports")
    Call<Void> report(@Path("id") String queueId, @Body ReportBody body);

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

    /**
     * Join: where the queuer is (read once, only for queues that check proximity) and who the
     * ticket is for (the account, unless Join's Edit changed it). Null fields are left out.
     */
    final class JoinBody {
        final Double latitude;
        final Double longitude;
        final String holderName;
        final String holderPhone;

        JoinBody(@Nullable Double latitude, @Nullable Double longitude, String holderName,
                 String holderPhone) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.holderName = holderName;
            this.holderPhone = holderPhone;
        }
    }

    final class MoveBackBody {
        final int minutesNeeded;

        MoveBackBody(int minutesNeeded) {
            this.minutesNeeded = minutesNeeded;
        }
    }

    final class ReportBody {
        final Report.Reason reason;
        final String details;

        ReportBody(Report.Reason reason, @Nullable String details) {
            this.reason = reason;
            this.details = details;
        }
    }

    /** MODELS.md "Cooldown". */
    final class CooldownState {
        @Nullable
        Instant until;
        int strikes;
        int strikeLimit;
        int durationMinutes;
    }
}
