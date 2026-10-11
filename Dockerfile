FROM europe-north1-docker.pkg.dev/cgr-nav/pull-through/nav.no/jre:openjdk-27@sha256:800806e884ee83be8995c5f134b076798ecfddc183f13f42994a3c71786e3d01

ENV LANG='nb_NO.UTF-8' LANGUAGE='nb_NO:nb' LC_ALL='nb_NO.UTF-8' TZ="Europe/Oslo"

COPY build/libs/* /app.jar

ENTRYPOINT ["java", "-jar", "/app.jar"]