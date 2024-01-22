package com.task06;

import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.DynamodbEvent;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import com.syndicate.deployment.annotations.events.DynamoDbTriggerEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@LambdaHandler(lambdaName = "audit_producer",
	roleName = "audit_producer-role",
		timeout = 20
)
@DynamoDbTriggerEventSource(
		targetTable = "Configuration",
		batchSize = 10
)
public class AuditProducer implements RequestHandler<DynamodbEvent, Void> {

	private static final String PREFIX = "cmtr-4df2c6a7-";
	private static final String SUFFIX = "-test";
	private static final String TABLE_NAME = PREFIX + "Audit" + SUFFIX;

	public Void handleRequest(DynamodbEvent ddbEvent, Context context) {
		LambdaLogger logger = context.getLogger();

		AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();

		for (DynamodbEvent.DynamodbStreamRecord record : ddbEvent.getRecords()){
			if (record == null) {
				continue;
			}
			String eventType = record.getEventName();

			if(eventType != null && eventType.equals("INSERT") || eventType.equals("MODIFY")) {
				Map<String, AttributeValue> newImageData = record.getDynamodb().getNewImage();
				Map<String, AttributeValue> oldImageData = record.getDynamodb().getOldImage();

				PutItemRequest putItemRequest = new PutItemRequest();
				putItemRequest.withTableName("Audit")
						.addItemEntry("id",
								new AttributeValue().withS(UUID.randomUUID().toString()))
						.addItemEntry("itemKey",
								new AttributeValue().withS(newImageData.get("key").getS()))
						.addItemEntry("modificationTime",
								new AttributeValue().withS(Instant.now().toString()));

				if(eventType.equals("INSERT")){
					putItemRequest.addItemEntry("newValue", newImageData.get("value"));
				} else if(eventType.equals("MODIFY")){
					putItemRequest.addItemEntry("oldValue", oldImageData.get("value"));
					putItemRequest.addItemEntry("newValue", newImageData.get("value"));
					putItemRequest.addItemEntry("updatedAttribute",
							new AttributeValue().withS("value"));
				}
				ddb.putItem(putItemRequest);
			}
		}
		return null;
	}
}
