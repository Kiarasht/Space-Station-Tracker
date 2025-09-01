package com.restart.spacestationtracker.di

import android.content.Context
import com.restart.spacestationtracker.data.iss_live.remote.IssApiService
import com.restart.spacestationtracker.data.iss_live.repository.IssRepositoryImpl
import com.restart.spacestationtracker.data.iss_passes.remote.IssPassesApiService
import com.restart.spacestationtracker.data.iss_passes.repository.IssPassesRepositoryImpl
import com.restart.spacestationtracker.data.people_in_space.remote.PeopleInSpaceApiService
import com.restart.spacestationtracker.data.people_in_space.repository.PeopleInSpaceRepositoryImpl
import com.restart.spacestationtracker.domain.iss_live.repository.IssRepository
import com.restart.spacestationtracker.domain.iss_passes.repository.IssPassesRepository
import com.restart.spacestationtracker.domain.people_in_space.repository.PeopleInSpaceRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", "SpaceStationTracker/6.02; restartapplication@gmail.com")
                val request = requestBuilder.build()
                chain.proceed(request)
            }
            .build()
    }

    @Provides
    @Singleton
    @Named("IssApi")
    fun provideIssApiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.wheretheiss.at/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("PeopleApi")
    fun providePeopleApiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.open-notify.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("WikiApi")
    fun provideWikiApiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("IssPassesApi")
    fun provideIssPassesApiRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl("https://api.n2yo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }


    @Provides
    @Singleton
    fun provideIssApiService(@Named("IssApi") retrofit: Retrofit): IssApiService {
        return retrofit.create(IssApiService::class.java)
    }

    @Provides
    @Singleton
    fun providePeopleInSpaceApiService(@Named("PeopleApi") retrofit: Retrofit): PeopleInSpaceApiService {
        return retrofit.create(PeopleInSpaceApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideIssPassesApiService(@Named("IssPassesApi") retrofit: Retrofit): IssPassesApiService {
        return retrofit.create(IssPassesApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideIssRepository(api: IssApiService): IssRepository {
        return IssRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun providePeopleInSpaceRepository(api: PeopleInSpaceApiService): PeopleInSpaceRepository {
        return PeopleInSpaceRepositoryImpl(api)
    }

    @Provides
    @Singleton
    fun provideIssPassesRepository(api: IssPassesApiService): IssPassesRepository {
        return IssPassesRepositoryImpl(api)
    }
}
