package com.example.socialvidsdkassignment.repository

import com.example.socialvidsdkassignment.api.ApiInterface
import com.example.socialvidsdkassignment.model.Msg
import com.example.socialvidsdkassignment.model.VideoApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class VideoRepository
@Inject
constructor(private val apiInterface: ApiInterface) {
    fun getVideo(): Flow<List<Msg>> = flow {
        try {
            // Fetch the video data from the API
            val response: Response<VideoApi> = apiInterface.getVideo()

            if (response.isSuccessful) {
                val videoApi: VideoApi? = response.body()
                if (videoApi != null) {
                    // Emit the list of messages
                    emit(videoApi.msg)
                } else {
                    emit(emptyList())
                }
            } else {
                emit(emptyList())
            }
        } catch (e: IOException) {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)
}