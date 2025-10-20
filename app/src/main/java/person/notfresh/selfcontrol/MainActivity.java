package person.notfresh.noteplus;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {
    
    // 权限请求码
    private static final int PERMISSION_REQUEST_OVERLAY = 1001;
    private static final int PERMISSION_REQUEST_ACCESSIBILITY = 1002;
    
    // UI组件
    private Button startBlockButton;
    private Button stopBlockButton;
    private TextView statusTextView;
    
    // 屏蔽状态
    private boolean isBlocking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 初始化UI组件
        initViews();
        
        // 检查并请求必要权限
        checkAndRequestPermissions();
    }
    
    private void initViews() {
        startBlockButton = findViewById(R.id.startBlockButton);
        stopBlockButton = findViewById(R.id.stopBlockButton);
        statusTextView = findViewById(R.id.statusTextView);
        
        startBlockButton.setOnClickListener(v -> startBlockingMode());
        stopBlockButton.setOnClickListener(v -> stopBlockingMode());
        
        updateUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 停止屏蔽服务
        Intent serviceIntent = new Intent(this, BlockerService.class);
        stopService(serviceIntent);
    }

    private void checkAndRequestPermissions() {
        // 检查悬浮窗权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, PERMISSION_REQUEST_OVERLAY);
            }
        }
        
        // 检查无障碍服务权限
        checkAccessibilityPermission();
    }
    
    private void checkAccessibilityPermission() {
        // 这里需要检查无障碍服务是否已启用
        // 暂时显示提示信息
        Toast.makeText(this, "请前往设置开启无障碍服务权限", Toast.LENGTH_LONG).show();
    }
    
    private void startBlockingMode() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(this, "请先授予所有必要权限", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isBlocking = true;
        updateUI();
        
        // 启动屏蔽服务
        Intent serviceIntent = new Intent(this, BlockerService.class);
        startForegroundService(serviceIntent);
        
        Toast.makeText(this, "专注模式已启动", Toast.LENGTH_SHORT).show();
    }
    
    private void stopBlockingMode() {
        isBlocking = false;
        updateUI();
        
        // 停止屏蔽服务
        Intent serviceIntent = new Intent(this, BlockerService.class);
        stopService(serviceIntent);
        
        Toast.makeText(this, "专注模式已停止", Toast.LENGTH_SHORT).show();
    }
    
    private boolean hasRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }
    
    private void updateUI() {
        if (isBlocking) {
            statusTextView.setText("专注模式运行中");
            startBlockButton.setEnabled(false);
            stopBlockButton.setEnabled(true);
        } else {
            statusTextView.setText("专注模式未启动");
            startBlockButton.setEnabled(true);
            stopBlockButton.setEnabled(false);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == PERMISSION_REQUEST_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "悬浮窗权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "悬浮窗权限未授予，部分功能可能无法使用", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
    
}