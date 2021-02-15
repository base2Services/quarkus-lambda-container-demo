# (1)
FROM  public.ecr.aws/lambda/java:11

ARG APP_NAME=quarkus-lambda-demo
ARG APP_VERSION=1.0-SNAPSHOT

# (2) Copies artifacts into /function directory
ADD ${APP_NAME}/target/${APP_NAME}-${APP_VERSION}-runner.jar /var/task/lib/${APP_NAME}.jar
ADD ${APP_NAME}/target/lib/  /var/task/lib/

# (3) Setting the command to the Quarkus lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]