package com.restart.spacestationtracker.di

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.restart.spacestationtracker.BuildConfig
import com.restart.spacestationtracker.domain.iss_live.repository.IssRepository
import com.restart.spacestationtracker.domain.iss_live.use_case.GetFutureIssLocationsUseCase
import com.restart.spacestationtracker.domain.iss_passes.repository.IssPassesRepository
import com.restart.spacestationtracker.domain.iss_passes.use_case.GetIssPassesUseCase
import com.restart.spacestationtracker.domain.people_in_space.repository.PeopleInSpaceRepository
import com.restart.spacestationtracker.domain.youtube.repository.YouTubeRepository
import com.restart.spacestationtracker.domain.youtube.use_case.GetNasaLiveStreamStatusUseCase
import com.restart.spacestationtracker.shared.network.KtorSpaceStationRepository
import com.restart.spacestationtracker.shared.network.NetworkConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(@ApplicationContext context: Context): NetworkConfig {
        return NetworkConfig(
            youtubeLiveStreamsUrl = BuildConfig.YOUTUBE_LIVE_STREAMS_URL,
            youtubeApiKey = BuildConfig.YOUTUBE_API_KEY,
            n2yoApiKey = BuildConfig.N2YO_API_KEY,
            youtubeRequestHeaders = mapOf(
                "X-Android-Package" to context.packageName,
                "X-Android-Cert" to getCertificateSha1Fingerprint(context)
            )
        )
    }

    @Provides
    @Singleton
    fun provideSpaceStationRepository(
        config: NetworkConfig
    ): KtorSpaceStationRepository = KtorSpaceStationRepository(config)

    @Provides
    fun provideIssRepository(
        repository: KtorSpaceStationRepository
    ): IssRepository = repository

    @Provides
    fun provideIssPassesRepository(
        repository: KtorSpaceStationRepository
    ): IssPassesRepository = repository

    @Provides
    fun providePeopleInSpaceRepository(
        repository: KtorSpaceStationRepository
    ): PeopleInSpaceRepository = repository

    @Provides
    fun provideYouTubeRepository(
        repository: KtorSpaceStationRepository
    ): YouTubeRepository = repository

    @Provides
    fun provideGetFutureIssLocationsUseCase(
        repository: IssRepository
    ): GetFutureIssLocationsUseCase = GetFutureIssLocationsUseCase(repository)

    @Provides
    fun provideGetIssPassesUseCase(
        repository: IssPassesRepository
    ): GetIssPassesUseCase = GetIssPassesUseCase(repository)

    @Provides
    fun provideGetNasaLiveStreamStatusUseCase(
        repository: YouTubeRepository
    ): GetNasaLiveStreamStatusUseCase = GetNasaLiveStreamStatusUseCase(repository)

    private fun getCertificateSha1Fingerprint(context: Context): String {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            @Suppress("DEPRECATION")
            PackageManager.GET_SIGNATURES
        }
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, flags)
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signingInfo = packageInfo.signingInfo ?: return ""
            if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
            }
        } else {
            @Suppress("DEPRECATION")
            packageInfo.signatures
        }
        val certificate = signatures?.firstOrNull()?.toByteArray() ?: return ""
        return MessageDigest.getInstance("SHA-1")
            .digest(certificate)
            .joinToString(":") { byte -> "%02X".format(byte) }
    }
}
