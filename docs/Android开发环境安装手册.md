# Android 开发环境安装手册

> 安装日期: 2026-03-12  
> 目标系统: CentOS 7 (x86_64)  
> 安装目录: /opt/android

---

## 目录

1. [系统要求](#1-系统要求)
2. [安装目录结构](#2-安装目录结构)
3. [安装步骤](#3-安装步骤)
4. [环境变量配置](#4-环境变量配置)
5. [验证安装](#5-验证安装)
6. [已安装组件清单](#6-已安装组件清单)
7. [常用命令](#7-常用命令)
8. [故障排除](#8-故障排除)

---

## 1. 系统要求

| 要求项 | 说明 |
|--------|------|
| 操作系统 | CentOS 7 / RHEL 7 或兼容系统 |
| 架构 | x86_64 |
| JDK | JDK 17+ (推荐 JDK 21) |
| 磁盘空间 | 至少 5GB 可用空间 |
| 网络要求 | 需要访问 dl.google.com |

### 检查 Java 环境

```bash
java -version
```

预期输出:
```
java version "21.0.5" LTS
```

---

## 2. 安装目录结构

```
/opt/android/
├── sdk/                          # Android SDK
│   ├── cmdline-tools/latest/     # SDK 命令行工具
│   ├── platform-tools/           # adb, fastboot 等工具
│   ├── build-tools/34.0.0/       # 构建工具 (aapt2, d8 等)
│   └── platforms/android-34/     # Android 34 平台
└── ndk/                          # Android NDK
    ├── build/                    # NDK 构建系统
    ├── toolchains/               # 交叉编译工具链
    └── sources/                  # NDK 源码
```

---

## 3. 安装步骤

### 3.1 创建目录结构

```bash
mkdir -p /opt/android/sdk /opt/android/ndk
```

### 3.2 下载并安装 SDK Command-line Tools

```bash
cd /opt/android

# 下载 Command-line Tools
curl -L -o cmdline-tools.zip "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"

# 解压并配置目录结构
unzip -q cmdline-tools.zip
mkdir -p sdk/cmdline-tools
mv cmdline-tools sdk/cmdline-tools/latest
rm cmdline-tools.zip
```

### 3.3 接受 SDK 许可证

```bash
export ANDROID_HOME=/opt/android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

yes | sdkmanager --licenses
```

### 3.4 安装 SDK 组件

```bash
sdkmanager "platform-tools" "build-tools;34.0.0" "platforms;android-34"
```

### 3.5 下载并安装 NDK

```bash
cd /opt/android

# 下载 NDK r26d
curl -L -o ndk.zip "https://dl.google.com/android/repository/android-ndk-r26d-linux.zip"

# 解压
unzip -q ndk.zip

# 重命名目录
mv android-ndk-r26d ndk
rm ndk.zip
```

---

## 4. 环境变量配置

### 4.1 创建环境变量脚本

创建文件 `/etc/profile.d/android.sh`:

```bash
cat > /etc/profile.d/android.sh << 'EOF'
# Android SDK & NDK Environment
export ANDROID_HOME=/opt/android/sdk
export ANDROID_NDK_HOME=/opt/android/ndk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/build-tools/34.0.0
export PATH=$PATH:$ANDROID_NDK_HOME
EOF

chmod +x /etc/profile.d/android.sh
```

### 4.2 加载环境变量

**方式一**: 新开终端 (自动加载)

**方式二**: 当前终端手动加载
```bash
source /etc/profile.d/android.sh
```

---

## 5. 验证安装

### 5.1 验证环境变量

```bash
echo $ANDROID_HOME
# 输出: /opt/android/sdk

echo $ANDROID_NDK_HOME
# 输出: /opt/android/ndk
```

### 5.2 验证 SDK 工具

```bash
# sdkmanager 版本
sdkmanager --version
# 输出: 12.0

# adb 版本
adb --version
# 输出: Android Debug Bridge version 1.0.41
#       Version 37.0.0

# aapt2 版本
aapt2 version
# 输出: Android Asset Packaging Tool (aapt) 2.19-10229193

# d8 版本
d8 --version
# 输出: D8 8.2.2-dev
```

### 5.3 验证 NDK

```bash
cat $ANDROID_NDK_HOME/source.properties
# 输出:
# Pkg.Desc = Android NDK
# Pkg.Revision = 26.3.11579264
# Pkg.ReleaseName = r26d
```

### 5.4 查看已安装的 SDK 包

```bash
sdkmanager --list_installed
```

预期输出:
```
Installed packages:
  Path                 | Version | Description                | Location            
  -------              | ------- | -------                    | -------             
  build-tools;34.0.0   | 34.0.0  | Android SDK Build-Tools 34 | build-tools/34.0.0  
  platform-tools       | 37.0.0  | Android SDK Platform-Tools | platform-tools      
  platforms;android-34 | 3       | Android SDK Platform 34    | platforms/android-34
```

---

## 6. 已安装组件清单

| 组件 | 版本 | 说明 |
|------|------|------|
| SDK Command-line Tools | 12.0 | SDK 管理工具 |
| Platform Tools | 37.0.0 | adb, fastboot, sqlite3 等 |
| Build Tools | 34.0.0 | aapt2, d8, zipalign 等 |
| Android Platform | 34 (API 34) | Android 14 SDK |
| NDK | r26d (26.3.11579264) | Native 开发工具包 |

---

## 7. 常用命令

### SDK 管理

```bash
# 列出所有可用包
sdkmanager --list

# 安装额外组件
sdkmanager "build-tools;35.0.0"
sdkmanager "platforms;android-35"

# 更新所有已安装包
sdkmanager --update

# 卸载包
sdkmanager --uninstall "build-tools;33.0.0"
```

### ADB 调试

```bash
# 列出已连接设备
adb devices

# 安装 APK
adb install app.apk

# 推送文件到设备
adb push local.txt /sdcard/

# 从设备拉取文件
adb pull /sdcard/file.txt ./

# 进入 shell
adb shell
```

### NDK 编译

```bash
# 使用 ndk-build
cd <project>/jni
ndk-build

# 使用 CMake (推荐)
cmake -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK_HOME/build/cmake/android.toolchain.cmake \
      -DANDROID_ABI=arm64-v8a \
      -DANDROID_PLATFORM=android-34 \
      ..
```

---

## 8. 故障排除

### 问题 1: sdkmanager 找不到 Java

**错误信息**:
```
ERROR: JAVA_HOME is not set and no 'java' command could be found
```

**解决方案**:
```bash
# 设置 JAVA_HOME
export JAVA_HOME=/usr/java/jdk-21
export PATH=$JAVA_HOME/bin:$PATH
```

### 问题 2: 许可证未接受

**错误信息**:
```
License for package <package> not accepted
```

**解决方案**:
```bash
yes | sdkmanager --licenses
```

### 问题 3: 下载超时

**解决方案**:
```bash
# 使用代理或镜像
sdkmanager --proxy=http --proxy_host=<proxy_host> --proxy_port=<proxy_port> --list
```

### 问题 4: 权限问题

**解决方案**:
```bash
# 确保 /opt/android 目录权限正确
chown -R root:root /opt/android
chmod -R 755 /opt/android
```

---

## 附录: 下载链接

| 组件 | 下载地址 |
|------|----------|
| Command-line Tools | https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip |
| NDK r26d | https://dl.google.com/android/repository/android-ndk-r26d-linux.zip |
| Platform Tools | https://developer.android.com/studio/releases/platform-tools |

---

*文档生成于 2026-03-12*
