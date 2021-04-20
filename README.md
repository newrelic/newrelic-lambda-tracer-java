[![Community Plus header](https://github.com/newrelic/opensource-website/raw/master/src/images/categories/Community_Plus.png)](https://opensource.newrelic.com/oss-category/#community-plus)

# New Relic OpenTracing Tracer for AWS Lambda Java

The New Relic Lambda Tracer is an OpenTracing [Tracer](https://opentracing.io/docs/overview/tracers/) implementation specifically designed to instrument Java-based AWS Lambda functions. The New Relic Lambda Tracer is intended to work in conjunction with the [AWS Lambda OpenTracing Java SDK](https://github.com/newrelic/java-aws-lambda).

It generates the following data in AWS Cloudwatch logs which, is then scraped by a [log ingestion Lambda function](https://docs.newrelic.com/docs/serverless-function-monitoring/aws-lambda-monitoring/get-started/enable-new-relic-monitoring-aws-lambda#stream-logs) and sent to New Relic's telemetry data platform:

- Span events
- Transaction events
- Error events
- Traced errors

Currently, the New Relic Lambda Tracer does not generate data types such as metrics or transaction traces.

## Installation

You can find artifacts for the [New Relic OpenTracing AWS Lambda Tracer](https://search.maven.org/search?q=a:newrelic-java-lambda) and [AWS Lambda OpenTracing Java SDK](https://search.maven.org/search?q=a:java-aws-lambda) on Maven Central.

The example below shows how to add them as dependencies in your `build.gradle` file:

```groovy
dependencies {
    implementation("com.newrelic.opentracing:newrelic-java-lambda:2.2.1")
    implementation("com.newrelic.opentracing:java-aws-lambda:2.1.0")
}
```

## Supported OpenTracing versions

The New Relic Lambda Tracer and SDK supports version 0.33.0 of OpenTracing as detailed below: 

* OpenTracing `0.33.0`:
  * Lambda Tracer: [com.newrelic.opentracing:newrelic-java-lambda:2.2.1](https://search.maven.org/artifact/com.
    newrelic.opentracing/newrelic-java-lambda)
  * Lambda SDK: [com.newrelic.opentracing:java-aws-lambda:2.1.0](https://search.maven.org/artifact/com.newrelic.opentracing/java-aws-lambda) 

## Getting Started

For full details on getting started please see the documentation on how to [Enable monitoring AWS Lambda with New Relic Serverless](https://docs.newrelic.com/docs/serverless-function-monitoring/aws-lambda-monitoring/get-started/enable-new-relic-monitoring-aws-lambda).

## Usage

```java
package com.handler.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import io.opentracing.util.GlobalTracer;
import com.newrelic.opentracing.aws.TracingRequestHandler;
import com.newrelic.opentracing.aws.LambdaTracing;
import com.newrelic.opentracing.LambdaTracer;

import java.util.Map;

/**
 * Tracing request handler that creates a span on every invocation of a Lambda.
 *
 * @param Map<String, Object> The Lambda Function input
 * @param String The Lambda Function output
 */
public class MyLambdaHandler implements RequestHandler<Map<String, Object>, String> {
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
        return LambdaTracing.instrument(apiGatewayProxyRequestEvent, context, (event, ctx) -> {
            // TODO Your function logic here
            return "Lambda Function output";
        });
    }
}
```

## Conventions for recording errors

The New Relic Lambda Tracer follows OpenTracing [semantic conventions](https://github.com/opentracing/specification/blob/master/semantic_conventions.md#log-fields-table) when recording error events and traces. The minimum required attributes for errors are `error.object` and `message`.

| Log key        | Log type                |                        Note                      | Required |
| :------------: | :---------------------: | :----------------------------------------------: | :------: |
| `error.object` | `Throwable`             | The `Throwable` object                           |   Yes    |
| `message`      | `Throwable` message     | The detail message string of the throwable       |   Yes    |
| `event`        | `String` `"error"`      | Indicates that an error event has occurred       | Optional |
| `stack`        | `Throwable` stacktrace  | The stack trace information of the throwable | Optional |
| `error.kind`   | `String` `"Exception"`  | Indicates that the error was an `Exception`      | Optional |

## Building

Run jar task: `./gradlew jar`

Artifact: `newrelic-lambda-tracer-java/build/libs/newrelic-java-lambda.jar`

## Testing

* Unit tests: `newrelic-lambda-tracer-java/src/test`
* JMH benchmarks: `newrelic-lambda-tracer-java/src/jmh`
  * Run JMH benchmarks: `./gradlew jmh`  
  * Find results in `build/reports/jmh`

When submitting a pull request, please ensure that tests are included for any new functionality and that all test suites are passing.

## Example JSON logged to AWS Cloudwatch

The New Relic Lambda Tracer logs a JSON payload to AWS Cloudwatch logs upon completion of the Lambda function. By default, everything but the metadata section in the JSON payload is compressed, as seen in the following example:

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

Additionally, you can use debug logging to log the uncompressed payloads, as seen in the below example.

To enable debug logging set the `NEW_RELIC_DEBUG` environment variable to `true` in the AWS Lambda console for a given function. 

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

## Support

New Relic hosts and moderates an online forum where customers can interact with New Relic employees as well as other customers to get help and share best practices. Like all official New Relic open source projects, there's a related Community topic in the New Relic Explorers Hub. You can find this project's topic/threads here:

https://discuss.newrelic.com/tags/javaagent

## Contributing

We encourage your contributions to improve `newrelic-lambda-tracer-java`! Keep in mind that when you submit your pull request, you'll need to sign the CLA via the click-through using CLA-Assistant. You only have to sign the CLA one time per project.

If you have any questions, or to execute our corporate CLA (which is required if your contribution is on behalf of a company), drop us an email at opensource@newrelic.com.

**A note about vulnerabilities**

As noted in our [security policy](../../security/policy), New Relic is committed to the privacy and security of our customers and their data. We believe that providing coordinated disclosure by security researchers and engaging with the security community are important means to achieve our security goals.

If you believe you have found a security vulnerability in this project or any of New Relic's products or websites, we welcome and greatly appreciate you reporting it to New Relic through [HackerOne](https://hackerone.com/newrelic).

If you would like to contribute to this project, review [these guidelines](./CONTRIBUTING.md).

## License
`newrelic-lambda-tracer-java` is licensed under the [Apache 2.0](http://apache.org/licenses/LICENSE-2.0.txt) License.

`newrelic-lambda-tracer-java` also uses source code from third-party libraries. You can find full details on which libraries are used and the terms under which they are licensed in the third-party notices document.
