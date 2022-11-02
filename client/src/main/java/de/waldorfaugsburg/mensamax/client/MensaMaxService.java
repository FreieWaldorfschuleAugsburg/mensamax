package de.waldorfaugsburg.mensamax.client;

import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface MensaMaxService {

    @GET("user/chip/{chip}")
    Call<MensaMaxUser> getUserByChip(@Path("chip") String chip);

    @GET("user/username/{username}")
    Call<MensaMaxUser> getUserByUsername(@Path("username") String username);

    @POST("transaction")
    Call<Void> transaction(@Query("chip") String chip,
                           @Query("kiosk") String kiosk,
                           @Query("barcode") long barcode,
                           @Query("quantity") int quantity);
}
