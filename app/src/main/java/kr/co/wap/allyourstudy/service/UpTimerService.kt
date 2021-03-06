package kr.co.wap.allyourstudy.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kr.co.wap.allyourstudy.R
import kr.co.wap.allyourstudy.TimerActivity
import kr.co.wap.allyourstudy.model.TimerEvent
import kr.co.wap.allyourstudy.utils.*

class UpTimerService: LifecycleService() {

    companion object {
        val timerEvent = MutableLiveData<TimerEvent>()
        val upTimer = MutableLiveData<Long>()
    }
    private lateinit var notificationManager: NotificationManagerCompat
    private var isServiceStopped = false

    private val wakelock = PowerManager.ACQUIRE_CAUSES_WAKEUP

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)

        val upTimerNotificationBuilder :NotificationCompat.Builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setColor(ContextCompat.getColor(baseContext, R.color.up_timer_green))
                .setGroup(ALL_YOUR_STUDY)
                .setSmallIcon(R.drawable.up_timer)
                .setContentTitle("스톱워치")
                .setContentText("00:00:00")

        startForeground(UP_TIMER_NOTIFICATION_ID, upTimerNotificationBuilder.build())

        upTimer.observe(this) {
            if (!isServiceStopped) {
                upTimerNotificationBuilder
                    .setContentIntent(getTimerActivityPendingIntent())
                    .setContentText(TimerUtil.getFormattedSecondTime(it, false)
                    )
                notificationManager.notify(UP_TIMER_NOTIFICATION_ID, upTimerNotificationBuilder.build())
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when (it.action) {
                ACTION_UP_TIMER_START -> {
                    startForegroundService(it.action!!,it.getLongExtra("data",-1))
                }
                ACTION_UP_TIMER_STOP -> {
                    stopService(false)
                }
                ACTION_UP_TIMER_PAUSE ->{
                   stopService(true)
                }
                else -> {}
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    private fun stopService(pause: Boolean){
        isServiceStopped = true
        timerEvent.postValue(TimerEvent.UpTimerStop)
        if(!pause) {
           upTimer.postValue(0L)
        }
        notificationManager.cancel(UP_TIMER_NOTIFICATION_ID)
        stopForeground(true)
        stopSelf()
    }
    private fun startForegroundService(action: String, data: Long) {
        timerEvent.postValue(TimerEvent.UpTimerStart)
        if(action == ACTION_UP_TIMER_START){
            startUpTimer(data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            )

        notificationManager.createNotificationChannel(channel)
    }
    private fun getTimerActivityPendingIntent() =
        PendingIntent.getActivity(
            this,
            420,
            Intent(this, TimerActivity::class.java).apply{
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    private fun startUpTimer(data: Long){
        var timeStarted = data * 1000
        CoroutineScope(Dispatchers.Main).launch{
            while(!isServiceStopped && timerEvent.value!! == TimerEvent.UpTimerStart){
                upTimer.postValue(timeStarted)
                timeStarted += 1000
                delay(997L)
            }
        }
    }
}