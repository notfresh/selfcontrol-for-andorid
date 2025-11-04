package person.notfresh.selfcontrol.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
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
    
    // Static flag to track if service is running (for AccessibilityService to check)
    private static volatile boolean isServiceRunning = false;
    
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
}