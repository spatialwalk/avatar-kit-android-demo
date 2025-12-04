package ai.spatialwalk.avatarkitdemo

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
    var selectedAvatar by remember { mutableStateOf(allAvatars.first()) }
    val context = LocalContext.current

    val launchAvatarActivity = {
        val intent = Intent(context, AvatarActivity::class.java).apply {
            putExtra(AvatarActivity.EXTRA_AVATAR_ID, selectedAvatar.id)
        }
        context.startActivity(intent)
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
            items(allAvatars) { avatar ->
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
                onClick = launchAvatarActivity,
                modifier = Modifier.weight(1f)
            ) {
                Text("SDK Driven")
            }
            OutlinedButton(
                onClick = launchAvatarActivity,
                modifier = Modifier.weight(1f)
            ) {
                Text("Host Driven")
            }
        }
    }
}

data class AvatarInfo(
    val id: String,
    val name: String,
)

private val allAvatars = listOf(
    AvatarInfo("b7ba14f6-f9aa-4f89-9934-3753d75aee39", "George"),
    AvatarInfo("8f2c10a1-e47d-465d-9dea-9a34a1361d0e", "Lisa"),
    AvatarInfo("d5f39c85-8613-42db-820a-dc0d59157e06", "Seraphina"),
    AvatarInfo("ded56165-f3f0-404d-ab32-aed4aea48e27", "Musk"),
    AvatarInfo("76f0f477-4ce8-4fba-b4a6-b1d7f70434c2", "Xiaomei"),
    AvatarInfo("97a8a20c-b82a-44d2-9d4e-19b06748b733", "Queen"),
    AvatarInfo("73e08cb6-c56b-427c-9842-06dd5532fa13", "Mona Lisa"),
)