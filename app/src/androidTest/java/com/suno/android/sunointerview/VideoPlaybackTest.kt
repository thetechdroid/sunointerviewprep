package com.suno.android.sunointerview

import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.geometry.Offset
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoPlaybackTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun loadingPanelShowsBufferedPercentage() {
        composeRule.setContent {
            MaterialTheme {
                LoadingOverlay(progressPercent = 56)
            }
        }
        composeRule.onNodeWithText("Loading 56%").assertIsDisplayed()
    }

    @Test
    fun swipesStartingInBottomAreaDoNotChangeVideos() {
        composeRule.setContent {
            MaterialTheme {
                VideoPager(
                    listOf(
                        VideoCard("file:///first-test.wav", "First", "Test artist"),
                        VideoCard("file:///second-test.wav", "Second", "Test artist")
                    )
                )
            }
        }
        composeRule.onNodeWithText("1 / 2").assertIsDisplayed()
        composeRule.onRoot().performTouchInput {
            swipe(
                start = Offset(width * 0.5f, height * 0.95f),
                end = Offset(width * 0.5f, height * 0.55f)
            )
        }
        composeRule.onNodeWithText("1 / 2").assertIsDisplayed()
        composeRule.onRoot().performTouchInput {
            swipe(
                start = Offset(width * 0.5f, height * 0.7f),
                end = Offset(width * 0.5f, height * 0.2f)
            )
        }
        composeRule.onNodeWithText("2 / 2").assertIsDisplayed()
    }

    @Test
    fun musicCanPauseResumeAndRestartAfterCompletion() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val audioFile = File(context.cacheDir, "playback-control-test.wav")
        val sampleRate = 8000
        val dataSize = sampleRate * 2 * 3
        val audio = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
            .put("RIFF".toByteArray(Charsets.US_ASCII))
            .putInt(36 + dataSize)
            .put("WAVEfmt ".toByteArray(Charsets.US_ASCII))
            .putInt(16)
            .putShort(1.toShort())
            .putShort(1.toShort())
            .putInt(sampleRate)
            .putInt(sampleRate * 2)
            .putShort(2.toShort())
            .putShort(16.toShort())
            .put("data".toByteArray(Charsets.US_ASCII))
            .putInt(dataSize)
            .array()
        audioFile.writeBytes(audio)

        composeRule.setContent {
            MaterialTheme {
                VideoSurface(
                    video = VideoCard(Uri.fromFile(audioFile).toString(), "Test music", "Test artist"),
                    active = true
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 20000) {
            composeRule.onAllNodesWithContentDescription("Pause").fetchSemanticsNodes().isNotEmpty()
        }
        val controlBounds = composeRule.onNodeWithContentDescription("Pause").fetchSemanticsNode().boundsInRoot
        val screenBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        assertTrue(controlBounds.center.y > screenBounds.top + screenBounds.height * 0.65f)
        composeRule.onNodeWithContentDescription("Pause").performClick()
        composeRule.onNodeWithContentDescription("Play").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 20000) {
            composeRule.onAllNodesWithContentDescription("Restart").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithContentDescription("Restart").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }
}
