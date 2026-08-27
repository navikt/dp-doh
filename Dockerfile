FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-21@sha256:7090031b4682ffa879328f78185aa2d480775a1e5c7c147e6946429a4e30b6fd

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/* /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]