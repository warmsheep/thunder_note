# Windows Android 模拟器远程连接配置指南

## 文档定位
本文档描述如何在 Windows 环境安装 Android 模拟器，并配置 ADB 远程连接，供 CI/CD 或远程开发环境使用。

## 前置要求
- Windows 10/11 64位
- 至少 8GB 内存
- BIOS 中开启虚拟化（VT-x / AMD-V）

---

## 步骤 1：安装 JDK 17

1. 下载：https://adoptium.net/temurin/releases/?version=17
   - 选择 `Windows x64` → `.msi` 安装包
   
2. 安装后验证：
   ```cmd
   java -version
   ```

3. 设置环境变量（如果未自动设置）：
   - `JAVA_HOME` = `C:\Program Files\Eclipse Adoptium\jdk-17.x.x`
   - `Path` 添加 `%JAVA_HOME%\bin`

---

## 步骤 2：安装 Android SDK Command-line Tools

1. 创建目录：
   ```
   C:\Android\Sdk
   ```

2. 下载：https://developer.android.com/studio#command-tools
   - 点击 "Command line tools only"
   - 解压到 `C:\Android\Sdk\cmdline-tools\latest\`

3. 设置环境变量：
   - `ANDROID_HOME` = `C:\Android\Sdk`
   - `ANDROID_SDK_ROOT` = `C:\Android\Sdk`
   - `Path` 添加：
     - `%ANDROID_HOME%\cmdline-tools\latest\bin`
     - `%ANDROID_HOME%\platform-tools`
     - `%ANDROID_HOME%\emulator`

---

## 步骤 3：安装 SDK 组件

打开 **管理员权限的 PowerShell**，执行：

```powershell
cd C:\Android\Sdk\cmdline-tools\latest\bin

# 接受许可
.\sdkmanager --licenses

# 安装必要组件
.\sdkmanager "platform-tools" "build-tools;34.0.0" "platforms;android-34" "emulator"

# 安装系统镜像
.\sdkmanager "system-images;android-34;google_apis;x86_64"
```

---

## 步骤 4：创建 AVD 模拟器

```powershell
# 创建 AVD
.\avdmanager create avd -n thunder_test -k "system-images;android-34;google_apis;x86_64" -d "pixel_6"

# 输入 no 然后回车
```

---

## 步骤 5：启动模拟器

**方式 A：命令行启动**
```powershell
cd C:\Android\Sdk\emulator
.\emulator -avd thunder_test
```

**方式 B：创建快捷方式**
1. 右键桌面 → 新建快捷方式
2. 位置填：`C:\Android\Sdk\emulator\emulator.exe -avd thunder_test`
3. 命名为 "Thunder Test Emulator"

---

## 步骤 6：配置远程 ADB 连接

### 6.1 开启 TCP/IP 模式

在 Windows 模拟器机器上执行：

```powershell
# 1. 进入 platform-tools 目录
cd C:\Android\Sdk\platform-tools

# 2. 确认设备已连接
.\adb devices

# 3. 开启 TCP/IP 模式（端口 5555）
.\adb tcpip 5555

# 4. 查看本机 IP
ipconfig
# 记下 IPv4 地址，例如 192.168.1.100
```

### 6.2 配置防火墙

**方式一：PowerShell（管理员）**
```powershell
New-NetFirewallRule -DisplayName "ADB 5555" -Direction Inbound -LocalPort 5555 -Protocol TCP -Action Allow
```

**方式二：手动配置**
1. 控制面板 → Windows Defender 防火墙 → 高级设置
2. 入站规则 → 新建规则 → 端口 → TCP 5555 → 允许连接

---

## 步骤 7：从远程机器连接

### 7.1 Linux 服务器连接

```bash
# 连接 Windows 模拟器
adb connect 192.168.1.100:5555

# 确认连接
adb devices
# 应显示：192.168.1.100:5555   device

# 运行 Android 测试
cd /data/workspace/thunder_note/thunder-note-android
./gradlew connectedAndroidTest
```

### 7.2 SSH 端口转发（可选）

如果需要通过跳板机连接：

```bash
# 本地转发 ADB 端口
ssh -L 5555:localhost:5555 user@跳板机IP

# 连接本地转发端口
adb connect localhost:5555
```

---

## 常用命令参考

| 操作 | 命令 |
|------|------|
| 启动模拟器 | `emulator -avd thunder_test` |
| 查看 ADB 设备 | `adb devices` |
| 开启远程端口 | `adb tcpip 5555` |
| 远程连接 | `adb connect <IP>:5555` |
| 断开连接 | `adb disconnect <IP>:5555` |
| 查看已连接设备 | `adb devices -l` |

---

## 常见问题

### Q: 模拟器启动失败，提示 HAXM 错误
- 进入 BIOS 开启 VT-x 虚拟化
- 或使用 Hyper-V：`bcdedit /set hypervisorlaunchtype auto`

### Q: 连接被拒绝
- 检查 Windows 防火墙是否放行 5555 端口
- 确认 `adb tcpip 5555` 已执行
- 检查网络是否可达（ping 测试）

### Q: 模拟器重启后无法连接
- 重新执行 `adb tcpip 5555`
- 可考虑创建启动脚本自动执行

### Q: ADB 连接超时
- 检查 Windows 防火墙入站规则
- 确认两台机器在同一网段或路由可达
- 尝试关闭 Windows 防火墙测试

---

## 自动化脚本（可选）

### Windows 启动脚本 `start_emulator.bat`

```batch
@echo off
cd C:\Android\Sdk\emulator
start emulator -avd thunder_test

timeout /t 30

cd C:\Android\Sdk\platform-tools
adb tcpip 5555

echo Emulator started, ADB listening on port 5555
echo Your IP:
ipconfig | findstr /i "IPv4"
pause
```

### 自动启动配置

1. 将 `start_emulator.bat` 放入 Windows 启动文件夹：
   - 按 `Win+R`，输入 `shell:startup`
   - 复制脚本到该文件夹

2. 或创建计划任务：
   - 任务计划程序 → 创建基本任务
   - 触发器：系统启动时
   - 操作：启动程序 `start_emulator.bat`
