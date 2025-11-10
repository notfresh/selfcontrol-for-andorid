## 倒计时功能方案

### 1. 时间选择与传递
- UI：在 `MainActivity` 中添加时间选择器（Spinner/TimePicker），预设选项：5/10/15/30/60 分钟
- 传递：启动专注模式时，通过 `Intent` 的 `Extra` 将时长（毫秒）传给 `BlockerService`
- 存储：使用 `SharedPreferences` 持久化倒计时状态，避免进程重启丢失

### 2. 倒计时实现
- 使用 `CountDownTimer`（或 `Handler.postDelayed` + `Runnable`）
- 在 `BlockerService` 中管理倒计时：
  - `onTick()` 更新通知栏显示剩余时间
  - `onFinish()` 标记倒计时结束，允许停止
- 状态标志：`isTimerActive`（倒计时中）和 `isTimerFinished`（已结束）

### 3. 解锁控制
- 倒计时期间：
  - `MainActivity` 的停止按钮禁用（`setEnabled(false)`）
  - `stopBlockingMode()` 检查倒计时状态，未结束则拒绝并提示
- 倒计时结束后：
  - 启用停止按钮
  - 允许正常停止服务

## 防止应用被强制关闭方案

### 1. 系统窗口覆盖层（Overlay）
- 原理：使用 `WindowManager` 创建 `TYPE_APPLICATION_OVERLAY` 系统窗口
- 权限：`SYSTEM_ALERT_WINDOW`（Android 6.0+ 需动态请求）
- 实现：
  - 创建全屏透明/半透明覆盖层
  - 拦截触摸事件（`onTouchEvent` 返回 `true`）
  - 显示提示文字（如“专注模式进行中，请稍候”）

### 2. 防止返回主界面
- 监听：在 `BlockerAccessibilityService` 中监听 `GLOBAL_ACTION_HOME`
- 拦截：检测到返回桌面时，立即显示覆盖层
- 时机：在 `onAccessibilityEvent()` 中检测窗口变化，或使用 `performGlobalAction()` 的返回值判断

### 3. 防止任务管理器关闭
- 方案A：覆盖层拦截
  - 覆盖层覆盖整个屏幕，拦截所有触摸
  - 用户无法点击任务管理器中的“关闭”按钮
- 方案B：监听应用生命周期
  - 在 `MainActivity.onDestroy()` 中检测是否在倒计时期间
  - 如果是，立即重启服务或显示覆盖层

### 4. 防止系统设置中关闭服务
- 限制：无法完全阻止用户在设置中关闭无障碍服务
- 缓解：
  - 检测服务状态变化（`onServiceConnected()` / `onServiceDisconnected()`）
  - 服务被关闭时，立即显示全屏警告覆盖层
  - 提示用户重新开启服务

### 5. 防止强制停止（应用信息 → 强制停止）
- 限制：无法完全阻止
- 缓解：
  - 使用前台服务（`startForeground()`）提高优先级
  - 使用 `START_STICKY` 让系统自动重启服务
  - 在 `BlockerService.onDestroy()` 中检测异常停止，记录日志

## 技术要点

### 1. 系统窗口创建
```java
WindowManager.LayoutParams params = new WindowManager.LayoutParams(
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.MATCH_PARENT,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,  // 系统窗口类型
    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |      // 不拦截触摸事件
    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,   // 监听外部触摸
    PixelFormat.TRANSLUCENT
);
```

### 2. 倒计时状态同步
- `BlockerService` 维护倒计时状态
- `MainActivity` 通过 `BroadcastReceiver` 接收倒计时更新
- 或使用 `LocalBroadcastManager` 在应用内通信

### 3. 进程保活
- 前台服务 + 通知栏常驻
- `START_STICKY` 自动重启
- 忽略电池优化（已有）
- WakeLock 防止休眠（已有）

## 潜在问题与限制

1. 系统窗口权限：用户可能拒绝授权
2. 无障碍服务：用户可在设置中关闭
3. 强制停止：系统级操作，无法完全阻止
4. 重启设备：重启后服务需重新启动

## 推荐实现顺序

1. 先实现倒计时功能（UI + 服务端逻辑）
2. 添加倒计时期间禁用停止按钮
3. 实现系统窗口覆盖层
4. 在无障碍服务中监听返回桌面事件
5. 测试各种关闭场景，完善边界处理

该方案可在大多数情况下防止用户提前退出，但无法做到 100% 防关闭（受 Android 安全机制限制）。