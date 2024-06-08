FROM gradle:8.8.0-jdk17-alpine as build

WORKDIR /app

# All files which may not be copied must be specified in the .dockerignore
# file.
COPY . ./

RUN gradle build -x test

FROM amazoncorretto:22.0.1-alpine3.19

# Set the working directory
WORKDIR /app

# Copy the JAR file from the build stage
COPY --from=build "/app/build/libs/technology.idlab.jvm-runner-[0-9\.]*-all.jar" runner.jar

# Set the entry point to run the application
ENTRYPOINT ["java", "-jar", "runner.jar"]
