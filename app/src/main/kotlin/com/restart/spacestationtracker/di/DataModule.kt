package com.restart.spacestationtracker.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.restart.spacestationtracker.data.iss_live.remote.IssApiService
import com.restart.spacestationtracker.data.iss_live.repository.IssRepositoryImpl
import com.restart.spacestationtracker.data.iss_passes.remote.IssPassesApiService
import com.restart.spacestationtracker.data.iss_passes.repository.IssPassesRepositoryImpl
import com.restart.spacestationtracker.data.people_in_space.remote.PeopleInSpaceApiService
import com.restart.spacestationtracker.data.people_in_space.repository.PeopleInSpaceRepositoryImpl
import com.restart.spacestationtracker.data.youtube.remote.YouTubeApiService
import com.restart.spacestationtracker.data.youtube.repository.YouTubeRepositoryImpl
import com.restart.spacestationtracker.domain.iss_live.repository.IssRepository
import com.restart.spacestationtracker.domain.iss_passes.repository.IssPassesRepository
import com.restart.spacestationtracker.domain.people_in_space.repository.PeopleInSpaceRepository
import com.restart.spacestationtracker.domain.youtube.repository.YouTubeRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val original = chain.request()
                val requestBuilder = original.newBuilder()
                    .header("User-Agent", "SpaceStationTracker/7.02; restartapplication@gmail.com")
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
    @Named("YouTubeApi")
    fun provideYouTubeApiService(
        okHttpClient: OkHttpClient,
        @ApplicationContext context: Context
    ): Retrofit {
        val youtubeClient = okHttpClient.newBuilder()
            .addInterceptor { chain ->
                val pkgName = context.packageName
                val cert = getCertificateSHA1Fingerprint(context)
                
                val request = chain.request().newBuilder()
                    .addHeader("X-Android-Package", pkgName)
                    .addHeader("X-Android-Cert", cert)
                    .build()
                chain.proceed(request)
            }
            .build()

        return Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(youtubeClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun getCertificateSHA1Fingerprint(context: Context): String {
        val pm = context.packageManager
        val packageName = context.packageName
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val packageInfo = pm.getPackageInfo(packageName, flags)
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return ""
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            packageInfo.signatures
        }
        
        if (signatures == null || signatures.isEmpty()) return ""

        val cert = signatures[0].toByteArray()
        val md = MessageDigest.getInstance("SHA-1")
        val publicKey = md.digest(cert)
        val hexString = StringBuilder()
        for (i in publicKey.indices) {
            val appendString = Integer.toHexString(0xFF and publicKey[i].toInt())
            if (appendString.length == 1) hexString.append("0")
            hexString.append(appendString)
            if (i < publicKey.size - 1) hexString.append(":")
        }
        return hexString.toString().uppercase()
    }

    @Provides
    @Singleton
    fun provideYouTubeService(@Named("YouTubeApi") retrofit: Retrofit): YouTubeApiService {
        return retrofit.create(YouTubeApiService::class.java)
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
    fun provideYouTubeRepository(api: YouTubeApiService): YouTubeRepository {
        return YouTubeRepositoryImpl(api)
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
