package ai.spatialwalk.avatarkitdemo

import ai.spatialwalk.avatarkit.AvatarKit
import android.app.Application

class AvatarApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initAvatarKit()
    }

    private fun initAvatarKit() {
        AvatarKit.initialize(this, "", AvatarKit.Configuration("test"))
    }
}
