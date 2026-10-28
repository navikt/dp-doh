FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-27@sha256:a2ab7b89449f5be81a68f128291313d4af289833dd96ffbdb787c49ab1c63722

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/* /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]