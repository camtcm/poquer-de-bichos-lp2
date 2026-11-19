FROM eclipse-temurin:21-jdk AS construccion
WORKDIR /app
RUN curl -fsSL https://github.com/sbt/sbt/releases/download/v1.10.11/sbt-1.10.11.tgz | tar -xz -C /opt
ENV PATH="/opt/sbt/bin:${PATH}"
COPY build.sbt ./
COPY project/build.properties project/plugins.sbt project/
RUN sbt -batch update
COPY src src
RUN sbt -batch assembly

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=construccion /app/target/scala-3.3.6/poquer-de-bichos.jar juego.jar
COPY web web
ENV CARPETA_WEB=web
EXPOSE 8080
CMD ["java", "-XX:MaxRAMPercentage=70", "-jar", "juego.jar"]
