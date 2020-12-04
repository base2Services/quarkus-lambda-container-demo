# (1)
FROM  public.ecr.aws/lambda/java:8.al2

ARG APP_NAME=quarkus-lambda-demo
ARG APP_VERSION=1.0-SNAPSHOT

#3 Copies artifacts into /function directory
ADD ${APP_NAME}/target/${APP_NAME}-${APP_VERSION}-runner.jar /var/task/lib/${APP_NAME}.jar
ADD ${APP_NAME}/target/lib/  /var/task/lib/


CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]