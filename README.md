# New Relic OpenTracing Tracer for AWS Lambda Java

## Purpose

The New Relic Lambda Tracer is an OpenTracing [Tracer](https://opentracing.io/docs/overview/tracers/) implementation specifically designed to support AWS Lambda.

It captures and generates the following data:

- Span Events
- Transaction Events
- Error Events
- Traced Errors

It does not capture other New Relic data types like metrics or transaction traces.

### Supported OpenTracing Versions

* OpenTracing 0.31.0: [com.newrelic.opentracing:newrelic-java-lambda:1.1.1](https://search.maven.org/artifact/com.newrelic.opentracing/newrelic-java-lambda/1.1.1/jar)
* OpenTracing 0.33.0: [com.newrelic.opentracing:newrelic-java-lambda:2.0.0](https://search.maven.org/artifact/com.newrelic.opentracing/newrelic-java-lambda/2.0.0/jar)

## How to Use

See New Relic documentation: https://docs.newrelic.com/docs/serverless-function-monitoring/aws-lambda-monitoring/get-started/enable-new-relic-monitoring-aws-lambda#java

1. [Add the `newrelic-java-lambda` dependency to your Gradle project.](#add-artifacts-to-gradle-project)
2. Add the [AWS Lambda OpenTracing Java SDK](https://github.com/newrelic/java-aws-lambda) as a dependency to your project and [implement the tracing request handler](https://github.com/newrelic/java-aws-lambda#how-to-use). *Note:* In order for the `LambdaTracer` to function fully it must be used in conjunction with the `TracingRequestHandler` interface provided by the AWS Lambda OpenTracing Java SDK. If a different request handler is used the `LambdaTracer` will not be able to generate Error Events or Traced Errors.
3. Register a `LambdaTracer.INSTANCE` as the OpenTracing Global tracer as shown in the [example](#example-usage).
4. See Amazon's documentation on [creating a ZIP deployment package for a Java Lambda function](https://docs.aws.amazon.com/lambda/latest/dg/create-deployment-pkg-zip-java.html)
5. When creating your Lambda function in AWS Lambda console the handler for the given example would be entered as `com.handler.example.MyLambdaHandler::handleRequest` or just `com.handler.example.MyLambdaHandler`, the latter of which will use `handleRequest` as the handler method by default. *Note:* `handleRequest` is used as the handler entry point as it will call `doHandleRequest`.

## Build the Project

Run jar task: `./gradlew jar`

Artifact: `newrelic-lambda-tracer-java/build/libs/newrelic-java-lambda.jar`

## Add Artifacts to Gradle Project

Include the [New Relic OpenTracing AWS Lambda Tracer](https://search.maven.org/search?q=a:newrelic-java-lambda) and [AWS Lambda OpenTracing Java SDK](https://search.maven.org/search?q=a:java-aws-lambda) artifacts by adding them as dependencies in your `build.gradle` file:

```groovy
dependencies {
    compile "com.newrelic.opentracing:newrelic-java-lambda:2.0.0"
    compile "com.newrelic.opentracing:java-aws-lambda:2.0.0"
}
```

## Enable New Relic Distributed Tracing
By default, traced Lambda functions won't be included in New Relic distributed traces. In order to enable distributed tracing for AWS Lambda the following environment variables must be configured in the AWS Lambda console for each function:

 | Environment Variable               | Description          | Default | Required |
 | :--------------------------------- | :------------------- | :-------- | :------- |
 | `NEW_RELIC_ACCOUNT_ID`             | New Relic account ID | `null` | Yes |
 | `NEW_RELIC_TRUSTED_ACCOUNT_KEY`    | If your New Relic account is a sub-account, this needs to be the account ID for the root/parent account. | `NEW_RELIC_ACCOUNT_ID` | For cross-account DT |
 | `NEW_RELIC_PRIMARY_APPLICATION_ID` | New Relic application ID | `Unknown` | No |

## Example Usage

```java
package com.handler.example;

import com.amazonaws.services.lambda.runtime.Context;
import io.opentracing.util.GlobalTracer;
import com.newrelic.opentracing.aws.TracingRequestHandler;
import com.newrelic.opentracing.LambdaTracer;

import java.util.Map;

/**
 * Tracing request handler that creates a span on every invocation of a Lambda.
 *
 * @param Map<String, Object> The Lambda Function input
 * @param String The Lambda Function output
 */
public class MyLambdaHandler implements TracingRequestHandler<Map<String, Object>, String> {
    static {
        // Register the New Relic OpenTracing LambdaTracer as the Global Tracer
        GlobalTracer.registerIfAbsent(LambdaTracer.INSTANCE);
    }

    /**
     * Method that handles the Lambda function request.
     *
     * @param input The Lambda Function input
     * @param context The Lambda execution environment context object
     * @return String The Lambda Function output
     */
    @Override
    public String doHandleRequest(Map<String, Object> input, Context context) {
        // TODO Your function logic here
        return "Lambda Function output";
    }
}
```

## Reporting Errors

The LambdaTracer follows OpenTracing [semantic conventions](https://github.com/opentracing/specification/blob/master/semantic_conventions.md#log-fields-table) when recording error events and traces to [Span Logs](https://opentracing.io/docs/overview/tags-logs-baggage/#logs). The minimum required attributes are `error.object` and `message`.

| Log key        | Log type                |                        Note                      | Required |
| :------------: | :---------------------: | :----------------------------------------------: | :------: |
| `error.object` | `Throwable`             | The `Throwable` object                           |   Yes    |
| `message`      | `Throwable` message     | The detail message string of the throwable       |   Yes    |
| `event`        | `String` `"error"`      | Indicates that an error event has occurred       | Optional |
| `stack`        | `Throwable` stacktrace  | The the stack trace information of the throwable | Optional |
| `error.kind`   | `String` `"Exception"`  | Indicates that the error was an `Exception`      | Optional |

## Sending Lambda Data to New Relic

The New Relic OpenTracing AWS Lambda Tracer logs the data it generates in JSON format to the [AWS Cloudwatch logs](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/WhatIsCloudWatchLogs.html) associated with the Lambda function that is being traced. The Cloudwatch logs are monitored by a separate [New Relic log ingestion Lambda function](https://docs.newrelic.com/docs/serverless-function-monitoring/aws-lambda-monitoring/get-started/enable-new-relic-monitoring-aws-lambda) that must be deployed and configured with a New Relic license key. When a Cloudwatch Event occurs the log ingestion Lambda will parse the relevant logs for the New Relic specific JSON payload and report the payload to New Relic.

## New Relic AWS Lambda JSON Payload

By default, everything but the metadata section in the JSON payload created by the New Relic Lambda Tracer is compressed, as seen in the following example:

```json
[
  2,
  "NR_LAMBDA_MONITORING",
  {
    "agent_version": "2.0.0",
    "protocol_version": 16,
    "agent_language": "java",
    "execution_environment": "AWS_Lambda_java8",
    "arn": "arn:aws:lambda:us-west-2:47474747:function:test",
    "metadata_version": 2
  },
  "H4sIAAAAAAAAAM2UyW7bMBCG34VnWRYpiVpuQZouKIoETXKKA4MiR44amVJJKo4b+N079BIbaBagTYFcdBiRM//MfPwfiO2FnsIdaDdVwglSXumhbYMHso7ZqQXQpGQBMWDB3HWNmdrmF2BoFVxdPRBnhIQvipQE8oTHkqpUMirjSJGA9MJgklD0Pf5njGHILXu8TM6xrD9gms40bklKGsZ5lHBeBESbEG+Z5VnXaEdKZwbYpVoX4rxWVa5UERcpjWjtsxqhrZCu6fT6SF1hNsgqRnkay7rea9nWP0JJAVGDEf4OdhNGSZbSeK9Zym7w5QmlFI9qMff3boRWLXyHnwNYh+HZ0Ph6ihZSFFBFFU+Tqo4P6nllfWfcxabwpb7V3cL3LoWDWWewdzIDDaaRT9z6cKCQJTzjNDnJAmLFvG9B7YbjmjnKwRjOMS0Yp4xnSZElK9zjjXN9iD/dYKeyU14DiyIsJRY2bMW8UiIUBgsQ/JYYLDfBcrCjBWYdsbKIsyjL4iwt60Gvh1y6TfsHOWTXqnMnzOPG/D+zGdRubSwXdTJKaJ6OaApylMuCj7hM4wiEpLSKCSpeXQf/h6sDhJ7Y11sgxPz0X0NoLm7h5N6B0aI9Fm170R31zSeEYSGWZ6a7X+6xyoVKiioqcPEoEqJ3gVXBPVaw7eDStP5ZIGS2nIwnY943+Y86KmQWwj3IwcFI9E34CFMo5uJXpz0csptPxh6kRs8mY+VGEqcB4CcFCwNtIz+DUGDsTg9aUN9pC9/AWjHzbZ9+Jfvw8ZpuhHsN0fU1IogCl66Rrxsc/dPg6D8Z3MUep5d59MQ+Z2tPAXdAMe4jhSxOQFaMcVb8laOduhswk/HH7cvebGSP4AvP4FkE397E3qtXPbK3ddituXr6Vr8B65xj8FwHAAA="
]
```

## ‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍‍Debug Logging

To enable debug logging add the `NEW_RELIC_DEBUG` key to the Lambda environment variable section with the value of `true`. Debug logging entries will be prefixed with `nr_debug` and will show full uncompressed payloads for Span events, Transaction events, and Error events as seen in the example.

##### Example Debug Log Entry

```json
[
  2,
  "DEBUG",
  {
    "agent_version": "2.0.0",
    "protocol_version": 16,
    "agent_language": "java",
    "execution_environment": "AWS_Lambda_java8",
    "arn": "arn:aws:lambda:us-west-2:47474747474747:function:test",
    "metadata_version": 2
  },
  {
    "span_event_data": [
      null,
      {
        "events_seen": 2,
        "reservoir_size": 2
      },
      [
        [
          {
            "traceId": "e8463c1d5c21c30d",
            "parent.app": "222",
            "type": "Span",
            "priority": 1.3804669,
            "nr.entryPoint": true,
            "parentId": "66fdb8dd9395101f",
            "transactionId": "fb380e7b21653cff",
            "parent.type": "App",
            "duration": 2.047513,
            "parent.account": "111",
            "name": "handleRequest",
            "guid": "d19ca9eb0b654bf3",
            "parent.transportType": "Unknown",
            "category": "generic",
            "parent.transportDuration": 22467614,
            "sampled": true,
            "timestamp": 1592612674974
          },
          {
            "http.status_code": "200",
            "aws.lambda.arn": "arn:aws:lambda:us-west-2:47474747474747:function:test",
            "aws.lambda.coldStart": true,
            "aws.requestId": "66f28af4-4747-4747-4747-6c530eac11b3"
          },
          {}
        ],
        [
          {
            "traceId": "e8463c1d5c21c30d",
            "parent.app": "222",
            "type": "Span",
            "priority": 1.3804669,
            "parentId": "d19ca9eb0b654bf3",
            "transactionId": "fb380e7b21653cff",
            "parent.type": "App",
            "duration": 2.021592,
            "parent.account": "111",
            "name": "makeExternalCallToApiGatewayProxy",
            "guid": "8ad49b09467bf3e0",
            "parent.transportType": "Unknown",
            "category": "generic",
            "parent.transportDuration": 22467614,
            "sampled": true,
            "timestamp": 1592612674996
          },
          {
            "externalUrl": "https://4747474747.execute-api.us-west-2.amazonaws.com/testing/dt-callee",
            "newrelicHeaders": true,
            "responseMessage": "OK",
            "responseCode": 200
          },
          {}
        ]
      ]
    ],
    "analytic_event_data": [
      null,
      {
        "events_seen": 1,
        "reservoir_size": 1
      },
      [
        [
          {
            "traceId": "e8463c1d5c21c30d",
            "parent.app": "222",
            "type": "Transaction",
            "priority": 1.3804669,
            "parentSpanId": "66fdb8dd9395101f",
            "parent.type": "App",
            "parentId": "1265e734ecb22629",
            "duration": 2.047513,
            "parent.account": "111",
            "name": "Other/Function/test",
            "guid": "fb380e7b21653cff",
            "parent.transportType": "Unknown",
            "parent.transportDuration": 22467614,
            "sampled": true,
            "timestamp": 1592612674974
          },
          {
            "aws.lambda.arn": "arn:aws:lambda:us-west-2:47474747474747:function:test",
            "aws.lambda.coldStart": true,
            "aws.requestId": "66f28af4-4747-4747-4747-6c530eac11b3"
          },
          {
            "response.status": "200"
          }
        ]
      ]
    ]
  }
]
``` 

## JMH Benchmarks
Run: `./gradlew jmh`  
Results can be found in `build/reports/jmh`
