# 开发环境

## 1. 构建工具

| 工具          | 版本                                                                            | 说明                                |
|-------------|-------------------------------------------------------------------------------|-----------------------------------|
| Gradle      | **9.3.1**                                                                     | 通过 `gradle-wrapper.properties` 锁定 |
| Gradle 分发地址 | `https://mirrors.aliyun.com/gradle/distributions/v9.3.1/gradle-9.3.1-bin.zip` | 阿里云镜像                             |

## 2. JDK

> ⚠️ **必须使用 JDK 21 进行构建和测试**。系统默认 JDK（如 Zulu 11）不满足项目要求。

| 配置项       | 值                                                                                                 |
|-----------|---------------------------------------------------------------------------------------------------|
| 要求版本      | **Java 21**（class file version 65.0）                                                              |
| 推荐发行版     | JBR-21.0.8（JetBrains Runtime）                                                                     |
| Gradle 配置 | `org.gradle.java.home=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home` |

### 验证

```bash
./gradlew --version
```

输出中 `JVM` 一行应显示 **21** 或更高版本。

## 3. Gradle 配置

```properties
# gradle.properties
org.gradle.java.home=/Users/bytedance/Library/Java/JavaVirtualMachines/jbr-21.0.8/Contents/Home
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

## 4. IDE 建议

- **Android Studio** / **IntelliJ IDEA**：将项目的 Gradle JDK 指向 JBR-21.0.8
- **命令行**：确保 `./gradlew` 使用的 JDK 为 21（已通过 `gradle.properties` 固定）
