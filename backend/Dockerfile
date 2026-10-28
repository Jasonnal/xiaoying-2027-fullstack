# 多阶段构建：编译产物与运行时分离，最终镜像只有 JRE + jar
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先只拷 pom，利用镜像层缓存依赖
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre

LABEL org.opencontainers.image.title="material-studio-backend" \
      org.opencontainers.image.description="素材制作网站后端（Spring Boot + MyBatis + MySQL）"

WORKDIR /app

# 以非 root 运行；storage 是上传素材与结果图的落盘目录（compose 里挂数据卷）
RUN groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /app/storage && chown -R app:app /app

COPY --from=build --chown=app:app /build/target/material-studio-1.0.0.jar /app/app.jar

USER app
EXPOSE 8080
ENV JAVA_OPTS="-XX:MaxRAMPercentage=70"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
