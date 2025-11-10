package person.notfresh.selfcontrol.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.app.ActivityManager;
import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;
import android.util.Log;

/**
 * 无障碍服务
 * 持续监控正在运行的应用，检测并关闭被屏蔽的应用
 */
public class BlockerAccessibilityService extends AccessibilityService {
    
    private static final String TAG = "BlockerAccessibilityService";
    
    // 需要屏蔽的应用包名列表
    private static final String[] BLOCKED_PACKAGES = {
            "com.tencent.mm",           // 微信
            "com.tencent.mobileqq",      // QQ
            "com.sina.weibo",           // 微博
            "com.zhihu.android",         // 知乎
            "com.ss.android.ugc.aweme",  // 抖音
            "com.tencent.news",          // 腾讯新闻
            "com.netease.newsreader.activity", // 网易新闻
            "com.jingdong.app.mall",    // 京东
            "com.taobao.taobao",        // 淘宝
            "com.sankuai.meituan",      // 美团
            "com.xingin.xhs"            // 小红书
    };
    
    // Self package name (cached for performance)
    private String selfPackageName = null;
    
    // Handler for periodic monitoring
    private android.os.Handler monitorHandler = null;
    private Runnable monitorRunnable = null;
    
    // 共享的 Handler 用于后台进程处理（避免重复创建）
    private android.os.Handler killProcessHandler = null;
    
    // 记录正在等待杀死的进程（避免重复调用）
    private final java.util.Set<String> pendingKillProcesses = new java.util.HashSet<>();
    
    // Monitoring interval (check every 500ms as backup)
    // NOTE: Primary detection is via AccessibilityEvent, this is just a backup
    // IMPORTANT: Even if app process is sleeping, this monitoring continues running
    private static final long MONITOR_INTERVAL = 200; // Reduced from 500ms for faster detection
    
    // Track if we just performed a home action to ignore subsequent events
    // But ONLY for non-blocked apps - blocked apps must ALWAYS be blocked!
    private boolean justReturnedHome = false;
    private long homeActionTime = 0;
    private static final long HOME_ACTION_IGNORE_PERIOD = 200; // Ignore events for 200ms after returning home (reduced from 300ms)
    
    // 去重机制：避免短时间内重复处理同一应用
    private String lastProcessedPackage = null;
    private long lastProcessedTime = 0;
    private static final long DEBOUNCE_PERIOD = 500; // 500ms内同一包名只处理一次
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "AccessibilityService created");
        // Cache self package name for performance
        selfPackageName = getPackageName();
        Log.d(TAG, "Self package name: " + selfPackageName);
        Log.d(TAG, "Blocked packages: " + java.util.Arrays.toString(BLOCKED_PACKAGES));
        
        // 初始化共享 Handler
        killProcessHandler = new android.os.Handler(getMainLooper());
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "========== AccessibilityService CONNECTED ==========");
        Log.d(TAG, "Service connected at: " + System.currentTimeMillis());

        // Configure service info
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 500;
        info.flags = AccessibilityServiceInfo.DEFAULT | AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        this.setServiceInfo(info);
        Log.d(TAG, "AccessibilityService configured with eventTypes: " + info.eventTypes);
        Log.d(TAG, "Waiting for TYPE_WINDOW_STATE_CHANGED events...");

        // Start continuous monitoring
        // startMonitoring();
        Log.d(TAG, "========== AccessibilityService ready ==========");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        int eventType = event.getEventType();
        String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
        
        // 添加日志确认事件是否到达
        Log.d(TAG, "onAccessibilityEvent: type=" + eventType + ", package=" + packageName);
        
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            handleWindowStateChange(event);
        } else {
            if (!packageName.isEmpty() && isBlockedApp(packageName) && isBlockerServiceRunning()) {
                Log.w(TAG, "WARNING: Blocked app detected in non-WINDOW_STATE_CHANGED event! " +
                      "EventType: " + eventType + ", Package: " + packageName);
                // Still try to block it
                handleWindowStateChange(event);
            }
        }
    }
    
    /**
     * Handle window state change event - this is the PRIMARY detection method
     * When an app's window opens, we immediately check if it's blocked
     */
    private void handleWindowStateChange(AccessibilityEvent event) {
        // Get package name and class name
            String packageName = event.getPackageName() != null ? 
                    event.getPackageName().toString() : "";
            String className = event.getClassName() != null ? 
                    event.getClassName().toString() : "";
            
        if (packageName.equals("com.miui.home") ||
                packageName.equals("com.android.launcher") ||
                packageName.equals("com.android.launcher3")) {
            Log.d(TAG, "Launcher/home screen detected, skipping: " + packageName);
            return;
        }
        // Skip system UI windows (input method, recents screen, etc.)
        if (isSystemUIWindow(packageName, className)) {
            Log.d(TAG, "System UI window detected, skipping: " + packageName);
            return;
        }
        if (packageName.isEmpty()) {
            Log.d(TAG, "Package name is empty, skipping");
            return;
        }
        // CRITICAL: Ensure self package name is set
        if (selfPackageName == null) {
            selfPackageName = getPackageName();
            Log.w(TAG, "WARNING: selfPackageName was null, re-initialized to: " + selfPackageName);
        }
        
        if (selfPackageName != null && selfPackageName.equals(packageName)) {
            Log.d(TAG, "Self app detected (check 1), skipping: " + packageName);
            return;
        }
        
        // Skip if BlockerService is not running
        if (!isBlockerServiceRunning()) {
            return;
        }
        
        // 去重：避免短时间内重复处理同一应用（防止事件积压）
        long currentTime = System.currentTimeMillis();
        if (lastProcessedPackage != null && lastProcessedPackage.equals(packageName)) {
            long timeSinceLastProcess = currentTime - lastProcessedTime;
            if (timeSinceLastProcess < DEBOUNCE_PERIOD) {
                Log.d(TAG, "Duplicate event ignored: " + packageName + " (processed " + timeSinceLastProcess + "ms ago)");
                return; // 快速返回，避免重复处理
            }
        }
        
        // 更新处理记录
        lastProcessedPackage = packageName;
        lastProcessedTime = currentTime;
        
        Log.d(TAG, "Processing: " + packageName);

        boolean isBlocked = isBlockedApp(packageName);
        if (isBlocked) {
            // Immediately return to home screen to prevent app from opening
            forceStopApp(packageName);
            // Note: showBlockedMessage() is already called inside forceStopApp()
        } else {
            // Normal app - allow to open
            Log.d(TAG, "Non-blocked app opened: " + packageName);
        }
        
        Log.d(TAG, "========== End of window state change ==========");
    }
    
    @Override
    public void onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted");
        stopMonitoring();
    }
    

    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "AccessibilityService destroyed");
        stopMonitoring();
    }
    
    /**
     * Start continuous monitoring of running apps
     * 
     * CRITICAL: This monitoring runs continuously, even if:
     * - App process is sleeping
     * - AccessibilityEvent is delayed
     * - System buffers events
     * 
     * This is the SAFETY NET that ensures blocked apps are caught even if events are missed
     */
    private void startMonitoring() {
        if (monitorHandler == null) {
            monitorHandler = new android.os.Handler(getMainLooper());
        }
        
        if (monitorRunnable == null) {
            monitorRunnable = new Runnable() {
                @Override
                public void run() {
                    // 确保服务还在运行
                    if (!isBlockerServiceRunning()) {
                        // 如果服务停止，停止监控
                        stopMonitoring();
                        return;
                    }
                    
                    // 执行检查
                    checkRunningApps();
                    
                    // 立即安排下一次检查（不等待，确保连续运行）
                    if (monitorHandler != null) {
                        monitorHandler.postDelayed(this, MONITOR_INTERVAL);
                    }
                }
            };
        }
        
        // 停止旧的监控（如果存在）
        if (monitorHandler != null && monitorRunnable != null) {
            monitorHandler.removeCallbacks(monitorRunnable);
        }
        
        // 启动新的监控循环
        monitorHandler.post(monitorRunnable);
        Log.d(TAG, "Started continuous monitoring as backup (every " + MONITOR_INTERVAL + "ms)");
    }
    
    /**
     * Stop continuous monitoring
     */
    private void stopMonitoring() {
        if (monitorHandler != null && monitorRunnable != null) {
            monitorHandler.removeCallbacks(monitorRunnable);
            Log.d(TAG, "Stopped continuous monitoring");
        }
    }
    
    /**
     * Check all running apps and block any blocked apps
     * 
     * CRITICAL: This is the SAFETY NET that continues working even if:
     * - AccessibilityEvent is delayed due to app sleeping
     * - System buffers events
     * - App process is put to sleep by Android
     * 
     * This method runs continuously and will catch blocked apps even if events are missed
     */
    private void checkRunningApps() {
        if (!isBlockerServiceRunning()) {
            return;
        }
        
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am == null) {
                return;
            }
            
            java.util.List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes == null || processes.isEmpty()) {
                return;
            }
            
            // 快速检查：只检查前台应用（最重要的）
            for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                // 只检查真正的前台应用
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    if (processInfo.pkgList != null && processInfo.pkgList.length > 0) {
                        String packageName = processInfo.pkgList[0];
                        
                        // 快速跳过：系统UI和自己应用
                        if (isSystemUIWindow(packageName, "") || 
                            (selfPackageName != null && selfPackageName.equals(packageName))) {
                            continue;
                        }
                        
                        if (isBlockedApp(packageName)) {
                            Log.w(TAG, "BACKUP DETECTION: Found blocked app: " + packageName);
                            forceStopApp(packageName);
                            showBlockedMessage();
                            return;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking running apps", e);
        }
    }
    
    private void forceStopApp(String packageName) {
        Log.d(TAG, "CLEANSTEP Force stopping app: " + packageName);
        // 先显示消息（在返回桌面之前）
        showBlockedMessage();
        
        // STEP 1: 立即返回桌面（必须在主线程立即执行）
        // 注意：先返回桌面，让任务有时间添加到任务栈
        justReturnedHome = true;
        homeActionTime = System.currentTimeMillis();
        
        try {
            boolean success = performGlobalAction(GLOBAL_ACTION_HOME);
            Log.d(TAG, "CLEANSTEP 1: performGlobalAction(HOME) result: " + success);
            if (!success) {
                justReturnedHome = false;
                Log.w(TAG, "Failed to return to home screen");
            }
        } catch (Exception e) {
            Log.e(TAG, "CLEANSTEP Error returning to home", e);
            justReturnedHome = false;
        }

        // 去重：如果同一个包名已经在等待杀死，跳过
        synchronized (pendingKillProcesses) {
            if (pendingKillProcesses.contains(packageName)) {
                Log.d(TAG, "Package " + packageName + " is already pending kill, skipping");
                return; // 已经在等待杀死，跳过
            }
            pendingKillProcesses.add(packageName);
            Log.d(TAG, "Added " + packageName + " to pending kill list");
        }
        
        if (killProcessHandler == null) {
            killProcessHandler = new android.os.Handler(getMainLooper());
        }
        
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) {
            Log.e(TAG, "ActivityManager is null");
            synchronized (pendingKillProcesses) {
                pendingKillProcesses.remove(packageName);
            }
                return;
            }
            
        // STEP 2: 延迟清除任务栈（使用无障碍服务模拟用户操作）
        // Android 5.0+ 后无法通过系统API删除其他应用的任务栈
        // 只能使用无障碍服务模拟用户操作：打开多任务界面 → 找到卡片 → 向右滑动删除
        killProcessHandler.postDelayed(() -> {
            try {
                Log.d(TAG, "CLEANSTEP 2: Attempting to remove task from recent tasks: " + packageName);
                //removeTaskFromRecentTasks(packageName);
            } catch (Exception e) {
                Log.w(TAG, "CLEANSTEP Failed to remove task from recent tasks: " + e.getMessage());
            }
        }, 300); // 300ms延迟，确保返回桌面完成
        
        // STEP 3: 延迟杀死进程（在清除任务栈之后）
        killProcessHandler.postDelayed(() -> {
            try {
                Log.d(TAG, "CLEANSTEP STEP 3: Attempting to kill process: " + packageName);
                
                am.killBackgroundProcesses(packageName);
                Log.d(TAG, "CLEANSTEP Called killBackgroundProcesses for: " + packageName);
                
                // 等待一下再检查进程是否还在运行
                killProcessHandler.postDelayed(() -> {
                    try {
                        java.util.List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
                        if (processes != null) {
                            boolean stillRunning = false;
                            for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                                if (processInfo.pkgList != null) {
                                    for (String pkg : processInfo.pkgList) {
                                        if (pkg.equals(packageName)) {
                                            stillRunning = true;
                                            Log.w(TAG, "Package " + packageName + " is still running after kill attempt");
                                            break;
                                        }
                                    }
                                }
                                if (stillRunning) break;
                            }
                            if (!stillRunning) {
                                Log.d(TAG, "CLEANSTEP Package " + packageName + " successfully killed");
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "CLEANSTEP Error checking if process is still running", e);
                    }
                }, 200);
            } catch (Exception e) {
                Log.e(TAG, "CLEANSTEP Error killing background process", e);
                e.printStackTrace();
            } finally {
                // 移除记录
                synchronized (pendingKillProcesses) {
                    pendingKillProcesses.remove(packageName);
                    Log.d(TAG, "CLEANSTEP Removed " + packageName + " from pending kill list");
                }
            }
        }, 800); // 800ms延迟，确保清除任务栈操作完成 
    }
    
    
    /**
     * 检测是否是小米设备
     */
    private boolean isXiaomiDevice() {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        String brand = android.os.Build.BRAND.toLowerCase();
        return manufacturer.contains("xiaomi") || 
               brand.contains("xiaomi") || 
               manufacturer.contains("redmi") ||
               brand.contains("redmi");
    }
    

    /**
     * Show warning message that app is blocked
     */
    private void showBlockedMessage() {
        Log.d(TAG, "CLEANSTEP Showing blocked message");
        
        // 使用共享 Handler，避免重复创建
        if (killProcessHandler == null) {
            killProcessHandler = new android.os.Handler(getMainLooper());
        }
        
        killProcessHandler.post(() -> {
            try {
                Log.d(TAG, "CLEANSTEP Creating Toast message");
                Toast toast = Toast.makeText(
                        BlockerAccessibilityService.this,
                        "专注模式期间，不能打开此应用",
                        Toast.LENGTH_LONG
                );
                toast.show();
                Log.d(TAG, "CLEANSTEP Toast shown successfully");
            } catch (Exception e) {
                Log.e(TAG, "CLEANSTEP Error showing blocked message", e);
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Check if package is self app (strict check)
     * CRITICAL: This must be 100% accurate to prevent closing our own app
     */
    private boolean isSelfApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        if (selfPackageName == null) {
            selfPackageName = getPackageName();
            Log.w(TAG, "Self package name was null, re-fetched: " + selfPackageName);
        }
        return selfPackageName.equals(packageName);
    }
    
    /**
     * Check if current window is system UI window
     */
    private boolean isSystemUIWindow(String packageName, String className) {
        if (packageName == null) {
            return false;
        }
        
        // Input method windows
        if (packageName.contains("inputmethod")) {
                return true;
            }
        
        // Launcher/home screen should NOT be blocked
        if (packageName.equals("com.miui.home") || 
            packageName.equals("com.android.launcher") ||
            packageName.equals("com.android.launcher3")) {
            return true; // Home screen is system UI, don't block
        }
        
        // System UI (but not recents screen - we handle that separately)
        if (packageName.equals("com.android.systemui")) {
            // Recents screen might be okay, but other system UI should be skipped
            return false; // Let it through, we'll handle in monitoring
        }
        
        return false;
    }
    
    /**
     * Check if BlockerService is running
     * Uses static flag from BlockerService for reliable checking
     */
    private boolean isBlockerServiceRunning() {
        // Use static flag from BlockerService - more reliable than getRunningServices()
        return BlockerService.isRunning();
    }
    
    /**
     * Check if app is in blocked list
     */
    private boolean isBlockedApp(String packageName) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }
        
        for (String blockedPackage : BLOCKED_PACKAGES) {
            if (blockedPackage.equals(packageName)) {
                Log.d(TAG, "isBlockedApp: " + packageName + " is BLOCKED (matched: " + blockedPackage + ")");
                return true;
            }
        }
        
        //Log.d(TAG, "isBlockedApp: " + packageName + " is NOT blocked");
        return false;
    }
}


