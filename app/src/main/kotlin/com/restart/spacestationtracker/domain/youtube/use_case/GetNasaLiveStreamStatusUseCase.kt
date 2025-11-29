package com.restart.spacestationtracker.domain.youtube.use_case

import com.restart.spacestationtracker.domain.youtube.model.LiveStream
import com.restart.spacestationtracker.domain.youtube.repository.YouTubeRepository
import javax.inject.Inject

class GetNasaLiveStreamStatusUseCase @Inject constructor(
    private val youTubeRepository: YouTubeRepository
) {
    suspend operator fun invoke(): List<LiveStream> {
        return youTubeRepository.getNasaLiveStreams()
    }
}
