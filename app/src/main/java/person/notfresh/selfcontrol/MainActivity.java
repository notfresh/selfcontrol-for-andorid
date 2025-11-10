package person.notfresh.selfcontrol;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import person.notfresh.selfcontrol.service.BlockerService;

public class MainActivity extends AppCompatActivity {
    
    // 权限请求码
    private static final int PERMISSION_REQUEST_ACCESSIBILITY = 1002;
    private static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 1003;
    
    // UI组件
    private Button startBlockButton;
    private Button stopBlockButton;
    private TextView statusTextView;
    private TextView quoteTextView;
    
    // 屏蔽状态
    private boolean isBlocking = false;
    
    // 专注名言数组
    private static final String[] FOCUS_QUOTES = {
            "与其花许多时间和精力去凿许多浅井，不如花同样的时间和精力去凿一口深井。\n—— 罗曼·罗兰",
            "聪明人会把凡是分散精力的要求置之度外，只专心致志地去学一门，学一门就要把它学好。\n—— 歌德",
            "专注和简单一直是我的秘诀之一。简单可能比复杂更难做到：你必须努力厘清思路，从而使其变得简单。但最终这是值得的，因为一旦你做到了，便可以创造奇迹。\n—— 史蒂夫·乔布斯",
            "获得惊人成就的唯一方法是专注和简化。\n—— 亨利·福特",
            "性痴则其志凝。故书痴者文必工，艺痴者技必良。\n—— 蒲松龄",
            "当你全心全意地想做一件事时，整个宇宙都会协同起来帮助你完成。\n—— 保罗·柯艾略 《炼金术士》",
            "只要专注于某一项事业，就一定会作出使自己感到吃惊的成绩来。\n—— 马克·吐温",
            "最大的敌人不是别人，正是你缺少专注、摇摆不定的心。",
            "一个人应该总是专注于他正在做的事情。当你在太阳下行走时，你不会想要点蜡烛。\n—— 史怀哲",
            "无论做什么事，都要用全部的精力和注意力去做。\n—— 西塞罗",
            "一生做好一件事。\n—— 黄永玉（中国艺术家）",
            "非淡泊无以明志，非宁静无以致远。\n—— 诸葛亮",
            "专注力是一种新的智商。\n—— 丹尼尔·戈尔曼（情商之父）",
            "多任务处理是一个神话。当你同时处理多项任务时，你只是在任务之间快速切换，而且每次切换都有成本。\n—— 戴维·迈耶（心理学家）",
            "说'不'意味着对分散注意力的事情说'不'，以便对聚焦注意力的事情说'是'。\n—— 史蒂夫·乔布斯",
            "当我写作时，我就像一个忘记了过去和未来的人。\n—— 伊莎贝尔·阿连德"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("MainActivity", "MainActivity onCreate called");
        try {
            setContentView(R.layout.activity_main);
            Log.d("MainActivity", "ContentView set successfully");
            
            // 初始化UI组件
            initViews();
            Log.d("MainActivity", "Views initialized");
            
            // 检查并请求必要权限
            checkAndRequestPermissions();
            Log.d("MainActivity", "Permission check completed");
        } catch (Exception e) {
            Log.e("MainActivity", "Error in onCreate", e);
            e.printStackTrace();
            throw e;
        }
    }
    
    private void initViews() {
        try {
            Log.d("MainActivity", "Initializing views...");
            startBlockButton = findViewById(R.id.startBlockButton);
            stopBlockButton = findViewById(R.id.stopBlockButton);
            statusTextView = findViewById(R.id.statusTextView);
            quoteTextView = findViewById(R.id.quoteTextView);
            
            if (startBlockButton == null) {
                Log.e("MainActivity", "startBlockButton is null!");
            }
            if (stopBlockButton == null) {
                Log.e("MainActivity", "stopBlockButton is null!");
            }
            if (statusTextView == null) {
                Log.e("MainActivity", "statusTextView is null!");
            }
            if (quoteTextView == null) {
                Log.e("MainActivity", "quoteTextView is null!");
            }
            
            startBlockButton.setOnClickListener(v -> {
                Log.d("MainActivity", "Start button clicked");
                try {
                    startBlockingMode();
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in startBlockingMode", e);
                    e.printStackTrace();
                }
            });
            stopBlockButton.setOnClickListener(v -> {
                Log.d("MainActivity", "Stop button clicked");
                try {
                    stopBlockingMode();
                } catch (Exception e) {
                    Log.e("MainActivity", "Error in stopBlockingMode", e);
                    e.printStackTrace();
                }
            });
            
            updateUI();
            Log.d("MainActivity", "Views initialized successfully");
        } catch (Exception e) {
            Log.e("MainActivity", "Error initializing views", e);
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("MainActivity", "MainActivity onResume called");
        // Check actual service running status and sync
        boolean serviceRunning = isServiceRunning();
        Log.d("MainActivity", "Service running status in onResume: " + serviceRunning);
        isBlocking = serviceRunning;
        updateUI();
        
        // 每次回到前台时检查电池优化状态
        checkAndRequestBatteryOptimization();
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
                if (powerManager != null && powerManager.isIgnoringBatteryOptimizations(getPackageName())) {
                    Toast.makeText(this, "已忽略电池优化，应用将保持运行", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "请忽略电池优化以确保专注模式正常工作", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("MainActivity", "MainActivity onDestroy called");
        // Don't stop service here - let it run in background
        // User can stop it manually via stop button
        // Intent serviceIntent = new Intent(this, BlockerService.class);
        // stopService(serviceIntent);
    }

    private void checkAndRequestPermissions() {
        // 检查无障碍服务权限
        checkAccessibilityPermission();
        // 检查并请求忽略电池优化
        checkAndRequestBatteryOptimization();
    }
    
    private void checkAccessibilityPermission() {
        // 检查无障碍服务是否已启用
        if (!isAccessibilityServiceEnabled()) {
            // 显示提示信息并引导用户开启
            Toast.makeText(this, "请前往设置开启无障碍服务权限", Toast.LENGTH_LONG).show();
            
            // 引导用户到无障碍服务设置页面
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        }
    }
    
    /**
     * 检查并请求忽略电池优化
     * 强制要求：确保应用不被系统休眠，始终保持运行
     * 这是应用正常工作的必要条件
     */
    private void checkAndRequestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
                String packageName = getPackageName();
                
                if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                    // 未忽略电池优化，请求用户授权
                    Log.d("MainActivity", "Battery optimization is not ignored, requesting...");
                    
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("重要提示")
                            .setMessage("为确保专注模式正常工作，必须忽略电池优化。\n\n这将允许应用在后台持续运行，不会被系统休眠。\n\n这是应用正常工作的必要条件。")
                            .setPositiveButton("前往设置", (dialog, which) -> {
                                try {
                                    Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                    intent.setData(android.net.Uri.parse("package:" + packageName));
                                    startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                } catch (Exception e) {
                                    Log.e("MainActivity", "Error requesting battery optimization", e);
                                    // 如果 ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 不可用，尝试打开设置页面
                                    try {
                                        Intent intent = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                        startActivity(intent);
                                    } catch (Exception e2) {
                                        Log.e("MainActivity", "Error opening battery optimization settings", e2);
                                    }
                                }
                            })
                            .setNegativeButton("稍后", null)
                            .show();
                } else {
                    Log.d("MainActivity", "Battery optimization is already ignored");
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error checking battery optimization", e);
            }
        }
    }
    
    /**
     * 检查无障碍服务是否已启用
     */
    private boolean isAccessibilityServiceEnabled() {
        // 完整的服务类名路径
        String serviceName = getPackageName() + "/person.notfresh.selfcontrol.service.BlockerAccessibilityService";
        try {
            int accessibilityEnabled = android.provider.Settings.Secure.getInt(
                    getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
            
            if (accessibilityEnabled == 1) {
                String enabledServices = android.provider.Settings.Secure.getString(
                        getContentResolver(),
                        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
                
                if (enabledServices != null) {
                    // Debug logs
                    Log.d("AccessibilityCheck", "Enabled accessibility services: " + enabledServices);
                    Log.d("AccessibilityCheck", "Looking for service: " + serviceName);
                    
                    // Check if service is in enabled list
                    boolean isEnabled = enabledServices.contains(serviceName);
                    Log.d("AccessibilityCheck", "Service enabled status: " + isEnabled);
                    
                    return isEnabled;
                }
            } else {
                Log.d("AccessibilityCheck", "Accessibility service master switch is OFF");
            }
        } catch (Exception e) {
            Log.e("AccessibilityCheck", "Error checking accessibility service", e);
        }
        return false;
    }
    
    /**
     * 检查 BlockerService 是否正在运行
     */
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (BlockerService.class.getName().equals(service.service.getClassName())) {
                    Log.d("ServiceCheck", "BlockerService is running");
                    return true;
                }
            }
        }
        Log.d("ServiceCheck", "BlockerService is not running");
        return false;
    }
    
    private void startBlockingMode() {
        Log.d("MainActivity", "startBlockingMode called");
        
        try {
            Log.d("MainActivity", "Checking permissions...");
            if (!hasRequiredPermissions()) {
                Log.d("MainActivity", "Missing required permissions");
                // Detailed permission message
                StringBuilder message = new StringBuilder("请先授予以下权限：\n");
                
                if (!isAccessibilityServiceEnabled()) {
                    message.append("• 无障碍服务权限\n");
                }
                
                // 检查电池优化（强制要求）
                if (!isBatteryOptimizationIgnored()) {
                    message.append("• 忽略电池优化（必需）\n");
                }
                
                message.append("\n点击确定前往设置");
                
                Log.d("MainActivity", "Showing permission dialog");
                // Show detailed permission prompt
                try {
                    new android.app.AlertDialog.Builder(this)
                            .setTitle("缺少必要权限")
                            .setMessage(message.toString())
                            .setPositiveButton("确定", (dialog, which) -> {
                                // Re-check and request permissions
                                checkAndRequestPermissions();
                            })
                            .setNegativeButton("取消", null)
                            .show();
                } catch (Exception dialogError) {
                    Log.e("MainActivity", "Error showing dialog", dialogError);
                    dialogError.printStackTrace();
                }
                return;
            }
            
            Log.d("MainActivity", "All permissions granted, starting service");
            isBlocking = true;
            // Show random quote when starting focus mode
            showRandomQuote();
            updateUI();
            
            Log.d("MainActivity", "Creating service intent...");
            // Start blocking service
            Intent serviceIntent = new Intent(this, BlockerService.class);
            
            Log.d("MainActivity", "Starting BlockerService...");
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Log.d("MainActivity", "Calling startForegroundService (Android O+)");
                    startForegroundService(serviceIntent);
                } else {
                    Log.d("MainActivity", "Calling startService (pre-Android O)");
                    startService(serviceIntent);
                }
                Log.d("MainActivity", "BlockerService startForegroundService called successfully");
                
                // Wait a bit and check if service is actually running
                new android.os.Handler(getMainLooper()).postDelayed(() -> {
                    boolean isRunning = isServiceRunning();
                    Log.d("MainActivity", "Service running check after 1 second: " + isRunning);
                    if (!isRunning) {
                        Log.e("MainActivity", "WARNING: Service did not start!");
                    }
                }, 1000);
            } catch (SecurityException se) {
                Log.e("MainActivity", "SecurityException when starting service", se);
                se.printStackTrace();
                isBlocking = false;
                updateUI();
                Toast.makeText(this, "Permission error: " + se.getMessage(), Toast.LENGTH_LONG).show();
                return;
            } catch (Exception e) {
                Log.e("MainActivity", "Failed to start BlockerService", e);
                e.printStackTrace();
                isBlocking = false;
                updateUI();
                Toast.makeText(this, "Failed to start service: " + e.getMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            
            Log.d("MainActivity", "Showing success toast");
            Toast.makeText(this, "专注模式已启动", Toast.LENGTH_SHORT).show();
            Log.d("MainActivity", "startBlockingMode completed successfully");
        } catch (Exception e) {
            Log.e("MainActivity", "Unexpected error in startBlockingMode", e);
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void stopBlockingMode() {
        // Show confirmation dialog with focus time statistics
        long currentDuration = BlockerService.getCurrentSessionDuration(this);
        long totalToday = BlockerService.getTotalFocusTimeToday(this);
        
        String currentDurationText = BlockerService.formatDuration(currentDuration);
        String totalTodayText = BlockerService.formatDuration(totalToday);
        
        new android.app.AlertDialog.Builder(this)
                .setTitle("确定要结束专注模式吗？")
                .setMessage("本次专注时长：\n" + currentDurationText + "\n\n今日累计时长：\n" + totalTodayText)
                .setPositiveButton("继续专注", null)
                .setNegativeButton("结束专注", (dialog, which) -> {
                    isBlocking = false;
                    updateUI();
                    
                    // 停止屏蔽服务
                    Intent serviceIntent = new Intent(this, BlockerService.class);
                    stopService(serviceIntent);
                    
                    Toast.makeText(this, "专注模式已停止", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    
    private boolean hasRequiredPermissions() {
        // 检查无障碍服务是否已启用（必需）
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        
        // 检查电池优化是否已忽略（强制要求）
        boolean batteryOptimizationIgnored = isBatteryOptimizationIgnored();
        
        return accessibilityEnabled && batteryOptimizationIgnored;
    }
    
    /**
     * 检查是否忽略了电池优化（强制要求）
     * 应用必须忽略电池优化才能正常工作，否则会被系统休眠
     */
    private boolean isBatteryOptimizationIgnored() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.os.PowerManager powerManager = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                return powerManager.isIgnoringBatteryOptimizations(getPackageName());
            }
        }
        return true; // Android M 以下版本默认返回 true
    }
    
    private void updateUI() {
        if (isBlocking) {
            // Display real-time focus duration
            long currentDuration = BlockerService.getCurrentSessionDuration(this);
            long totalToday = BlockerService.getTotalFocusTimeToday(this);
            
            String currentDurationText = BlockerService.formatDuration(currentDuration);
            String totalTodayText = BlockerService.formatDuration(totalToday);
            
            StringBuilder statusText = new StringBuilder("专注模式运行中\n\n");
            statusText.append("本次专注：").append(currentDurationText).append("\n");
            statusText.append("今日累计：").append(totalTodayText);
            
            statusTextView.setText(statusText.toString());
            startBlockButton.setEnabled(false);
            stopBlockButton.setEnabled(true);
            
            // Schedule UI update every second while blocking
            if (isBlocking) {
                new android.os.Handler(getMainLooper()).postDelayed(() -> {
                    if (isBlocking && isServiceRunning()) {
                        updateUI(); // Refresh UI
                    }
                }, 1000);
            }
        } else {
            // Hide quote when not blocking
            quoteTextView.setVisibility(android.view.View.GONE);
            
            // 显示当前权限状态
            StringBuilder statusText = new StringBuilder("专注模式未启动\n\n权限状态：\n");
            
            if (isAccessibilityServiceEnabled()) {
                statusText.append("✓ 无障碍服务已启用\n");
            } else {
                statusText.append("✗ 无障碍服务未启用\n");
            }
            
            // 检查电池优化状态（强制要求）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (isBatteryOptimizationIgnored()) {
                    statusText.append("✓ 已忽略电池优化");
                } else {
                    statusText.append("✗ 未忽略电池优化（必需）");
                }
            }
            
            statusTextView.setText(statusText.toString());
            startBlockButton.setEnabled(hasRequiredPermissions());
            stopBlockButton.setEnabled(false);
        }
    }
    
    /**
     * Show a random focus quote
     */
    private void showRandomQuote() {
        if (quoteTextView != null && FOCUS_QUOTES.length > 0) {
            java.util.Random random = new java.util.Random();
            int randomIndex = random.nextInt(FOCUS_QUOTES.length);
            quoteTextView.setText(FOCUS_QUOTES[randomIndex]);
            quoteTextView.setVisibility(android.view.View.VISIBLE);
        }
    }

    
}