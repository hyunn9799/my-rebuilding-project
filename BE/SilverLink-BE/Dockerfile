# 1. 자바 21 버전이 설치된 리눅스 컴퓨터를 베이스로 씁니다.
FROM amazoncorretto:21

# 2. 내 컴퓨터에서 만든 jar 파일을 컨테이너 안으로 복사합니다.
# (build/libs 폴더 안에 있는 -SNAPSHOT.jar 로 끝나는 파일을 app.jar라는 이름으로 복사)
COPY build/libs/*-SNAPSHOT.jar app.jar

# 3. 서버를 실행할 명령어를 지정합니다.
ENTRYPOINT ["java", "-jar", "/app.jar"]