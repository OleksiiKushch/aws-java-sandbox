package com.task04;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.events.SnsEventSource;

@LambdaHandler(lambdaName = "sns_handler",
	roleName = "sns_handler-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}"
)
@SnsEventSource(
		targetTopic = "lambda_topic"
)
public class SnsHandler implements RequestHandler<SNSEvent, Void> {

	public Void handleRequest(SNSEvent event, Context context) {
		LambdaLogger logger = context.getLogger();

		for (SNSEvent.SNSRecord record : event.getRecords()) {
			SNSEvent.SNS sns = record.getSNS();
			logger.log(sns.getMessage());
		}

		return null;
	}
}
