# (1)
FROM  public.ecr.aws/lambda/java:8.al2


#2 Install lambda runtime emulator - used for testing locally in docker
# See https://github.com/aws/aws-lambda-runtime-interface-emulator/
RUN yum install -y unzip && \
  curl -Lo /aws-lambda-rie https://github.com/aws/aws-lambda-runtime-interface-emulator/releases/latest/download/aws-lambda-rie && \
  chmod +x /aws-lambda-rie

#3 Copies artifacts into /function directory
ADD target/quarkus-lambda-demo-1.0-SNAPSHOT-runner.jar /var/task/quarkus-lambda-demo.jar
ADD target/lib/ /var/task/

#4 lambda runtime entrypoint for testing
ENTRYPOINT [ "/aws-lambda-rie" ]

#5 wrap the execution of the quarkus lambda runtime in the AWS lambda runtime
CMD ["/var/lang/bin/java", "-cp", "./*", "com.amazonaws.services.lambda.runtime.api.client.AWSLambda", "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]