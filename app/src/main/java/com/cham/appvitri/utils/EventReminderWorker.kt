package com.cham.appvitri.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.cham.appvitri.R // QUAN TRỌNG: Đảm bảo R này được import đúng từ package của bạn
import com.cham.appvitri.repository.Event
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ===== PHẦN 1: WORKER - ĐÂY LÀ "CÔNG NHÂN" SẼ HIỂN THỊ THÔNG BÁO =====
class EventReminderWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        // Lấy dữ liệu đã được truyền vào khi đặt lịch
        val eventTitle = inputData.getString(KEY_EVENT_TITLE) ?: return Result.failure()
        val eventId = inputData.getString(KEY_EVENT_ID) ?: return Result.failure()

        Log.d("EventReminderWorker", "Worker is running for event: $eventTitle (ID: $eventId)")
        showNotification(eventTitle, eventId)

        return Result.success()
    }

    private fun showNotification(title: String, id: String) {
        val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "event_reminder_channel"

        // Tạo Notification Channel (bắt buộc từ Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Nhắc nhở sự kiện",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Thông báo cho các sự kiện sắp diễn ra"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Xây dựng thông báo
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.logo) // Thay R.drawable.logo bằng icon của bạn
            .setContentTitle("Sự kiện sắp diễn ra!")
            .setContentText("Sự kiện '$title' sẽ bắt đầu trong ít phút nữa.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        
        // Hiển thị thông báo, dùng hashCode của ID để mỗi thông báo là duy nhất
        notificationManager.notify(id.hashCode(), notification)
    }

    // Dùng companion object để định nghĩa các key, tránh gõ sai chuỗi
    companion object {
        const val KEY_EVENT_TITLE = "EVENT_TITLE"
        const val KEY_EVENT_ID = "EVENT_ID"
    }
}


// ===== PHẦN 2: HÀM TIỆN ÍCH - ĐÂY LÀ HÀM MÀ VIEWMODEL SẼ GỌI =====
fun scheduleEventReminder(context: Context, event: Event) {
    val now = System.currentTimeMillis()
    val eventTime = event.eventTimestamp.toDate().time
    
    // Đặt lịch thông báo trước 5 phút
    val notificationTime = eventTime - TimeUnit.MINUTES.toMillis(5)
    //val notificationTime = eventTime - TimeUnit.SECONDS.toMillis(30)

    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    Log.d("EventScheduler", "---------------------------------")
    Log.d("EventScheduler", "Scheduling for event: ${event.title}")
    Log.d("EventScheduler", "Current Time:     ${sdf.format(Date(now))}")
    Log.d("EventScheduler", "Event Time:       ${sdf.format(Date(eventTime))}")
    Log.d("EventScheduler", "Notification Time:  ${sdf.format(Date(notificationTime))}")

    // Chỉ đặt lịch nếu thời gian thông báo vẫn còn trong tương lai
    if (notificationTime > now) {
        val delay = notificationTime - now
        Log.d("EventScheduler", "--> Scheduling with delay: ${delay / 1000} seconds.")
        // Đóng gói dữ liệu cần thiết để gửi cho Worker
        val data = Data.Builder()
            .putString(EventReminderWorker.KEY_EVENT_TITLE, event.title)
            .putString(EventReminderWorker.KEY_EVENT_ID, event.id)
            .build()
        
        // Tạo yêu cầu công việc chạy một lần
        val reminderRequest = OneTimeWorkRequestBuilder<EventReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("event_reminder") // Tag chung để dễ quản lý/hủy
            .build()

        // Dùng tên duy nhất để đảm bảo mỗi sự kiện chỉ có 1 lịch
        val uniqueWorkName = "reminder_for_${event.id}"

        // Gửi yêu cầu cho WorkManager
        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                uniqueWorkName,
                ExistingWorkPolicy.REPLACE, // Nếu đã có lịch -> thay bằng lịch mới. Rất quan trọng khi sửa sự kiện.
                reminderRequest
            )
        
        Log.d("EventScheduler", "Scheduled reminder for '${event.title}' (ID: ${event.id}) to run in ${delay / 1000} seconds.")
    } else {
        // Nếu sự kiện đã quá gần hoặc đã qua, hủy lịch cũ nếu có để dọn dẹp
        val uniqueWorkName = "reminder_for_${event.id}"
        WorkManager.getInstance(context).cancelUniqueWork(uniqueWorkName)
        Log.d("EventScheduler", "Event '${event.title}' (ID: ${event.id}) is past or too soon. Canceled any existing reminder.")
    }
}