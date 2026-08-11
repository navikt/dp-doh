FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:7e54b85b5d9bb53b33d17584d6007b2cf27acdcf7d55da1cfc0570c4c347e821

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/* /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]