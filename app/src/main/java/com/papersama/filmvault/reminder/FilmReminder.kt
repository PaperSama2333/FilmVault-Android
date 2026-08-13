package com.papersama.filmvault.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.WorkManager
import com.papersama.filmvault.MainActivity
import com.papersama.filmvault.R
import java.util.concurrent.TimeUnit

object ReminderPreferences {
    private const val FILE = "filmvault_reminders"
    private const val ENABLED = "enabled"
    private const val DELAY_DAYS = "delay_days"

    fun isEnabled(context: Context): Boolean = preferences(context).getBoolean(ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        preferences(context).edit().putBoolean(ENABLED, enabled).apply()
        if (!enabled) FilmReminder.cancelAll(context)
    }

    fun delayDays(context: Context): Int = preferences(context).getInt(DELAY_DAYS, 3)

    fun setDelayDays(context: Context, days: Int) {
        preferences(context).edit().putInt(DELAY_DAYS, days.coerceIn(1, 30)).apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
}

object FilmReminder {
    const val CHANNEL_ID = "developing_reminders"
    private const val WORK_TAG = "filmvault_developing_reminders"

    fun createChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "冲洗提醒",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "提醒已拍完的胶卷及时冲洗、扫描"
            },
        )
    }

    fun canNotify(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun schedule(context: Context, rollId: String, rollName: String) {
        if (!ReminderPreferences.isEnabled(context)) return
        val data = Data.Builder()
            .putString(DevelopmentReminderWorker.ROLL_ID, rollId)
            .putString(DevelopmentReminderWorker.ROLL_NAME, rollName)
            .build()
        val request = OneTimeWorkRequestBuilder<DevelopmentReminderWorker>()
            .setInitialDelay(ReminderPreferences.delayDays(context).toLong(), TimeUnit.DAYS)
            .setInputData(data)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueName(rollId),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancel(context: Context, rollId: String) {
        WorkManager.getInstance(context).cancelUniqueWork(uniqueName(rollId))
    }

    fun cancelAll(context: Context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(WORK_TAG)
    }

    fun sendTest(context: Context): Boolean = post(
        context = context,
        notificationId = 1001,
        title = "胶片匣提醒已开启",
        text = "拍完胶卷后，我会按你选择的时间提醒冲洗。",
    )

    @SuppressLint("MissingPermission")
    fun post(context: Context, notificationId: Int, title: String, text: String): Boolean {
        if (!canNotify(context)) return false
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(notificationId, notification)
        return true
    }

    private fun uniqueName(rollId: String) = "filmvault_developing_$rollId"
}

class DevelopmentReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : Worker(appContext, params) {
    override fun doWork(): Result {
        if (!ReminderPreferences.isEnabled(applicationContext)) return Result.success()
        val rollId = inputData.getString(ROLL_ID).orEmpty()
        val rollName = inputData.getString(ROLL_NAME).orEmpty().ifBlank { "这卷胶片" }
        FilmReminder.post(
            context = applicationContext,
            notificationId = rollId.hashCode(),
            title = "记得冲洗 $rollName",
            text = "胶片已经拍完一段时间了，别让影像在暗盒里等太久。",
        )
        return Result.success()
    }

    companion object {
        const val ROLL_ID = "roll_id"
        const val ROLL_NAME = "roll_name"
    }
}
