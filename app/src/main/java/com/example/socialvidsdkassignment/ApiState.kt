package com.example.socialvidsdkassignment

import com.example.socialvidsdkassignment.model.Msg

sealed class ApiState {
    class Success(val data: List<Msg>) : ApiState()
    class Failure(val msg:Throwable) : ApiState()
    object Loading:ApiState()
    object Empty: ApiState()

}