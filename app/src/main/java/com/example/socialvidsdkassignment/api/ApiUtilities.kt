package com.example.socialvidsdkassignment.api

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

    @Module
    @InstallIn(SingletonComponent::class)
    class ApiUtilities {
        val baseUrl = "https://fatema.takatakind.com/"

        @Singleton
        @Provides
        fun getInstance() : Retrofit {
            return Retrofit.Builder().baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create()).build()
        }
        @Singleton
        @Provides
        fun provideInterface(retrofit: Retrofit): ApiInterface{
            return retrofit.create(ApiInterface::class.java)


        }

    }