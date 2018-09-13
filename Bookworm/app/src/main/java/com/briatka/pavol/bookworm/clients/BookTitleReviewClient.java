package com.briatka.pavol.bookworm.clients;

import com.briatka.pavol.bookworm.customobjects.BookObject;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface BookTitleReviewClient {

    @GET("/book/title.xml")
    Call<BookObject> getReviews(@Query("key") String key, @Query("title") String title);
}
