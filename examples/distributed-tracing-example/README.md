# AWS Lambda distributed tracing example
 
Example application demonstrating common use cases of the New Relic AWS Lambda OpenTracing instrumentation:  
* Distributed tracing between Lambda functions
* Manual span creation
* Tracing external calls
* Adding custom attributes (aka Tags) to spans

## Project structure

This project contains source code and supporting files for serverless applications that you can deploy with the AWS Serverless Application Model (SAM) CLI.

It includes the following files and folders:

- `DTCallerFunction/src/main` - Lambda function that calls the DTCalleeFunction lambda function through an API Gateway HTTP request.
- `DTCalleeFunction/src/main` - Lambda function that can be invoked by another lambda.
- `events/request-input.json` - Invocation event representing an API Gateway HTTP request that you can use to invoke the functions locally.
- `template.yaml` - A template that defines the application's AWS resources.
- `*/src/main/resources/log4j.properties` - A configuration file for the log appender.

The application uses AWS resources, namely Lambda functions, that are defined in the `template.yaml` file in this project. You can update the template to add AWS resources through the same deployment process that updates your application code.

If you prefer to use an integrated development environment (IDE) to build and test your application, you can use the AWS Toolkit.  

The AWS Toolkit is an open source plug-in for popular IDEs that uses the SAM CLI to build and deploy serverless applications on AWS. The AWS Toolkit also adds a simplified step-through debugging experience for Lambda function code. See the following links to get started.

* [PyCharm](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [IntelliJ](https://docs.aws.amazon.com/toolkit-for-jetbrains/latest/userguide/welcome.html)
* [VS Code](https://docs.aws.amazon.com/toolkit-for-vscode/latest/userguide/welcome.html)
* [Visual Studio](https://docs.aws.amazon.com/toolkit-for-visual-studio/latest/user-guide/welcome.html)

## Usage/Installation

For full details on using the OpenTracing AWS Lambda SDK and Tracer please refer to the [main project README](../../README.md). This README supplements the main project README by documenting details specific to the example application such as using the SAM CLI.

## Intellij  
### Import Gradle projects    

When opening the project in Intellij for the first time you will likely need to right click the `build.gradle` file in each Lambda function directory and import each as a gradle project.

### Lambda function run configs  

To run the `DTCallerFunction` and `DTCalleeFunction` locally using the AWS Toolkit Intellij plugin, the run configurations for each function should be configured with your AWS Connection credentials, the specified environment variables/values, and use the provided `events/request-input.json` test event as follows:

DTCallerFunction Run Config     
![dt-caller-function-run-config](readme-resources/dt-caller-function-run-config.png?raw=true "DTCallerFunction Run Config")  

DTCalleeFunction Run Config     
![dt-callee-function-run-config](readme-resources/dt-callee-function-run-config.png?raw=true "DTCalleeFunction Run Config")  


## SAM CLI
### Deploy the sample application

The Serverless Application Model Command Line Interface (SAM CLI) is an extension of the AWS CLI that adds functionality for building and testing Lambda applications. It uses Docker to run your functions in an Amazon Linux environment that matches Lambda. It can also emulate your application's build environment and API.

To use the SAM CLI, you need the following tools.

* AWS CLI - [Install the AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-install.html) and [configure it with your AWS credentials](https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-quickstart.html).
* SAM CLI - [Install the SAM CLI](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-install.html)
* Java 8 SDK
* Docker - [Install Docker community edition](https://hub.docker.com/search/?type=edition&offering=community)

The SAM CLI uses an Amazon S3 bucket to store your application's deployment artifacts. If you don't have a bucket suitable for this purpose, create one. Replace `BUCKET_NAME` in the commands in this section with a unique bucket name.

```bash
aws s3 mb s3://BUCKET_NAME
```

Build your application with the `sam build` command.

```bash
sam build
```

To prepare the application for deployment, use the `sam package` command.

```bash
sam package --output-template-file packaged.yaml --s3-bucket BUCKET_NAME
``` 

The SAM CLI creates deployment packages for the Lambda function code, uploads them to the S3 bucket, and creates a new version of the template that refers to the artifacts in the bucket. The generated `packaged.yaml` file contains the IDs and resource URIs to utilize the artifacts that are uploaded to S3. The Lambda service loads code only from S3 buckets in the same region as the function. 

To deploy the application, use the `sam deploy` command.

```bash
sam deploy --template-file packaged.yaml --stack-name NewRelicExample-JavaDT --capabilities CAPABILITY_IAM
```

This will deploy the CloudFormation template to your AWS account, in your default region. At the end of the process, you should see a section like this:

    Outputs
    -------------------------------------------------------------------------------------------------
    Key                 CallerEndpoint
    Description         New Relic DT example testing stage caller endpoint
    Value               https://abcde12345.execute-api.us-east-1.amazonaws.com/testing/dt-caller

**Note:** Your URL will be different. A GET request to this URL (e.g. using a browser, or `curl`) will invoke the "Caller" Lambda function, which will invoke the "Callee" function in turn.

After deployment is complete you can run the following command to view the Stack:

```bash
aws cloudformation describe-stacks --stack-name NewRelicExample-JavaDT
``` 

### Use the SAM CLI to build and test locally

Build your application with the `sam build` command.

```bash
sam build
```

The SAM CLI installs dependencies defined in `DT*Function/build.gradle`, creates a deployment package, and saves it in the `.aws-sam/build` folder.

Test a single function by invoking it directly with a test event. An event is a JSON document that represents the input that the function receives from the event source. 

Run functions locally and invoke them with the `sam local invoke` command.

```bash
sam local invoke DTCallerFunction --event events/event.json
```

The SAM CLI can also emulate your application's API. Use the `sam local start-api` to run the API locally on port 3000.

```bash
sam local start-api
curl http://localhost:3000/
```

The SAM CLI reads the application template to determine the API's routes and the functions that they invoke. The `Events` property on each function's definition includes the route and method for each path.

```yaml
    Events:
      CallerApi:
        Type: Api
        Properties:
          Path: /dt-caller
          Method: get
          RestApiId:
            Ref: ApiGatewayApiCaller
```

### Add a resource to your application
The application template uses AWS Serverless Application Model (AWS SAM) to define application resources. AWS SAM is an extension of AWS CloudFormation with a simpler syntax for configuring common serverless application resources such as functions, triggers, and APIs. For resources not included in [the SAM specification](https://github.com/awslabs/serverless-application-model/blob/master/versions/2016-10-31.md), you can use standard [AWS CloudFormation](https://docs.aws.amazon.com/AWSCloudFormation/latest/UserGuide/aws-template-resource-type-ref.html) resource types.

### Fetch, tail, and filter Lambda function logs

To simplify troubleshooting, SAM CLI has a command called `sam logs`. `sam logs` lets you fetch logs generated by your deployed Lambda function from the command line. In addition to printing the logs on the terminal, this command has several nifty features to help you quickly find the bug.

**NOTE**: This command works for all AWS Lambda functions; not just the ones you deploy using SAM.

```bash
sam logs -n DTCallerFunction --stack-name NewRelicExample-JavaDT --tail
```

You can find more information and examples about filtering Lambda function logs in the [SAM CLI Documentation](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/serverless-sam-cli-logging.html).

### Cleanup

To delete the sample application and the bucket that you created, use the AWS CLI.

```bash
aws cloudformation delete-stack --stack-name NewRelicExample-JavaDT
aws s3 rb s3://BUCKET_NAME
```

### Resources

See the [AWS SAM developer guide](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/what-is-sam.html) for an introduction to SAM specification, the SAM CLI, and serverless application concepts.

Next, you can use AWS Serverless Application Repository to deploy ready to use Apps that go beyond hello world samples and learn how authors developed their applications: [AWS Serverless Application Repository main page](https://aws.amazon.com/serverless/serverlessrepo/)

## Unit tests

Tests are defined in the `DTCallerFunction/src/test` folder in this project.

```bash
cd DTCallerFunction
DTCallerFunction$ gradle test
```
