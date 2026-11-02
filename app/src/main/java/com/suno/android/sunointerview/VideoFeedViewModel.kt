package com.suno.android.sunointerview

import ApiResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class VideoCard(val url: String, val title: String, val creator: String)

internal fun ApiResponse.videoCards(): List<VideoCard> = songs.orEmpty()
    .mapNotNull { song ->
        val clip = song?.clip ?: return@mapNotNull null
        val url = clip.videoUrl?.trim()?.takeIf { it.isNotEmpty() }
            ?: return@mapNotNull null
        VideoCard(
            url = url,
            title = clip.title?.takeIf { it.isNotBlank() } ?: "Untitled video",
            creator = clip.displayName?.takeIf { it.isNotBlank() } ?: clip.handle.orEmpty()
        )
    }
    .distinctBy { it.url }

data class VideoFeedState(
    val loading: Boolean = true,
    val videos: List<VideoCard> = emptyList(),
    val error: String? = null
)

class VideoFeedViewModel : ViewModel() {
    private val mutableState = MutableStateFlow(VideoFeedState())
    val state = mutableState.asStateFlow()
    private val service = ApiClient.getInstance().create(ApiService::class.java)

    init {
        loadVideos()
    }

    fun loadVideos() {
        viewModelScope.launch {
            mutableState.value = VideoFeedState()
            try {
                val response = service.getSongs()
                if (!response.isSuccessful) {
                    mutableState.value = VideoFeedState(
                        loading = false,
                        error = "Couldn't load videos (${response.code()}). Please retry."
                    )
                    return@launch
                }
                mutableState.value = VideoFeedState(
                    loading = false,
                    videos = response.body()?.videoCards().orEmpty()
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableState.value = VideoFeedState(
                    loading = false,
                    error = "Couldn't load videos. Check your connection and retry."
                )
            }
        }
    }
}
