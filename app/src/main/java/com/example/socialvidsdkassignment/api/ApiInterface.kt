package com.example.socialvidsdkassignment.api

import com.example.socialvidsdkassignment.model.VideoApi
import retrofit2.Response
import retrofit2.http.GET

interface ApiInterface {

    @GET("app_api/index.php?p=showAllVideos")
    suspend fun getVideo(): Response<VideoApi>

}