FROM gradle:8.10-jdk22 AS build
WORKDIR /app

# We use `.dockerignore` to indicate what should and should not be copied.
COPY . .

# Add the Protobuf declarations.
RUN git clone https://github.com/jenspots/orchestrator-protobuf.git ./proto

# Create a fat jar for execution.
RUN gradle :rdfc-cli:shadowJar --console=plain --warning-mode all --no-daemon

FROM openjdk:23 AS production
WORKDIR /app

# Retrieve jar from the build stage.
COPY --from=build /app/rdfc-cli/build/libs/rdfc.jar rdfc.jar

# Execute the jar.
CMD java -jar rdfc.jar
