package com.sagor.myreminder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.sagor.myreminder.data.AppDb
import com.sagor.myreminder.notifications.NotificationHelper
import com.sagor.myreminder.scheduling.AlarmScheduler
import com.sagor.myreminder.ui.HomeScreen
import com.sagor.myreminder.ui.MyTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        NotificationHelper.createChannels(this)
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        // Safety net: make sure every active reminder has its alarm.
        val app = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            AppDb.get(app).dao().active().forEach { AlarmScheduler.schedule(app, it) }
        }
        setContent { MyTheme { HomeScreen(tick) } }
    }

    override fun onResume() {
        super.onResume()
        tick++
    }
}
