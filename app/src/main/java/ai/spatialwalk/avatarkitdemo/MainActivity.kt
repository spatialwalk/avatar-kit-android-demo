package ai.spatialwalk.avatarkitdemo

import ai.spatialwalk.avatarkit.AudioFormat
import ai.spatialwalk.avatarkit.AvatarSDK
import ai.spatialwalk.avatarkit.Configuration
import ai.spatialwalk.avatarkit.DrivingServiceMode
import ai.spatialwalk.avatarkit.Environment
import ai.spatialwalk.avatarkit.LogLevel
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ai.spatialwalk.avatarkitdemo.ui.theme.AvatarKitDemoTheme
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AvatarKitDemoTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedAvatar by remember { mutableStateOf(testAvatars.first()) }
    val coroutineScope = rememberCoroutineScope()

    val launchAvatarActivity: (Boolean) -> Unit = { isSdkDriven ->
        coroutineScope.launch {
            val drivingMode = if (isSdkDriven) DrivingServiceMode.SDK else DrivingServiceMode.HOST
            // You should only initialize the AvatarSDK once, and call it in Application.onCreate()
            AvatarSDK.initialize(
                context,
                "", // Set your app id here
                Configuration(
                    Environment.cn, // Environment.cn or Environment.intl
                    AudioFormat(16000),
                    drivingServiceMode =  drivingMode,
                    logLevel =  LogLevel.ALL
                )
            )
            AvatarSDK.sessionToken = fetchToken()
            val intent = Intent(context, AvatarActivity::class.java).apply {
                putExtra(AvatarActivity.EXTRA_AVATAR_ID, selectedAvatar.id)
            }
            context.startActivity(intent)
        }
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Text(
            text = "Select Avatar",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(testAvatars) { avatar ->
                ListItem(
                    headlineContent = { Text(avatar.name) },
                    leadingContent = {
                        RadioButton(
                            selected = selectedAvatar == avatar,
                            onClick = { selectedAvatar = avatar }
                        )
                    },
                    modifier = Modifier.clickable { selectedAvatar = avatar }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    launchAvatarActivity(true)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("SDK Driven")
            }
            OutlinedButton(
                onClick = {
                    launchAvatarActivity(false)
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Host Driven")
            }
        }
    }
}

private suspend fun fetchToken(): String {
    return "" // Fetch token here
}

data class AvatarInfo(
    val id: String,
    val name: String,
)

// @See: https://docs.spatialreal.ai/overview/test-avatars
val testAvatars = listOf(
    AvatarInfo("93dd60f8-d9e2-47cf-973e-d75e10cfc951", "Rohan"),
    AvatarInfo("17a9aed2-4b35-4eb8-8bf2-675a278bd80d", "Dr.Kellan"),
    AvatarInfo("2a5170ff-8d1f-4d10-ac50-0ab4893df328", "Priya"),
    AvatarInfo("ab7117a9-f954-44df-8c25-06d28e4f6ec7", "Josh"),
)