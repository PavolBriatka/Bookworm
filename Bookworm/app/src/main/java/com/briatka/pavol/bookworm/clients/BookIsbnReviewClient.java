package com.briatka.pavol.bookworm.clients;

import com.briatka.pavol.bookworm.customobjects.BookObject;

import io.reactivex.Observable;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface BookIsbnReviewClient {

    @GET("/book/isbn/{isbn}")
    Observable<Response<BookObject>> getReviewsIsbn(@Path("isbn") String isbn, @Query("key") String key);
}
