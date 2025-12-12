package ai.spatialwalk.avatarkitdemo

import ai.spatialwalk.avatarkit.AvatarSDK
import ai.spatialwalk.avatarkit.Configuration
import ai.spatialwalk.avatarkit.DrivingServiceMode
import ai.spatialwalk.avatarkit.Environment
import ai.spatialwalk.avatarkit.LogLevel
import android.app.Application

class AvatarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAvatarKit()
    }

    private fun initAvatarKit() {
        AvatarSDK.initialize(this, "", Configuration(Environment.valueOf("test"), DrivingServiceMode.SDK, LogLevel.ERROR))
    }
}
