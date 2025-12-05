package ai.spatialwalk.avatarkitdemo

import ai.spatialwalk.avatarkit.Avatar
import ai.spatialwalk.avatarkit.AvatarController
import ai.spatialwalk.avatarkit.AvatarController.ConnectionState
import ai.spatialwalk.avatarkit.AvatarKit
import ai.spatialwalk.avatarkit.AvatarView
import ai.spatialwalk.avatarkit.assets.AvatarManager
import ai.spatialwalk.avatarkit.player.AvatarPlayer.ConversationState
import ai.spatialwalk.avatarkitdemo.ui.theme.AvatarKitDemoTheme
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import kotlin.io.encoding.Base64

class AvatarActivity : ComponentActivity() {
    private var avatarView: AvatarView? = null

    private lateinit var avatar: Avatar

    private val avatarController: AvatarController
        get() = avatarView?.avatarController ?: error("Do not have a controller, have you called init?")

    val avatarId: String by lazy {
        intent.getStringExtra(EXTRA_AVATAR_ID) ?: error("Avatar ID is required")
    }

    private var isLoading by mutableStateOf(true)

    private var connectionState: ConnectionState by mutableStateOf(ConnectionState.Disconnected)
    private var conversationState: ConversationState by mutableStateOf(ConversationState.Idle)
    private var errorState: Throwable? by mutableStateOf(null)
    private var extraMessage: String by mutableStateOf("")

    private var pcmSendingJob: Job? = null
        set(value) {
            field?.cancel()
            field = value
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AvatarKitDemoTheme {
                LaunchedEffect(avatarId) {
                    avatar = AvatarManager.load(avatarId)
                    isLoading = false
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else {
                        AvatarScreen(
                            connectionState = connectionState,
                            conversationState = conversationState,
                            errorState = errorState,
                            extraMessage,
                            onAvatarViewCreated = {
                                avatarView = it.apply { init(avatar, lifecycleScope) }
                                setupController()
                            },
                            onConnectClick = { toConnect ->
                                if (toConnect) {
                                    avatarController.start()
                                } else {
                                    avatarController.stop()
                                }
                            },
                            onInterruptClick = {
                                avatarController.interrupt()
                            },
                            onFileClick = { file ->
                                if (AvatarKit.isSdkDriven()) {
                                    sendPcm(file)
                                } else {
                                    sendJson(file)
                                }
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        avatarView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        avatarView?.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        avatarView?.cleanup()
    }

    private fun setupController() {
        avatarController.onConversationState = { state ->
            conversationState = state
        }
        avatarController.onConnectionState = { state ->
            connectionState = state
        }
        avatarController.onError = { error ->
            errorState = error
        }
    }

    private fun sendPcm(filePath: String) {
        pcmSendingJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                avatarController.interrupt()
                val audioDataStream = assets.open(filePath)
                while (isActive) {
                    val seconds = 1
                    val bufferSize = 16000 * 2 * seconds
                    val audioData = audioDataStream.readNBytes(bufferSize)
                    avatarController.send(
                        audioData,
                        audioData.size < bufferSize
                    )
                    delay((100 * seconds).toLong())
                    if (audioData.size < bufferSize) {
                        break
                    }
                }
            } catch (e: CancellationException) {
                extraMessage = "PCM sending interrupted."
                throw e
            } catch (e: Exception) {
                errorState = e
            }
        }
    }

    private fun sendJson(filePath: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            val ana = Json.decodeFromStream<AudioAndAnimation>(assets.open(filePath))
            val reqId = avatarController.send(Base64.decode(ana.audio), true)
            avatarController.receiveAnimations(ana.animations.map(Base64::decode), reqId)
        }
    }

    companion object {
        const val EXTRA_AVATAR_ID = "avatar_id"
    }
}

@Composable
fun AvatarScreen(
    connectionState: ConnectionState,
    conversationState: ConversationState,
    errorState: Throwable?,
    extraMessage: String,
    onAvatarViewCreated: (AvatarView) -> Unit,
    onConnectClick: (toConnect: Boolean) -> Unit,
    onInterruptClick: () -> Unit,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

    val files = remember {
        val folder = if (AvatarKit.isSdkDriven()) "pcm" else "json"
        context.assets
            .list(folder)
            ?.toList()
            .orEmpty()
            .map { "$folder/$it" }
    }

    val avatarViewComposable: @Composable (Modifier) -> Unit = { mod ->
        AndroidView(
            factory = { ctx ->
                AvatarView(ctx).also { onAvatarViewCreated(it) }
            },
            modifier = mod
        )
    }

    val controlPanelComposable: @Composable (Modifier) -> Unit = { mod ->
        var selectedTabIndex by remember { mutableIntStateOf(0) }
        val tabs = listOf("Main", "Audio")

        Column(
            modifier = mod
                .background(MaterialTheme.colorScheme.surface)
        ) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTabIndex) {
                0 -> ControlTab(
                    connectionState = connectionState,
                    conversationState = conversationState,
                    errorState = errorState,
                    extraMessage = extraMessage,
                    onConnectClick = onConnectClick,
                    onInterruptClick = onInterruptClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
                1 -> AudioTab(
                    fileList = files,
                    onFileClick = onFileClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                )
            }
        }
    }

    if (isLandscape) {
        Row(modifier = modifier.fillMaxSize()) {
            avatarViewComposable(
                Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f)
            )
            controlPanelComposable(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            avatarViewComposable(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            controlPanelComposable(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
fun ControlTab(
    connectionState: ConnectionState,
    conversationState: ConversationState,
    errorState: Throwable?,
    extraMessage: String,
    onConnectClick: (toConnect: Boolean) -> Unit,
    onInterruptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val clickToConnect = connectionState != ConnectionState.Connected
            Button(
                onClick = { onConnectClick(clickToConnect) },
                enabled = connectionState != ConnectionState.Connecting && AvatarKit.isSdkDriven(),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (clickToConnect) "Connect" else "Disconnect")
            }
            OutlinedButton(
                onClick = onInterruptClick,
                enabled = conversationState == ConversationState.Playing,
                modifier = Modifier.weight(1f)
            ) {
                Text("Interrupt")
            }
        }

        StateInformation(connectionState, conversationState, errorState, extraMessage)
    }
}

@Composable
fun StateInformation(
    connectionState: ConnectionState,
    conversationState: ConversationState,
    errorState: Throwable?,
    extraMessage: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        if (AvatarKit.isSdkDriven()) {
            Text(
                text = "Connection: $connectionState",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = "Conversation: $conversationState",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 8.dp)
        )
        if (extraMessage.isNotEmpty()) {
            Text(
                text = "Message: $extraMessage",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        if (errorState != null) {
            Text(
                text = "Error: ${errorState.message}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun AudioTab(
    fileList: List<String>,
    onFileClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 16.dp)
        ) {
            items(fileList) { fileName ->
                ListItem(
                    headlineContent = { Text(File(fileName).name) },
                    modifier = Modifier.clickable { onFileClick(fileName) }
                )
            }
        }
    }
}

private fun AvatarKit.isSdkDriven(): Boolean {
    return config.drivingServiceMode == AvatarKit.DrivingServiceMode.SDK
}

@Serializable
data class AudioAndAnimation(
    val audio: String,
    val animations: List<String>,
)