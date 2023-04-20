package de.waldorfaugsburg.mensamax.client;

import com.google.gson.Gson;
import de.waldorfaugsburg.mensamax.client.api.ApiError;
import de.waldorfaugsburg.mensamax.client.api.ApiException;
import de.waldorfaugsburg.mensamax.common.MensaMaxUser;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

public class MensaMaxClient {

    private static final Gson GSON = new Gson();

    private static final ApiError SERVICE_UNAVAILABLE = new ApiError("SERVICE_UNAVAILABLE", null);
    private static final ApiError INVALID_CONTENT_TYPE = new ApiError("INVALID_CONTENT_TYPE", null);
    private static final ApiError EMPTY_BODY = new ApiError("EMPTY_BODY", null);
    private static final int SERVICE_UNAVAILABLE_CODE = 503;
    private static final Duration TIMEOUT_DURATION = Duration.ofMinutes(1);

    private final MensaMaxService service;

    public MensaMaxClient(final String endpointUrl, final String apiKey) {
        final OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.addInterceptor(chain -> chain.proceed(chain.request().newBuilder().addHeader("X-API-KEY", apiKey).build())).callTimeout(TIMEOUT_DURATION).connectTimeout(TIMEOUT_DURATION).readTimeout(TIMEOUT_DURATION).writeTimeout(TIMEOUT_DURATION);
        final OkHttpClient client = builder.build();
        final Retrofit retrofit = new Retrofit.Builder().baseUrl(endpointUrl).addConverterFactory(GsonConverterFactory.create(GSON)).client(client).build();

        this.service = retrofit.create(MensaMaxService.class);
    }

    public MensaMaxUser getUserByChipId(final String chipId) throws ApiException {
        final Call<MensaMaxUser> call = service.getUserByChip(chipId);
        final Response<MensaMaxUser> response = handleCall(call);
        return response.body();
    }

    public MensaMaxUser getUserByUsername(final String username) throws ApiException {
        final Call<MensaMaxUser> call = service.getUserByUsername(username);
        final Response<MensaMaxUser> response = handleCall(call);
        return response.body();
    }

    public void transaction(final String chipId, final String kiosk, final long productBarcode) {
        transaction(chipId, kiosk, productBarcode, 1);
    }

    public void transaction(final String chipId, final String kiosk, final long productBarcode, final int quantity) throws ApiException {
        final Call<Void> call = service.transaction(UUID.randomUUID(), chipId, kiosk, productBarcode, quantity);
        handleCall(call);
    }

    private <T> Response<T> handleCall(final Call<T> call) throws ApiException {
        try {
            final Response<T> response = call.execute();
            if (!response.isSuccessful()) {
                throw new ApiException(response.code(), parseError(response));
            }

            return response;
        } catch (final IOException e) {
            throw new ApiException(e);
        }
    }

    private ApiError parseError(final Response<?> response) throws IOException {
        if (response.code() == SERVICE_UNAVAILABLE_CODE) {
            return SERVICE_UNAVAILABLE;
        }

        final String contentType = response.headers().get("Content-Type");
        if (contentType == null || !contentType.equals("application/json;charset=UTF-8")) {
            return INVALID_CONTENT_TYPE;
        }

        final ResponseBody errorBody = response.errorBody();
        if (errorBody == null) {
            return EMPTY_BODY;
        }

        final String rawBody = errorBody.string();
        errorBody.close();
        return GSON.fromJson(rawBody, ApiError.class);
    }
}
