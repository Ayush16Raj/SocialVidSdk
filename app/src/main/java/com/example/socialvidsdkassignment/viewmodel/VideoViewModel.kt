package com.example.socialvidsdkassignment.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.socialvidsdkassignment.ApiState
import com.example.socialvidsdkassignment.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoViewModel
@Inject
constructor(private val videoRepository: VideoRepository): ViewModel() {
    private val _response: MutableStateFlow<ApiState> = MutableStateFlow(ApiState.Empty)
    val response = _response

    init {
       getVideo()
    }

    private fun getVideo() = viewModelScope.launch {
        videoRepository.getVideo()
            .onStart {
                _response.value = ApiState.Loading
            }.catch {
                _response.value = ApiState.Failure(it)
            }.collect {
                _response.value = ApiState.Success(it)
            }
    }

}