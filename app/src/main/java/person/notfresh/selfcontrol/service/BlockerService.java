package person.notfresh.selfcontrol.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import person.notfresh.selfcontrol.R;

/**
 * Blocker Service
 * Responsible for tracking blocking state and providing flag for AccessibilityService
 */
public class BlockerService extends Service {
    
    private static final String TAG = "BlockerService";
    private static final String CHANNEL_ID = "BlockerServiceChannel";
    private static final int NOTIFICATION_ID = 1;
    
    // SharedPreferences keys
    private static final String PREFS_NAME = "FocusTimeStats";
    private static final String KEY_SESSION_START_TIME = "current_session_start_time";
    private static final String KEY_SESSION_DURATION = "current_session_duration";
    private static final String KEY_TOTAL_TIME_TODAY = "total_focus_time_today";
    private static final String KEY_LAST_FOCUS_DURATION = "last_focus_duration";
    private static final String KEY_LAST_DATE = "last_date";
    
    // Static flag to track if service is running (for AccessibilityService to check)
    private static volatile boolean isServiceRunning = false;
    
    // Timer for updating focus time
    private Handler updateHandler;
    private Runnable updateRunnable;
    private long sessionStartTime = 0;
    
    /**
     * Check if BlockerService is running
     * This is a reliable way for AccessibilityService to check service status
     */
    public static boolean isRunning() {
        return isServiceRunning;
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "BlockerService onCreate called, process: " + android.os.Process.myPid());
        try {
            Log.d(TAG, "Creating notification channel...");
            createNotificationChannel();
            Log.d(TAG, "Notification channel created");
            
            // Initialize handler for timer updates
            updateHandler = new Handler(getMainLooper());
            
            Log.d(TAG, "BlockerService onCreate completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in BlockerService onCreate", e);
            throw e;
        }
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "BlockerService onStartCommand called, process: " + android.os.Process.myPid() + ", flags: " + flags + ", startId: " + startId);
        
        // Set service running flag - this is checked by AccessibilityService
        isServiceRunning = true;
        Log.d(TAG, "BlockerService is now RUNNING (flag set to true)");
        
        try {
            // Record session start time
            sessionStartTime = System.currentTimeMillis();
            saveSessionStartTime(sessionStartTime);
            Log.d(TAG, "Session start time recorded: " + sessionStartTime);
            
            // Check and reset daily total if date changed
            checkAndResetDailyTotal();
            
            // Start timer to update focus time every second
            startTimer();
            
            // Create foreground notification
            Log.d(TAG, "Creating foreground notification...");
            Notification notification = createNotification();
            Log.d(TAG, "Notification created successfully");
            
            // Start foreground service with proper type for Android 14+
            Log.d(TAG, "Starting foreground service, Android version: " + Build.VERSION.SDK_INT);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
                Log.d(TAG, "Foreground notification started with SPECIAL_USE type");
            } else {
                startForeground(NOTIFICATION_ID, notification);
                Log.d(TAG, "Foreground notification started");
            }
            
            Log.d(TAG, "BlockerService onStartCommand completed successfully");
            return START_STICKY;
        } catch (Exception e) {
            Log.e(TAG, "Critical error in BlockerService onStartCommand", e);
            e.printStackTrace();
            // Only stop service on critical errors
            stopSelf();
            return START_NOT_STICKY;
        }
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "BlockerService onDestroy called");
        
        // Stop timer
        stopTimer();
        
        // Calculate and save final session duration
        if (sessionStartTime > 0) {
            long endTime = System.currentTimeMillis();
            long sessionDuration = endTime - sessionStartTime;
            saveSessionEnd(sessionDuration);
            Log.d(TAG, "Session ended. Duration: " + sessionDuration + "ms (" + formatDuration(sessionDuration) + ")");
        }
        
        // Set service running flag to false - this is checked by AccessibilityService
        isServiceRunning = false;
        Log.d(TAG, "BlockerService is now STOPPED (flag set to false)");
        
        Log.d(TAG, "BlockerService destroyed");
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "防沉迷服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            serviceChannel.setDescription("防沉迷应用运行状态通知");
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }
    
    /**
     * 创建前台通知
     */
    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("防沉迷模式运行中")
                .setContentText("专注模式已启动，请勿中断")
                .setSmallIcon(R.drawable.ic_notification)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
    
    /**
     * Start timer to update focus time every second
     */
    private void startTimer() {
        if (updateHandler == null) {
            updateHandler = new Handler(getMainLooper());
        }
        
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (sessionStartTime > 0) {
                    long currentTime = System.currentTimeMillis();
                    long duration = currentTime - sessionStartTime;
                    updateSessionDuration(duration);
                    
                    // Schedule next update in 1 second
                    updateHandler.postDelayed(this, 1000);
                }
            }
        };
        
        updateHandler.post(updateRunnable);
        Log.d(TAG, "Timer started for focus time updates");
    }
    
    /**
     * Stop timer
     */
    private void stopTimer() {
        if (updateHandler != null && updateRunnable != null) {
            updateHandler.removeCallbacks(updateRunnable);
            updateRunnable = null;
            Log.d(TAG, "Timer stopped");
        }
    }
    
    /**
     * Save session start time to SharedPreferences
     */
    private void saveSessionStartTime(long startTime) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_SESSION_START_TIME, startTime)
                .putLong(KEY_SESSION_DURATION, 0)
                .apply();
    }
    
    /**
     * Update current session duration
     */
    private void updateSessionDuration(long duration) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_SESSION_DURATION, duration)
                .apply();
    }
    
    /**
     * Save session end and update daily total
     */
    private void saveSessionEnd(long duration) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        
        // Save last focus duration
        prefs.edit()
                .putLong(KEY_LAST_FOCUS_DURATION, duration)
                .putLong(KEY_SESSION_DURATION, duration)
                .apply();
        
        // Update daily total
        long totalToday = prefs.getLong(KEY_TOTAL_TIME_TODAY, 0);
        totalToday += duration;
        prefs.edit()
                .putLong(KEY_TOTAL_TIME_TODAY, totalToday)
                .putLong(KEY_SESSION_START_TIME, 0) // Clear start time
                .putLong(KEY_SESSION_DURATION, 0) // Clear duration
                .apply();
        
        Log.d(TAG, "Session end saved. Duration: " + duration + "ms, Total today: " + totalToday + "ms");
    }
    
    /**
     * Check if date changed and reset daily total if needed
     */
    private void checkAndResetDailyTotal() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lastDate = prefs.getString(KEY_LAST_DATE, "");
        String today = java.text.DateFormat.getDateInstance().format(new java.util.Date());
        
        if (!today.equals(lastDate)) {
            // Date changed, reset daily total
            prefs.edit()
                    .putString(KEY_LAST_DATE, today)
                    .putLong(KEY_TOTAL_TIME_TODAY, 0)
                    .apply();
            Log.d(TAG, "Date changed, daily total reset. Last date: " + lastDate + ", Today: " + today);
        }
    }
    
    /**
     * Format duration in milliseconds to readable string
     * @param durationMs Duration in milliseconds
     * @return Formatted string like "5分30秒" or "1小时25分"
     */
    public static String formatDuration(long durationMs) {
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        seconds = seconds % 60;
        minutes = minutes % 60;
        
        if (hours > 0) {
            return hours + "小时" + minutes + "分";
        } else if (minutes > 0) {
            return minutes + "分" + seconds + "秒";
        } else {
            return seconds + "秒";
        }
    }
    
    /**
     * Get current session duration from SharedPreferences
     * This can be called from MainActivity to display real-time duration
     */
    public static long getCurrentSessionDuration(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long startTime = prefs.getLong(KEY_SESSION_START_TIME, 0);
        
        if (startTime > 0) {
            long currentTime = System.currentTimeMillis();
            return currentTime - startTime;
        } else {
            // Return saved duration if session not active
            return prefs.getLong(KEY_SESSION_DURATION, 0);
        }
    }
    
    /**
     * Get total focus time today
     */
    public static long getTotalFocusTimeToday(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(KEY_TOTAL_TIME_TODAY, 0);
    }
    
    /**
     * Get last focus duration
     */
    public static long getLastFocusDuration(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getLong(KEY_LAST_FOCUS_DURATION, 0);
    }
}