FROM amazoncorretto:17.0.12
ARG JAR_FILE=build/libs/fcfs-0.0.1-SNAPSHOT.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]