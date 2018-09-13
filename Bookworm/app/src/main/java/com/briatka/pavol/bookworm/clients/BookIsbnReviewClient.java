package com.briatka.pavol.bookworm.clients;

import com.briatka.pavol.bookworm.customobjects.BookObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BookIsbnReviewClient {

    @GET("/book/isbn/{isbn}")
    Call<BookObject> getReviewsIsbn(@Path("isbn") String isbn, @Query("key") String key);
}
