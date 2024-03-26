@file:OptIn(ExperimentalFoundationApi::class)

package com.example.socialvidsdkassignment

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
                    if (isInternetAvailable()) {
                        Swipe(apiState = apiState)
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No Internet",
                                fontSize = 24.sp
                            )
                        }
                    }
                }
                }
            }
        }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                    )
        } else {
            val activeNetworkInfo = connectivityManager.activeNetworkInfo
            return activeNetworkInfo != null && activeNetworkInfo.isConnected
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
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ApiState.Success -> {
                    // Display the videos fetched from the API
                    if (msgList != null && it < msgList.size) {
                        VideoPlayer(msg = msgList[it])
                    }

                }

                is ApiState.Failure -> {
                    // Display error message
                    val errorMessage = apiState.msg.message!!
                    
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }

                is ApiState.Empty -> {
                    // Display empty state
                    Toast.makeText(context, "is empty", Toast.LENGTH_SHORT).show()
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
        var isPreparing by remember { mutableStateOf(true) } // State to track if the video is preparing


        DisposableEffect(Unit) {
            playerView.player = player

            val videoUrl = msg.video
            val uri = videoUrl.toUri()
            val mediaItem = MediaItem.fromUri(uri)

            player.setMediaItem(mediaItem)

            player.addListener(object : Player.Listener {
                override fun onIsLoadingChanged(isLoading: Boolean) {
                    super.onIsLoadingChanged(isLoading)
                    isPreparing = isLoading // Update the state when the loading status changes
                }
            })

            player.prepare()
            player.play()

            onDispose {
                player.release()
            }
        }

        Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
            if (isPreparing) {
                CircularProgressIndicator() // Display the progress indicator while preparing
            } else {
                PlayerViewComponent(playerView = playerView)
            }
        }
    }
}


@Composable
fun PlayerViewComponent(playerView: PlayerView) {
    AndroidView(factory = { playerView }, modifier = Modifier.fillMaxSize())
}