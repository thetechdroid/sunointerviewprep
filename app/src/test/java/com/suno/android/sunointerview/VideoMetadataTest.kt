package com.suno.android.sunointerview

import ApiResponse
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class VideoMetadataTest {
    private val json = Json {
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    @Test
    fun filtersMissingVideosAndUsesExistingClipMetadata() {
        val response = json.decodeFromString<ApiResponse>(
            """
            {"songs": [null, {}, {"clip": {}}, {"clip": {"video_url": " "}},
              {"clip": {"video_url": " https://example.com/first.mp4 ",
                        "title": "First", "display_name": "Artist"}},
              {"clip": {"video_url": "https://example.com/second.mp4",
                        "title": " ", "handle": "creator"}},
              {"clip": {"video_url": "https://example.com/first.mp4"}}]}
            """.trimIndent()
        )

        assertEquals(
            listOf(
                VideoCard("https://example.com/first.mp4", "First", "Artist"),
                VideoCard("https://example.com/second.mp4", "Untitled video", "creator")
            ),
            response.videoCards()
        )
    }

    @Test
    fun missingSongsProducesEmptyFeed() {
        assertEquals(emptyList<VideoCard>(), json.decodeFromString<ApiResponse>("{}").videoCards())
    }
}
