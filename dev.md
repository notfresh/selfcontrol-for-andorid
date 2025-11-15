# 技术原理详解

## 屏蔽机制详解

专注助手通过以下机制实现应用屏蔽：

### 1. 服务启动阶段

当您点击"开始专注"按钮时：
- **BlockerService（前台服务）启动**：创建一个前台服务，显示通知栏提示，防止被系统杀死
- **设置屏蔽标志**：服务启动后设置 `isServiceRunning = true`，作为屏蔽状态标志
- **BlockerAccessibilityService 激活**：无障碍服务检测到 `BlockerService` 运行后，开始监控应用

### 2. 应用检测阶段

**主要检测方式（事件驱动）：**
- **监听窗口状态变化**：无障碍服务监听 `TYPE_WINDOW_STATE_CHANGED` 事件
- **获取应用包名**：当任何应用的窗口打开时，系统会触发事件，应用从中获取包名（如 `com.tencent.mm`）
- **包名匹配**：将获取的包名与预定义的被屏蔽应用列表进行匹配

**备用检测方式（轮询检测）：**
- **定期检查运行应用**：每 500 毫秒检查一次当前前台运行的应用（作为备用机制）
- **确保不遗漏**：即使事件被延迟或遗漏，也能通过轮询检测到被屏蔽的应用

### 3. 屏蔽执行阶段

当检测到被屏蔽的应用时，立即执行以下操作（按顺序）：

**步骤 1：显示提示消息**
- 显示 Toast 提示："专注模式期间，不能打开此应用"

**步骤 2：立即返回桌面**
- 使用 `performGlobalAction(GLOBAL_ACTION_HOME)` 执行返回桌面操作
- 这是最关键的步骤，可以立即阻止用户看到被屏蔽应用的界面

**步骤 3：尝试终止进程（延迟执行）**
- 延迟 800 毫秒后，尝试使用 `ActivityManager.killBackgroundProcesses()` 终止应用进程
- 注意：此操作需要系统权限支持，在某些设备上可能无法成功，但不影响主要屏蔽功能

### 4. 去重机制

- **防重复处理**：如果同一个应用在短时间内多次触发事件，只处理一次（去重时间窗口：500 毫秒）
- **进程去重**：如果某个应用已经在等待终止，跳过重复的终止请求

### 5. 白名单机制

以下应用不会被屏蔽：
- **系统应用**：桌面启动器（Launcher）、系统 UI、输入法等
- **本应用自身**：专注助手自身不会被屏蔽
- **非前台应用**：只检测和屏蔽前台运行的应用

## 技术架构

```
用户操作
  ↓
MainActivity.startBlockingMode()
  ↓
启动 BlockerService（前台服务）
  ↓
设置 isServiceRunning = true
  ↓
BlockerAccessibilityService 检测到服务运行
  ↓
开始监听 TYPE_WINDOW_STATE_CHANGED 事件
  ↓
应用窗口打开 → 触发事件
  ↓
获取包名 → 检查是否在被屏蔽列表
  ↓
是 → forceStopApp() → 返回桌面 + 终止进程
否 → 允许正常打开
```

## 为什么选择这种方案？

1. **无需 root 权限**：使用无障碍服务是普通应用检测前台应用的唯一方式
2. **实时响应**：事件驱动机制可以立即响应应用打开
3. **双重保障**：事件检测 + 轮询检测，确保不遗漏
4. **系统兼容**：适用于 Android 5.0+ 所有设备
5. **用户可控**：用户可以随时关闭无障碍权限，停止屏蔽功能

## 限制说明

- **无法完全阻止启动**：应用可能在检测到之前短暂显示（通常 < 100 毫秒）
- **进程终止可能失败**：某些系统应用或受保护的应用可能无法被终止
- **需要无障碍权限**：必须授予无障碍服务权限才能工作
- **依赖系统响应**：返回桌面操作的执行速度取决于系统响应速度

## 核心代码位置

### BlockerAccessibilityService.java
- **位置**：`app/src/main/java/person/notfresh/selfcontrol/service/BlockerAccessibilityService.java`
- **职责**：监听应用窗口状态变化，检测被屏蔽的应用，执行屏蔽操作
- **关键方法**：
  - `onAccessibilityEvent()`：接收窗口状态变化事件
  - `handleWindowStateChange()`：处理窗口状态变化，判断是否屏蔽
  - `forceStopApp()`：执行屏蔽操作（返回桌面 + 终止进程）
  - `isBlockedApp()`：检查应用是否在被屏蔽列表中

### BlockerService.java
- **位置**：`app/src/main/java/person/notfresh/selfcontrol/service/BlockerService.java`
- **职责**：管理专注模式状态，提供状态标志给 AccessibilityService，记录专注时长
- **关键方法**：
  - `onStartCommand()`：启动服务，设置状态标志
  - `isRunning()`：静态方法，供 AccessibilityService 检查服务是否运行
  - `startTimer()`：启动定时器，更新专注时长

### MainActivity.java
- **位置**：`app/src/main/java/person/notfresh/selfcontrol/MainActivity.java`
- **职责**：用户界面，启动/停止专注模式，显示统计信息
- **关键方法**：
  - `startBlockingMode()`：启动专注模式，启动 BlockerService
  - `stopBlockingMode()`：停止专注模式，停止 BlockerService

## 被屏蔽应用列表配置

被屏蔽的应用列表定义在 `BlockerAccessibilityService.java` 中的 `BLOCKED_PACKAGES` 数组：

```java
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
```

要添加或删除被屏蔽的应用，只需修改此数组即可。
