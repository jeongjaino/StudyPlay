package kr.co.wap.allyourstudy.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.CountDownTimer
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
import kotlin.concurrent.thread
import kotlin.concurrent.timer

class DownTimerService: LifecycleService() {

    companion object{
        val timerEvent = MutableLiveData<TimerEvent>()
        val downTimer = MutableLiveData<Long>()
        val timerMax = MutableLiveData<Int>() //progressbar max값
    }

    private lateinit var notificationManager: NotificationManagerCompat
    private var isServiceStopped = false

    override fun onCreate() {
        super.onCreate()
        notificationManager = NotificationManagerCompat.from(this)

        val downTimerNotificationBuilder : NotificationCompat.Builder =
            NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setGroup(ALL_YOUR_STUDY)
                .setSmallIcon(R.drawable.down_timer)
                .setColor(ContextCompat.getColor(baseContext, R.color.down_timer_yellow))
                .setContentTitle("타이머")
                .setContentText("00:00:00")

        startForeground(DOWN_TIMER_NOTIFICATION_ID, downTimerNotificationBuilder.build())

        downTimer.observe(this) {
            if (!isServiceStopped) {
                downTimerNotificationBuilder
                    .setContentIntent(getTimerActivityPendingIntent())
                    .setContentText(TimerUtil.getFormattedSecondTime(it, false))
                notificationManager.notify(DOWN_TIMER_NOTIFICATION_ID, downTimerNotificationBuilder.build())
            }
        }

    }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent.let{
            when(it?.action){
                ACTION_DOWN_TIMER_START ->{
                    if(timerMax.value == null || timerMax.value == 0){
                        timerMax.postValue(it.getLongExtra("data",-1).toInt())
                    }
                    startForegroundService(it.action!!,it.getLongExtra("data", -1))
                }
                ACTION_DOWN_TIMER_STOP ->{
                    Log.d("tag", "stopService")
                    stopService(false)
                }
                ACTION_DOWN_TIMER_PAUSE ->{
                    stopService(true)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }
    private fun stopService(pause: Boolean){
        isServiceStopped = true
        if(!pause) {
            downTimer.postValue(0L)
            timerMax.postValue(0)
        }
        timerEvent.postValue(TimerEvent.DownTimerStop)
        notificationManager.cancel(DOWN_TIMER_NOTIFICATION_ID)
        stopForeground(true)
        stopSelf()
    }
    private fun startForegroundService(action: String, data: Long) {
        timerEvent.postValue(TimerEvent.DownTimerStart)
        if(action == ACTION_DOWN_TIMER_START){
            startDownTimer(data)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(){
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
            422,
            Intent(this, TimerActivity::class.java).apply{
                this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            },
           PendingIntent.FLAG_UPDATE_CURRENT
        )
    private fun startDownTimer(data: Long){
        var starting = data * 1000
        CoroutineScope(Dispatchers.Main).launch {
            while(!isServiceStopped && timerEvent.value!! == TimerEvent.DownTimerStart){
                downTimer.postValue(starting)
                if(starting == 0L){
                    delay(100) // 누적시간이 따라오는 시간
                    stopService(false)
                }
                starting -= 1000
                delay(997L)
            }
        }
    }
}