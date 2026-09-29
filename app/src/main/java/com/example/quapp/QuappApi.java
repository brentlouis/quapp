package com.example.quapp;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
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

    // ---- The organizer's queues ----------------------------------------------------

    /** Every status: live first, then upcoming, then closed. */
    @GET("me/queues")
    Call<List<Queue>> myQueues();

    @POST("queues")
    Call<Queue> createQueue(@Body QueueBody body);

    /** Every field is sent, so this saves the whole Edit screen at once. */
    @PATCH("queues/{id}")
    Call<Queue> editQueue(@Path("id") String queueId, @Body QueueBody body);

    @POST("queues/{id}/pause")
    Call<Queue> pause(@Path("id") String queueId);

    @POST("queues/{id}/resume")
    Call<Queue> resume(@Path("id") String queueId);

    @POST("queues/{id}/close")
    Call<Queue> close(@Path("id") String queueId);

    @POST("queues/{id}/extend")
    Call<Queue> extend(@Path("id") String queueId, @Body ExtendBody body);

    @GET("queues/{id}/stats")
    Call<QueueStats> stats(@Path("id") String queueId);

    // ---- The Live console ----------------------------------------------------------

    @GET("queues/{id}/line")
    Call<Line> line(@Path("id") String queueId);

    /** The one at the counter is served, the first in line is called. */
    @POST("queues/{id}/call-next")
    Call<Line> callNext(@Path("id") String queueId);

    /** The one at the counter didn't come, the first in line is called. */
    @POST("queues/{id}/no-show")
    Call<Line> noShow(@Path("id") String queueId);

    @POST("queues/{id}/walk-ins")
    Call<Ticket> walkIn(@Path("id") String queueId, @Body WalkInBody body);

    @POST("tickets/{id}/remove")
    Call<Void> remove(@Path("id") String ticketId, @Body RemoveBody body);

    // ---- Verification --------------------------------------------------------------

    /** The latest request (Profile's pending card), or an empty answer when there's none. */
    @GET("me/verification")
    Call<VerificationRequest> myVerification();

    @POST("me/verification")
    Call<VerificationRequest> askToBeVerified(@Body VerificationBody body);

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

    /**
     * Create and Edit Queue. Empty optional text is sent as "" rather than left out, so an
     * edit can clear it (the server turns "" into no value).
     */
    final class QueueBody {
        final String name;
        final Category category;
        final String shortDescription;
        final String details;
        final String bring;
        final String venue;
        final String municipality;
        final double latitude;
        final double longitude;
        final LocalDate startDate;
        final LocalDate endDate;
        final LocalTime opensAt;
        final LocalTime closesAt;
        final boolean gracePeriodEnabled;
        final boolean noShowCooldownEnabled;
        final boolean proximityCheckEnabled;
        final int joinRadiusMeters;

        QueueBody(Queue queue) {
            name = queue.getName();
            category = queue.getCategory();
            shortDescription = queue.getShortDescription();
            details = queue.getDetails() == null ? "" : queue.getDetails();
            bring = queue.getBring() == null ? "" : queue.getBring();
            venue = queue.getVenue();
            municipality = queue.getMunicipality();
            latitude = queue.getLatitude();
            longitude = queue.getLongitude();
            startDate = queue.getStartDate();
            endDate = queue.getEndDate();
            opensAt = queue.getOpensAt();
            closesAt = queue.getClosesAt();
            gracePeriodEnabled = queue.isGracePeriodEnabled();
            noShowCooldownEnabled = queue.isNoShowCooldownEnabled();
            proximityCheckEnabled = queue.isProximityCheckEnabled();
            joinRadiusMeters = queue.isProximityCheckEnabled() ? queue.getJoinRadiusMeters() : 0;
        }
    }

    final class ExtendBody {
        final LocalTime closesAt;

        ExtendBody(LocalTime closesAt) {
            this.closesAt = closesAt;
        }
    }

    final class WalkInBody {
        final String name;

        WalkInBody(String name) {
            this.name = name;
        }
    }

    final class RemoveBody {
        final Ticket.RemovalReason reason;

        RemoveBody(Ticket.RemovalReason reason) {
            this.reason = reason;
        }
    }

    final class VerificationBody {
        final String organizationName;
        final VerificationRequest.OrganizationType organizationType;
        final String position;
        final String officePhone;

        VerificationBody(String organizationName, VerificationRequest.OrganizationType organizationType,
                         String position, String officePhone) {
            this.organizationName = organizationName;
            this.organizationType = organizationType;
            this.position = position;
            this.officePhone = officePhone;
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
