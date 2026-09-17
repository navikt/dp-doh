FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-26@sha256:b9721c46127e3f2ea88e3cbb68fd15b65caf9acd0849a8a0aa563b0c83611e6e

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/* /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]