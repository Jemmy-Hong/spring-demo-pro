# ==============================================================================
# Dockerfile —— Spring Boot 后端镜像构建 (Ubuntu 多阶段构建)
# 端口: 9010 (与 application.yml server.port、deployment containerPort、service targetPort 一致)
# 多环境: 配置文件在 src/main/resources/ 下，已由Maven打包进jar，
#         运行时靠 SPRING_PROFILES_ACTIVE 自动选择，无需额外COPY配置
# 架构: 多阶段 = 阶段1编译(Maven+JDK) → 阶段2运行(精简JRE)，体积小、更安全
# JVM: 通过 JAVA_TOOL_OPTIONS 传参，可被 K8s deployment env 覆盖
# 构建方式: 必须在【项目根目录】构建，指定本文件路径:
#   docker build -f deploy/docker/Dockerfile -t <image>:<tag> .
# ==============================================================================

# ================ 阶段1: 构建阶段 (编译打包 jar) ================
# 使用官方 Maven + JDK17 镜像做构建环境
# 用固定tag不用latest，保证可复现
FROM maven:3.9-eclipse-temurin-17 AS builder

# 工作目录，后续命令都在这个目录执行
WORKDIR /build

# 把整个项目源码(含pom.xml和src)复制进构建目录
# 注意: 构建上下文=项目根目录，所以能访问到 pom.xml 和 src/
COPY . /build

# 用Maven打包，跳过测试(单测在CI单独跑)
# 产物在 /build/target/*.jar; src/main/resources 里的application*.yml已打进jar
RUN mvn clean package -DskipTests

# ================ 阶段2: 运行阶段 (精简镜像) ================
# 用官方 Ubuntu + JRE17 运行时镜像，比JDK更小
FROM eclipse-temurin:17-jre

# 设置时区环境变量(新加坡, 对应 ap-southeast-1 区域)
ENV TZ=Asia/Singapore

# 把系统时区软链接到TZ指定时区并写入timezone文件，保证容器内时间正确
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 创建非root运行用户(生产安全最佳实践，配合k8s runAsNonRoot)
# Ubuntu镜像默认root运行，创建app用户降低容器逃逸风险
RUN groupadd --system app && useradd --system --gid app app

# 从阶段1的 /build/target/ 复制打包好的jar到运行镜像
# 配置文件已在jar内，不需要单独COPY application*.yml
COPY --from=builder /build/target/*.jar /usr/app/app.jar

# 给运行目录设置属主为app用户(否则非root运行时可能无法写临时文件)
RUN mkdir -p /usr/app && chown -R app:app /usr/app

# ==================== JVM 参数: 可被外部覆盖 ====================
# 用 JAVA_TOOL_OPTIONS 环境变量传JVM参数，K8s deployment里能通过env覆盖，不必重建镜像
ENV JAVA_TOOL_OPTIONS="-Xmx256m -Xms256m -Xmn128m -XX:NewRatio=4 -XX:SurvivorRatio=4"

# 声明容器内应用监听端口(与application.yml server.port=9010一致)
EXPOSE 9010

# 切换到非root用户运行
USER app:app

# 容器启动命令(exec数组形式，能正确接收SIGTERM配合优雅停机)
# 不写死JVM参数，JVM参数统一走 JAVA_TOOL_OPTIONS，由deployment控制
CMD ["java", "-jar", "/usr/app/app.jar"]