package person.notfresh.selfcontrol;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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
    private Button manualButton;
    
    // 屏蔽状态
    private boolean isBlocking = false;
    
    // 菜单项引用（用于控制显示/隐藏）
    private android.view.MenuItem themeColorMenuItem;
    
    // 主题颜色相关
    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_THEME_COLOR = "theme_color";
    
    // 主题颜色配置（颜色值和名称）
    private static final int[] THEME_COLORS = {
            R.color.theme_blue_grey,
            R.color.theme_purple,
            R.color.theme_blue,
            R.color.theme_green,
            R.color.theme_red,
            R.color.theme_orange,
            R.color.theme_teal,
            R.color.theme_cyan,
            R.color.theme_indigo,
            R.color.theme_pink,
            R.color.theme_brown,
            R.color.theme_grey,
            
    };
    
    private static final String[] THEME_COLOR_NAMES = {
            "蓝灰色",
            "紫色", "蓝色", "绿色", "红色", "橙色",
            "青色", "天蓝色", "靛蓝色", "粉色", "棕色",
            "灰色", 
    };
    
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
            manualButton = findViewById(R.id.manualButton);
            
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
            if (manualButton == null) {
                Log.e("MainActivity", "manualButton is null!");
            }
            
            // 设置使用说明书按钮点击事件
            manualButton.setOnClickListener(v -> {
                Log.d("MainActivity", "Manual button clicked");
                showManualDialog();
            });
            
            startBlockButton.setOnClickListener(v -> {
                Log.d("MainActivity", "Start button clicked");
                try {
                    // 如果权限不足，打开权限设置对话框（复用设置按钮的逻辑）
                    if (!hasRequiredPermissions()) {
                        showManualPermissionDialog();
                        return;
                    }
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
            
            // Apply saved theme color
            applyThemeColor();
            
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
        
        // 不再自动弹窗，用户可以通过设置按钮手动检查权限
        
        // 更新菜单项可见性
        invalidateOptionsMenu();
    }
    
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        themeColorMenuItem = menu.findItem(R.id.action_theme_color);
        updateMenuVisibility();
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_theme_color) {
            Log.d("MainActivity", "Theme color menu item clicked");
            showThemeColorDialog();
            return true;
        } else if (id == R.id.action_settings) {
            Log.d("MainActivity", "Settings menu item clicked");
            // 打开权限设置页面，让用户手动选择要设置的权限
            showManualPermissionDialog();
            return true;
        } else if (id == R.id.action_manual) {
            Log.d("MainActivity", "Manual menu item clicked");
            showManualDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * Update menu item visibility based on blocking state
     */
    private void updateMenuVisibility() {
        if (themeColorMenuItem != null) {
            // Hide theme color menu item when blocking, show when not blocking
            themeColorMenuItem.setVisible(!isBlocking);
        }
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
        // 检查所有权限，每次进入应用都会检查
        boolean needsAccessibility = !isAccessibilityServiceEnabled();
        boolean needsBatteryOptimization = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                !isBatteryOptimizationIgnored();
        
        // 如果所有权限都已授予，直接返回
        if (!needsAccessibility && !needsBatteryOptimization) {
            return;
        }
        
        // 构建权限提示消息
        StringBuilder message = new StringBuilder("请授予以下权限以正常使用应用：\n\n");
        
        if (needsAccessibility) {
            message.append("• 无障碍服务权限\n");
        }
        
        if (needsBatteryOptimization) {
            message.append("• 忽略电池优化（必需）\n");
        }
        
        message.append("\n您可以点击下方按钮逐个设置权限");
        
        // 创建对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this)
                .setTitle("缺少必要权限")
                .setMessage(message.toString());
        
        // 根据缺失的权限添加按钮
        // 按钮顺序：Negative（左） → Neutral（中） → Positive（右）
        if (needsAccessibility && needsBatteryOptimization) {
            // 两个权限都缺失，添加三个按钮
            builder.setNegativeButton("设置无障碍服务", (dialog, which) -> {
                openAccessibilitySettings();
            })
            .setNeutralButton("设置电池优化", (dialog, which) -> {
                openBatteryOptimizationSettings();
            })
            .setPositiveButton("稍后", null);
        } else if (needsAccessibility) {
            // 只缺无障碍服务
            builder.setNegativeButton("前往设置", (dialog, which) -> {
                openAccessibilitySettings();
            })
            .setPositiveButton("稍后", null);
        } else if (needsBatteryOptimization) {
            // 只缺电池优化
            builder.setNegativeButton("前往设置", (dialog, which) -> {
                openBatteryOptimizationSettings();
            })
            .setPositiveButton("稍后", null);
        }
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // 设置按钮文字颜色为黑色并加粗（避免被主题颜色影响）
        int blackColor = ContextCompat.getColor(this, R.color.black);
        android.widget.Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        android.widget.Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
        android.widget.Button neutralButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL);
        
        if (positiveButton != null) {
            positiveButton.setTextColor(blackColor);
            positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(blackColor);
            negativeButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (neutralButton != null) {
            neutralButton.setTextColor(blackColor);
            neutralButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }
    
    /**
     * 打开无障碍服务设置页面
     */
    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening accessibility settings", e);
            Toast.makeText(this, "无法打开无障碍服务设置", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 打开电池优化设置页面
     */
    private void openBatteryOptimizationSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                String packageName = getPackageName();
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
                    Toast.makeText(this, "无法打开电池优化设置", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    /**
     * 显示手动权限设置对话框（通过设置按钮打开）
     * 让用户可以选择要设置的权限
     */
    private void showManualPermissionDialog() {
        boolean needsAccessibility = !isAccessibilityServiceEnabled();
        boolean needsBatteryOptimization = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && 
                !isBatteryOptimizationIgnored();
        
        // 如果所有权限都已授予
        if (!needsAccessibility && !needsBatteryOptimization) {
            Toast.makeText(this, "所有权限已授予", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 构建权限提示消息
        StringBuilder message = new StringBuilder("请设置以下权限：\n\n");
        
        if (needsAccessibility) {
            message.append("• 无障碍服务权限\n");
        }
        
        if (needsBatteryOptimization) {
            message.append("• 忽略电池优化（必需）\n");
        }
        
        message.append("\n请选择要设置的权限");
        
        // 创建对话框
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this)
                .setTitle("权限设置")
                .setMessage(message.toString());
        
        // 根据缺失的权限添加按钮
        if (needsAccessibility && needsBatteryOptimization) {
            // 两个权限都缺失，添加三个按钮
            // 按钮顺序：Negative（左） → Neutral（中） → Positive（右）
            builder.setNegativeButton("设置无障碍服务", (dialog, which) -> {
                openAccessibilitySettings();
            })
            .setNeutralButton("设置电池优化", (dialog, which) -> {
                openBatteryOptimizationSettings();
            })
            .setPositiveButton("取消", null);
        } else if (needsAccessibility) {
            // 只缺无障碍服务
            builder.setPositiveButton("前往设置", (dialog, which) -> {
                openAccessibilitySettings();
            })
            .setNegativeButton("取消", null);
        } else if (needsBatteryOptimization) {
            // 只缺电池优化
            builder.setPositiveButton("前往设置", (dialog, which) -> {
                openBatteryOptimizationSettings();
            })
            .setNegativeButton("取消", null);
        }
        
        android.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // 设置按钮文字颜色为黑色并加粗（避免被主题颜色影响）
        int blackColor = ContextCompat.getColor(this, R.color.black);
        android.widget.Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
        android.widget.Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
        android.widget.Button neutralButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEUTRAL);
        
        if (positiveButton != null) {
            positiveButton.setTextColor(blackColor);
            positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(blackColor);
            negativeButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        if (neutralButton != null) {
            neutralButton.setTextColor(blackColor);
            neutralButton.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }
    
    private void checkAccessibilityPermission() {
        // 检查无障碍服务是否已启用
        // 不再自动跳转，只检查状态，用户可以通过设置按钮手动跳转
        if (!isAccessibilityServiceEnabled()) {
            // 只显示提示信息，不自动跳转
            Log.d("MainActivity", "Accessibility service is not enabled");
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
            
            // Update menu visibility
            invalidateOptionsMenu();
            
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
                    
                    // Update menu visibility
                    invalidateOptionsMenu();
                    
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
            
            // Hide start and theme buttons, show stop button when blocking
            startBlockButton.setVisibility(android.view.View.GONE);
            stopBlockButton.setVisibility(android.view.View.VISIBLE);
            stopBlockButton.setEnabled(true);
            
            // Hide manual button when blocking
            manualButton.setVisibility(android.view.View.GONE);
            
            // Hide theme color menu item
            updateMenuVisibility();
            
            // Schedule UI update every second while blocking
            if (isBlocking) {
                new android.os.Handler(getMainLooper()).postDelayed(() -> {
                    if (isBlocking && isServiceRunning()) {
                        updateUI(); // Refresh UI
                    }
                }, 1000);
            }
        } else {
            // Show start and theme buttons, hide stop button when not blocking
            startBlockButton.setVisibility(android.view.View.VISIBLE);
            stopBlockButton.setVisibility(android.view.View.GONE);
            
            // Show manual button when not blocking
            manualButton.setVisibility(android.view.View.VISIBLE);
            
            // Show theme color menu item
            updateMenuVisibility();
            
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
            
            // 设置按钮的颜色（按钮始终启用，点击事件中会处理权限检查）
            boolean hasPermissions = hasRequiredPermissions();
            // 按钮始终启用，这样即使权限不足也可以点击，点击时会弹出权限设置对话框
            startBlockButton.setEnabled(true);
            
            // 如果权限不足，设置启动按钮为灰色（视觉提示）
            if (!hasPermissions) {
                int grayColor = ContextCompat.getColor(this, android.R.color.darker_gray);
                startBlockButton.setBackgroundColor(grayColor);
                startBlockButton.setTextColor(Color.WHITE);
            } else {
                // 如果有权限，应用主题颜色到启动按钮
                int colorIndex = getSavedThemeColorIndex();
                if (colorIndex < 0 || colorIndex >= THEME_COLORS.length) {
                    colorIndex = 10; // Fallback to default grey
                }
                int colorResId = THEME_COLORS[colorIndex];
                int colorValue = ContextCompat.getColor(this, colorResId);
                startBlockButton.setBackgroundColor(colorValue);
                startBlockButton.setTextColor(Color.WHITE);
            }
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
    
    /**
     * Show theme color selection dialog
     */
    private void showThemeColorDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("选择主题颜色");
        
        // Create color items for dialog
        String[] items = new String[THEME_COLOR_NAMES.length];
        for (int i = 0; i < THEME_COLOR_NAMES.length; i++) {
            items[i] = THEME_COLOR_NAMES[i];
        }
        
        builder.setItems(items, (dialog, which) -> {
            saveThemeColor(which);
            applyThemeColor();
            Toast.makeText(this, "主题颜色已更改为：" + THEME_COLOR_NAMES[which], Toast.LENGTH_SHORT).show();
        });
        
        builder.show();
    }
    
    /**
     * Save selected theme color index
     */
    private void saveThemeColor(int colorIndex) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_COLOR, colorIndex).apply();
    }
    
    /**
     * Get saved theme color index
     */
    private int getSavedThemeColorIndex() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_COLOR, 10); // Default to grey (index 10)
    }
    
    /**
     * Apply theme color to UI elements
     */
    private void applyThemeColor() {
        int colorIndex = getSavedThemeColorIndex();
        if (colorIndex < 0 || colorIndex >= THEME_COLORS.length) {
            colorIndex = 10; // Fallback to default grey
        }
        
        int colorResId = THEME_COLORS[colorIndex];
        int colorValue = ContextCompat.getColor(this, colorResId);
        
        // Apply to buttons (only if permissions are granted for start button)
        if (startBlockButton != null && hasRequiredPermissions()) {
            startBlockButton.setBackgroundColor(colorValue);
            startBlockButton.setTextColor(Color.WHITE);
        }
        if (stopBlockButton != null) {
            stopBlockButton.setBackgroundColor(colorValue);
            stopBlockButton.setTextColor(Color.WHITE);
        }
        if (manualButton != null) {
            manualButton.setBackgroundColor(colorValue);
            manualButton.setTextColor(Color.WHITE);
        }
        
        // Apply to ActionBar/Toolbar (顶部标题栏)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(colorValue));
        }
        
        // Apply to status bar (Android 5.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(colorValue);
        }
    }
    
    /**
     * Show manual dialog with Gitee and GitHub options
     */
    private void showManualDialog() {
        // 创建自定义布局视图
        android.view.LayoutInflater inflater = getLayoutInflater();
        android.view.View dialogView = inflater.inflate(R.layout.dialog_manual, null);
        
        // 创建对话框
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        // 获取按钮引用
        Button btnGitee = dialogView.findViewById(R.id.btnGitee);
        Button btnGitHub = dialogView.findViewById(R.id.btnGitHub);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
        
        // 设置按钮文字颜色为黑色
        int blackColor = ContextCompat.getColor(this, R.color.black);
        btnGitee.setTextColor(blackColor);
        btnGitHub.setTextColor(blackColor);
        btnCancel.setTextColor(blackColor);
        
        // 明确设置按钮背景，确保使用黑色边框（避免被主题颜色覆盖）
        android.graphics.drawable.Drawable borderDrawable = ContextCompat.getDrawable(this, R.drawable.button_border_black);
        btnGitee.setBackground(borderDrawable);
        btnGitHub.setBackground(borderDrawable);
        btnCancel.setBackground(borderDrawable);
        
        // 禁用主题颜色对按钮的影响（Material Components Button 可能会应用主题颜色）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            btnGitee.setBackgroundTintList(null);
            btnGitHub.setBackgroundTintList(null);
            btnCancel.setBackgroundTintList(null);
        }
        
        // 设置按钮点击事件
        btnGitee.setOnClickListener(v -> {
            dialog.dismiss();
            openUrl("https://gitee.com/notfresh/selfcontrol-for-andorid");
        });
        
        btnGitHub.setOnClickListener(v -> {
            dialog.dismiss();
            openUrl("https://github.com/notfresh/selfcontrol-for-andorid");
        });
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        // 显示对话框
        dialog.show();
    }
    
    /**
     * Open URL in browser
     */
    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            Log.e("MainActivity", "Error opening URL: " + url, e);
            Toast.makeText(this, "无法打开链接：" + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    
}