package de.waldorfaugsburg.mensamax.client;

import de.waldorfaugsburg.mensamax.transaction.MensaMaxTransaction;
import de.waldorfaugsburg.mensamax.transaction.TransactionStatus;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface TransactionService {

    @GET("transactions")
    Call<List<MensaMaxTransaction>> getAllTransactions();

    @GET("transactions/{status}")
    Call<List<MensaMaxTransaction>> getAllTransactionsByStatus(@Path("status") TransactionStatus status);

    @GET("transaction/{id}")
    Call<MensaMaxTransaction> getTransactionById(@Path("id") int id);

    @DELETE("transaction/{id}")
    Call<Void> deleteTransactionById(@Path("id") int id);

    @POST("transaction")
    Call<Void> transaction(@Query("chip") String chip, @Query("kiosk") String kiosk, @Query("barcode") long barcode, @Query("quantity") int quantity);
}
