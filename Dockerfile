# ===== Build =====
FROM maven:3.9.7-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# ===== Run =====
FROM eclipse-temurin:21-jre
WORKDIR /app
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Australia/Melbourne"
ENV APP_TZ="Australia/Melbourne"
# Render 会注入 PORT（比如 10000+），程序需监听它
ENV PORT=8081
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8081
CMD ["java","-jar","/app/app.jar"]
