
# `TYPE_WINDOW_STATE_CHANGED` 事件详解

### 基本定义

`TYPE_WINDOW_STATE_CHANGED` 是无障碍事件类型，表示窗口状态发生了改变。

### 触发时机

当以下情况发生时，系统会发送此事件：

#### 1. 应用启动并显示到前台
```
用户点击微信图标
  ↓
系统启动微信进程
  ↓
微信的Activity创建并显示
  ↓
系统发送 TYPE_WINDOW_STATE_CHANGED 事件
  ↓
你的 onAccessibilityEvent() 被调用
```

#### 2. Activity 切换
```
应用内部页面跳转也会触发
例如：微信主界面 → 聊天界面
```

#### 3. 应用从后台恢复
```
应用从后台切回前台时也会触发
```

#### 4. 对话框/弹窗显示
```
应用显示对话框、弹窗等也会触发
```

## 事件包含的信息

当收到 `TYPE_WINDOW_STATE_CHANGED` 事件时，可以通过 `AccessibilityEvent` 获取：

```java
AccessibilityEvent event = ...;

// 1. 包名（最重要！）
String packageName = event.getPackageName().toString();
// → "com.tencent.mm" (微信)

// 2. 类名
String className = event.getClassName().toString();
// → "com.tencent.mm.ui.LauncherUI" (微信主界面)

// 3. 事件类型
int eventType = event.getEventType();
// → AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED (值为 32)

// 4. 时间戳
long time = event.getEventTime();
// → 事件发生的时间（毫秒）
```

---

## 为什么选择这个事件？

### 优点

#### 1. 准确性高
```
窗口状态改变 = 应用真正显示到前台
这意味着：
- 应用已经完全启动
- 窗口已经创建并显示
- 用户能看到应用界面
```

#### 2. 实时性强
```
应用一显示到前台，立即触发
比轮询检查更及时
```

#### 3. 包含完整信息
```
事件中包含：
- 包名（知道是哪个应用）
- 类名（知道是哪个界面）
- 时间戳（知道什么时候）
```

### 与其他事件对比

| 事件类型 | 含义 | 是否适合检测应用打开 |
|---------|------|-------------------|
| `TYPE_WINDOW_STATE_CHANGED` | 窗口状态改变 | ✅ 最适合 |
| `TYPE_VIEW_CLICKED` | 视图被点击 | ❌ 太细粒度 |
| `TYPE_VIEW_FOCUSED` | 视图获得焦点 | ❌ 可能误判 |
| `TYPE_NOTIFICATION_STATE_CHANGED` | 通知状态改变 | ❌ 不相关 |

---

## 实际场景示例

### 场景1：用户打开微信

```
时间线：
T1: 用户点击微信图标
T2: 系统启动微信进程
T3: 微信的Activity创建
T4: 窗口显示到前台
    ↓
T5: 系统发送 TYPE_WINDOW_STATE_CHANGED 事件
    - packageName = "com.tencent.mm"
    - className = "com.tencent.mm.ui.LauncherUI"
    - eventType = TYPE_WINDOW_STATE_CHANGED
    ↓
T6: 你的 onAccessibilityEvent() 被调用
    ↓
T7: handleWindowStateChange() 执行
    - 检测到是微信
    - isBlockedApp("com.tencent.mm") = true
    - 立即调用 forceStopApp()
    ↓
T8: 微信被关闭，返回桌面
```

### 场景2：应用切换

```
用户从微信切换到QQ：
  ↓
微信窗口隐藏 → TYPE_WINDOW_STATE_CHANGED（微信）
QQ窗口显示 → TYPE_WINDOW_STATE_CHANGED（QQ）
```

### 场景3：应用恢复

```
QQ在后台，用户点击QQ图标：
  ↓
QQ从后台恢复
QQ窗口显示 → TYPE_WINDOW_STATE_CHANGED（QQ）
```

---

## 事件值常量

```java
// AccessibilityEvent 中的常量定义
public static final int TYPE_WINDOW_STATE_CHANGED = 32;

// 在你的代码中：
if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
    // 处理窗口状态改变事件
}
```

---

## 注意事项

### 1. 可能多次触发
```
一个应用启动可能触发多次事件：
- Activity创建
- 对话框显示
- 权限请求弹窗
等等
```

### 2. 系统UI也会触发
```
系统UI（如启动器、通知栏）也会触发
所以需要过滤：
- 跳过系统UI
- 跳过启动器
- 只处理普通应用
```

### 3. 时间精度
```
事件发送时间可能略有延迟（通常几毫秒到几十毫秒）
但比轮询检查快得多
```

---

## 总结

- `TYPE_WINDOW_STATE_CHANGED` = 窗口状态改变
- 触发时机：应用启动、切换、恢复、对话框显示等
- 用途：检测当前前台应用
- 优点：准确、实时、包含完整信息
- 在你的代码中：用于检测被屏蔽应用是否打开

这是检测应用打开最可靠的方式。