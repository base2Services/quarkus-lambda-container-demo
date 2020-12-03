# quarkus-lambda-container-demo

This demo show how you can packages a Quarkus Lambda App using the new [Lambda containers](https://docs.aws.amazon.com/lambda/latest/dg/lambda-images.html) support


## Prerequisites
To complete this guide, you need:

* less than 30 minutes
* JDK 11 (AWS requires JDK 1.8 or 11)
* Apache Maven 3.6.2+
* An Amazon AWS account
* AWS CLI
* docker

## So Lets get started


## Creating the Maven Deployment Project

First we are going to create a basic quarkus lambda app using the maven archetype

These steps are taken from the [quarkus lambda guide](https://quarkus.io/guides/amazon-lambda#creating-the-maven-deployment-project)

```bash
mvn archetype:generate \
       -DarchetypeGroupId=io.quarkus \
       -DarchetypeArtifactId=quarkus-amazon-lambda-archetype \
       -DarchetypeVersion=1.10.2.Final
```

This will have created a standard quarkus lambda app in a directory based on maven artifactId you entered. I will use `quarkus-lambda-demo` for the rest of the guide

### Add the AWS Lambda Runtime Editor Dependency

Add the following dependency to the maven pom dependencies

```xml
....
        <dependency>
            <groupId>com.amazonaws</groupId>
            <artifactId>aws-lambda-java-runtime-interface-client</artifactId>
            <version>1.0.0</version>
        </dependency>
....
```

## Build the demo

In the quarkus-lambda-demo run maven

```bash
$ mvn clean install
...
INFO] --- maven-install-plugin:2.4:install (default-install) @ quarkus-lambda-demo ---
[INFO] Installing /Users/aaronwalker/Workspaces/aaronwalker/quarkus-lambda-container-demo/quarkus-lambda-demo/target/quarkus-lambda-demo-1.0-SNAPSHOT.jar to /Users/aaronwalker/.m2/repository/com/base2services/quarkus-lambda-demo/1.0-SNAPSHOT/quarkus-lambda-demo-1.0-SNAPSHOT.jar
[INFO] Installing /Users/aaronwalker/Workspaces/aaronwalker/quarkus-lambda-container-demo/quarkus-lambda-demo/pom.xml to /Users/aaronwalker/.m2/repository/com/base2services/quarkus-lambda-demo/1.0-SNAPSHOT/quarkus-lambda-demo-1.0-SNAPSHOT.pom
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  13.732 s
[INFO] Finished at: 2020-12-03T15:10:39+01:00
```

This will create the required artifacts in the target directory

## Lambda Function

The quarkus-lambda-demo project has by default configured the test handler in the `quarkus-lambda-demo/src/main/resources/application.properties/ and we will use this handler for the demo

```
quarkus.lambda.handler=test
```

## Building the Dockerfile

Create a Dockerfile in the quarkus-lambda-demo using the `public.ecr.aws/lambda/java:8.al2` lambda java8 Amazon Linux 2 runtime container as the base

```Dockerfile
# (1)
FROM  public.ecr.aws/lambda/java:8.al2


WORKDIR /function

#2 Install lambda runtime emulator - used for testing locally in docker
# See https://github.com/aws/aws-lambda-runtime-interface-emulator/
RUN yum install -y unzip && \
  curl -Lo /aws-lambda-rie https://github.com/aws/aws-lambda-runtime-interface-emulator/releases/latest/download/aws-lambda-rie && \
  chmod +x /aws-lambda-rie

#3 Copies artifacts into /function directory
ADD target/quarkus-lambda-demo-1.0-SNAPSHOT-runner.jar /function/quarkus-lambda-demo.jar
ADD target/classes/application.properties /function/application.properties 
ADD target/lib/ /function/

#4 lambda runtime entrypoint for testing
ENTRYPOINT [ "/aws-lambda-rie" ]

#5 wrap the execution of the quarkus lambda runtime in the AWS lambda runtime
CMD ["/var/lang/bin/java", "-cp", "./*", "com.amazonaws.services.lambda.runtime.api.client.AWSLambda", "io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest", "quarkus.lambda.handler=test"]

```
1) Details about the lambda containers images can be found at https://docs.aws.amazon.com/lambda/latest/dg/images-create.html

### Then build the docker image

```bash
$ docker build -t quarkus/lambda-demo .
....
Successfully built 09666b8a56b0
Successfully tagged quarkus/lambda-demo:latest
```

### Testing local using docker

You can now use this image to test the lambda execution locally using

```bash
$ docker run --rm -it -p 9000:8080 quarkus/lambda-demo:latest
....
INFO[0000] exec '/var/lang/bin/java' (cwd=/function, handler=io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest) 
```

This starts the AWS lambda runtime emulator and a web server listen locally on port 9000. You can test the test lambda handler using curl

```bash
curl -XPOST "http://localhost:9000/2015-03-31/functions/function/invocations" -d '{"greeting":"herzlich willkommen", "name":"aaron"}'
....
{"result":"herzlich willkommen aaron","requestId":"d8a48f84-a166-429e-a8ec-d8bea2e7087c"}
```

## Push the container to ECR

In order to be able to create a lambda function from our container image we need to push it to a registry. I will use ECR in the guide

*** Assumes you have valid AWS credentials configured

### Create the ECR registry

```bash
$ aws ecr create-repository --repository-name quarkus/lambda-demo --region eu-central-1
....
{
    "repository": {
        "repositoryArn": "arn:aws:ecr:eu-central-1:<aws-accountid>:repository/quarkus/lambda-demo",
        "registryId": "<aws-accountid>",
        "repositoryName": "quarkus/lambda-demo",
        "repositoryUri": "<aws-accountid>.dkr.ecr.eu-central-1.amazonaws.com/quarkus/lambda-demo",
        "createdAt": "2020-12-03T16:10:37+01:00",
        "imageTagMutability": "MUTABLE",
        "imageScanningConfiguration": {
            "scanOnPush": false
        },
        "encryptionConfiguration": {
            "encryptionType": "AES256"
        }
    
```

### Tag and push the container image to ECR

```bash
$ aws ecr get-login-password --region eu-central-1 | docker login --username AWS --password-stdin <aws-accountid>.dkr.ecr.eu-central-1.amazonaws.com
$ docker tag quarkus/lambda-demo  <aws-accountid>.dkr.ecr.eu-central-1.amazonaws.com/quarkus/lambda-demo
$ docker push <aws-accountid>.dkr.ecr.eu-central-1.amazonaws.com/quarkus/lambda-demo
....
The push refers to repository [<aws-accountid>.dkr.ecr.eu-central-1.amazonaws.com/quarkus/lambda-demo]
```

### Create the Lambda function

```bash 
$ aws cloudformation deploy \
  --stack-name quarkus-lambda-demo \
  --template-file sam.container.yaml \
  --parameter-overrides ImageUri=<aws-accountid>.dkr.ecr.eu-central-1.amazonaws.com/quarkus/lambda-demo:latest \
  --capabilities CAPABILITY_IAM
```