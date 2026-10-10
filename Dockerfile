# ---- Build ----
FROM gradle:8-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle bootJar -x test --no-daemon && \
    ls build/libs && \
    cp $(ls build/libs/*.jar | grep -v -- '-plain.jar' | head -1) /app/app.jar

# ---- Run ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar
EXPOSE 8080
CMD ["sh", "-c", "java -Xmx380m -XX:+UseSerialGC -jar app.jar --server.port=${PORT:-8080}"]