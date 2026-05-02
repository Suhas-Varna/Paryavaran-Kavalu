package com.example.paryavaran_kavalu

import android.app.Application
import com.example.paryavaran_kavalu.data.AppDatabase
import com.example.paryavaran_kavalu.data.UserEntity
import com.example.paryavaran_kavalu.data.UserTypes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import java.io.File

class ParyavaranApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getInstance(this) }

    override fun onCreate() {
        super.onCreate()

        initOsmdroidPaths()

        applicationScope.launch {
            val userDao = database.userDao()
            if (userDao.getUser() == null) {
                userDao.insert(
                    UserEntity(
                        userId = 1,
                        nickname = "Eco Warrior",
                        userType = UserTypes.REPORTER,
                        bio = "",
                        ecoPoints = 0,
                    ),
                )
            }
        }
    }

    /**
     * OSMDroid crashes or hangs if tile/cache dirs are unset — especially on physical devices /
     * wireless debugging where timing differs from emulators.
     */
    private fun initOsmdroidPaths() {
        val cfg = Configuration.getInstance()
        cfg.load(this, getSharedPreferences("osm", MODE_PRIVATE))
        cfg.userAgentValue = packageName

        val base = File(cacheDir, "osmdroid").apply { mkdirs() }
        val tiles = File(base, "tiles").apply { mkdirs() }
        cfg.osmdroidBasePath = base
        cfg.osmdroidTileCache = tiles
    }
}
