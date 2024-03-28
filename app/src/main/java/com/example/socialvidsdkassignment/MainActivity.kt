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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.rememberAsyncImagePainter
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
        VerticalPager(state = pagerState, key = {
            msgList?.get(it)?._id ?: -1                 // after using key video overlapping problem solved
        }) {index ->
            when (apiState) {
                is ApiState.Loading -> {
                    // Display loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                is ApiState.Success -> {
                    // Display the videos fetched from the API
                    if (msgList != null && index < msgList.size && index == pagerState.currentPage ) {
                        VideoPlayer(msg = msgList[index],pagerState = pagerState)
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


@androidx.annotation.OptIn(UnstableApi::class) @Composable
fun VideoPlayer(msg: Msg?,pagerState: PagerState) {
    Column(modifier = Modifier.fillMaxSize()) {
      val lifecycleOwner = LocalLifecycleOwner.current
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

                // Clear existing media items
                player.clearMediaItems()

                player.setMediaItem(mediaItem)
                playerView.useController = false // hide controller

                player.addListener(object : Player.Listener {
                    override fun onIsLoadingChanged(isLoading: Boolean) {
                        super.onIsLoadingChanged(isLoading)
                        isPreparing = isLoading // Update the state when the loading status changes
                    }

                    override fun onPlaybackStateChanged(state: Int) {
                        super.onPlaybackStateChanged(state)
                        if (state == Player.STATE_READY && pagerState.currentPage == pagerState.targetPage) {
                            // Start playback when the media is ready and video is visible
                            player.playWhenReady = true
                        }
                    }
                })
                val observer = LifecycleEventObserver { _, event ->  // to stop exoplayer playing in background
                    if (event == Lifecycle.Event.ON_PAUSE) {
                       player.pause()
                    } else if (event == Lifecycle.Event.ON_STOP) {
                        player.stop()
                    }
                }

                // Add the observer to the lifecycle
                lifecycleOwner.lifecycle.addObserver(observer)

                player.prepare()
                player.repeatMode = Player.REPEAT_MODE_ONE // loop video until scroll
                playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM // to make video full screen

                onDispose {
                    player.release()
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            Box(modifier = Modifier.fillMaxSize(), Alignment.Center) {
                if (isPreparing) {
                    CircularProgressIndicator() //progress indicator while preparing
                } else {
                    PlayerViewComponent(playerView = playerView)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        CreatorInfo(msg = msg,context = context)
                    }
                }
            }
        }
    }
}


@Composable
fun PlayerViewComponent(playerView: PlayerView) {
    AndroidView(factory = { playerView },
        modifier = Modifier.fillMaxSize())
}

@Composable
fun CreatorInfo(msg: Msg?,context: Context) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // creator's photo
            Image(
            painter = rememberAsyncImagePainter(
                model = msg?.user_info?.profile_pic,
                imageLoader = ImageLoader.Builder(context).crossfade(true).build()
            ),
            contentDescription = "Profile Pic", modifier = Modifier
                .clip(CircleShape).size(15.dp)
        )


            // creator's username
            Text(
                text = msg?.user_info?.username ?: "",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

        }
        // Creator name
        msg?.user_info?.let {
            Text(
                text = it.first_name,
                fontSize = 16.sp,
                color = Color.White
            )
        }
        // caption
        Text(
            text = msg?.description ?: "",
            fontSize = 14.sp,
            color = Color.Gray
        )

    }
}

