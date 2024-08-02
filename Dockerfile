# 1단계: 빌드 단계
FROM gradle:jdk17-alpine AS build

# 빌드 컨텍스트의 모든 내용을 작업 디렉토리에 복사합니다.
WORKDIR /app
COPY . .

# Gradle을 사용하여 프로젝트를 빌드합니다.
RUN gradle clean build -x test

# 2단계: 실행 단계
FROM amazoncorretto:17-alpine

# JAR 파일을 실행할 작업 디렉토리 설정
WORKDIR /app

# 빌드 단계에서 생성된 JAR 파일을 가져옵니다.
COPY --from=build /app/build/libs/fcfs-0.0.1-SNAPSHOT app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-jar", "/app/app.jar"]