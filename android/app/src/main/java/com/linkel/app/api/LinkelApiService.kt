package com.linkel.app.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface LinkelApiService {

    @GET("api/links")
    suspend fun getLinks(
        @Query("order") order: String = "desc",
        @Query("username") username: String? = null
    ): List<Link>

    @POST("api/links")
    suspend fun addLink(@Body link: NewLink): Link

    @DELETE("api/links/{id}")
    suspend fun deleteLink(
        @Path("id") id: Int,
        @Body body: DeleteRequest
    ): Response<Unit>
}
