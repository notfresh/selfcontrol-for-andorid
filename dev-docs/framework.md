分析 `BlockerService` 和 `MainActivity` 的职责：


[2 tools called]

职责划分如下：

## BlockerService 的职责

### 核心职责：状态标志管理器

作用：提供一个全局状态标志，告知 `AccessibilityService` 是否启用屏蔽模式。

### 具体职责：

#### 1. 状态标志管理（第26-35行）
```java
// 静态标志 - 这是核心功能
private static volatile boolean isServiceRunning = false;

public static boolean isRunning() {
    return isServiceRunning;  // 供 AccessibilityService 检查
}
```

作用：
- 维护一个静态布尔值 `isServiceRunning`
- 提供 `isRunning()` 供其他组件查询
- `AccessibilityService` 通过它判断是否启用屏蔽

#### 2. 前台服务维持（第54-86行）
```java
onStartCommand() {
    isServiceRunning = true;  // 设置标志
    startForeground(...);    // 启动前台服务
    return START_STICKY;      // 服务被杀死后自动重启
}
```

作用：
- 启动并维持前台服务
- 显示持久通知（"防沉迷模式运行中"）
- 防止被系统杀死（`START_STICKY`）

#### 3. 生命周期管理（第88-98行）
```java
onDestroy() {
    isServiceRunning = false;  // 清除标志
}
```

作用：
- 销毁时重置标志为 `false`
- 通知 `AccessibilityService` 屏蔽已停止

### 重要特点：

- 不直接屏蔽应用：不包含屏蔽逻辑
- 只是一个标志：表示“屏蔽模式是否开启”
- 轻量级：仅维持状态标志和通知

---

## MainActivity 的职责

### 核心职责：用户界面和入口控制

作用：用户交互入口，负责启动/停止服务和权限管理。

### 具体职责：

#### 1. UI 界面管理（第53-96行）
```java
initViews() {
    // 绑定按钮、设置点击事件
    startBlockButton.setOnClickListener(...);
    stopBlockButton.setOnClickListener(...);
    updateUI();  // 更新界面状态
}
```

作用：
- 初始化 UI 组件
- 绑定按钮点击事件
- 更新界面显示状态

#### 2. 权限检查和引导（第119-170行）
```java
checkAccessibilityPermission() {
    if (!isAccessibilityServiceEnabled()) {
        // 显示提示，引导用户去设置
        Toast.makeText(...);
        startActivity(ACCESSIBILITY_SETTINGS);
    }
}
```

作用：
- 检查无障碍服务是否已启用
- 未启用时引导用户去系统设置开启
- 显示权限状态

#### 3. 服务启动/停止控制（第189-286行）
```java
startBlockingMode() {
    // 1. 检查权限
    if (!hasRequiredPermissions()) {
        // 显示权限提示对话框
        return;
    }
    
    // 2. 启动 BlockerService
    Intent serviceIntent = new Intent(this, BlockerService.class);
    startForegroundService(serviceIntent);
    
    // 3. 更新UI
    isBlocking = true;
    updateUI();
}

stopBlockingMode() {
    // 停止 BlockerService
    stopService(serviceIntent);
    isBlocking = false;
    updateUI();
}
```

作用：
- 启动：检查权限 → 启动 `BlockerService` → 更新 UI
- 停止：停止 `BlockerService` → 更新 UI

#### 4. 状态同步（第98-107行）
```java
onResume() {
    // 检查服务实际运行状态
    boolean serviceRunning = isServiceRunning();
    isBlocking = serviceRunning;  // 同步状态
    updateUI();
}
```

作用：
- 从后台恢复时检查服务实际状态
- 同步 UI 显示与实际状态

---

## 职责对比表

| 职责 | MainActivity | BlockerService |
|------|-------------|----------------|
| 用户界面 | ✅ 负责 | ❌ 不负责 |
| 权限检查 | ✅ 负责 | ❌ 不负责 |
| 启动/停止服务 | ✅ 负责（启动者） | ❌ 不负责（被启动） |
| 状态标志管理 | ❌ 不负责 | ✅ 负责 |
| 前台服务维持 | ❌ 不负责 | ✅ 负责 |
| 实际屏蔽逻辑 | ❌ 不负责 | ❌ 不负责（由 AccessibilityService 负责） |

---

## 三者协作关系

```
用户操作流程：

1. 用户打开应用
   ↓
   MainActivity.onCreate()
   → 显示界面
   → 检查权限

2. 用户点击"开始屏蔽"
   ↓
   MainActivity.startBlockingMode()
   → 检查权限 ✓
   → 启动 BlockerService

3. BlockerService 启动
   ↓
   BlockerService.onStartCommand()
   → isServiceRunning = true  ← 关键标志
   → 显示前台通知

4. AccessibilityService 检查状态
   ↓
   BlockerAccessibilityService.handleWindowStateChange()
   → 调用 BlockerService.isRunning()
   → 返回 true ✓
   → 执行屏蔽逻辑

5. 用户点击"停止屏蔽"
   ↓
   MainActivity.stopBlockingMode()
   → 停止 BlockerService

6. BlockerService 停止
   ↓
   BlockerService.onDestroy()
   → isServiceRunning = false
   → AccessibilityService 检测到，停止屏蔽
```

---

## 总结

### BlockerService
- 职责：状态标志管理器
- 作用：告诉 `AccessibilityService` 是否启用屏蔽
- 特点：轻量、只维护状态

### MainActivity
- 职责：用户界面和入口控制
- 作用：用户交互入口，启动/停止服务
- 特点：负责 UI、权限、服务控制

### BlockerAccessibilityService（补充）
- 职责：实际执行屏蔽逻辑
- 作用：监听应用启动，执行屏蔽
- 特点：依赖 `BlockerService` 的标志判断是否启用

分工明确：`MainActivity` 负责用户交互，`BlockerService` 负责状态标志，`AccessibilityService` 负责实际屏蔽。