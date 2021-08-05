FROM openjdk:16-alpine

COPY build/libs/dp-doh.jar app.jar

USER guest
CMD ["java", "-jar", "app.jar"]