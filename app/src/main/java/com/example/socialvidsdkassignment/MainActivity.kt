@file:OptIn(ExperimentalFoundationApi::class)

package com.example.socialvidsdkassignment

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.example.socialvidsdkassignment.model.Msg
import com.example.socialvidsdkassignment.ui.theme.SocialVidSdkAssignmentTheme
import com.example.socialvidsdkassignment.viewmodel.VideoViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val videoViewModel: VideoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SocialVidSdkAssignmentTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val apiState by videoViewModel.response.collectAsState()
                    Swipe(apiState = apiState)
                }
            }
        }
    }
}

@Composable
fun Swipe(apiState: ApiState) {
    val msgList = when (apiState) {
        is ApiState.Success -> apiState.data
        else -> null
    }

    val pagerState = rememberPagerState {
        msgList?.size ?: 0
    }


    Box(modifier = Modifier.fillMaxSize()) {
        val context = LocalContext.current
        VerticalPager(state = pagerState) {
            when (apiState) {
                is ApiState.Loading -> {
                    // Display loading indicator
                   CircularProgressIndicator()
                }

                is ApiState.Success -> {
                    // Display the videos fetched from the API
                    if (msgList != null && it < msgList.size) {
                        VideoPlayer(msg = msgList[it])
                    }

                }

                is ApiState.Failure -> {
                    // Display error message
                    Toast.makeText(context,"Something Went Wrong",Toast.LENGTH_SHORT).show()
                }

                is ApiState.Empty -> {
                    // Display empty state
                    Toast.makeText(context,"is empty",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(msg: Msg?) {
    if (msg != null) {
        val context = LocalContext.current
        val player = remember(context) {
            ExoPlayer.Builder(context).build()
        }

        val playerView = remember { PlayerView(context) }

        DisposableEffect(Unit) {
            playerView.player = player

            val videoUrl = msg.video
            val uri = videoUrl.toUri()
            val mediaItem = MediaItem.fromUri(uri)


            player.setMediaItem(mediaItem)
            player.prepare()
            player.play()
            player.playWhenReady = true

            onDispose {
                player.release()
            }
        }

        PlayerViewComponent(playerView = playerView)
    }

}

@Composable
fun PlayerViewComponent(playerView: PlayerView) {
    AndroidView(factory = { playerView })
}