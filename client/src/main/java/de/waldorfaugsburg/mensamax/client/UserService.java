package de.waldorfaugsburg.mensamax.client;

import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserService {

    @GET("user/username/{username}")
    Call<MensaMaxUser> getUserByUsername(@Path("username") String username);

    @GET("user/employeeId/{employeeId}")
    Call<MensaMaxUser> getUserByEmployeeId(@Path("employeeId") int employeeId);

}
