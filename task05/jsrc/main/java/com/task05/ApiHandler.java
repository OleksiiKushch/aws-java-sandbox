package com.task05;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.PutItemRequest;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.regions.Regions;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.Instant;

import com.task05.EventRequest;
import com.task05.EventResponse;

@LambdaHandler(lambdaName = "api_handler",
        roleName = "api_handler-role"
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final String PREFIX = "cmtr-4df2c6a7-";
    private static final String SUFFIX = "-test";
    private static final String TABLE_NAME = PREFIX + "Events" + SUFFIX;
    private static final String ITEM_ID_ATTR = "id";
    private static final String ITEM_PRINCIPAL_ID_ATTR = "id";
    private static final String ITEM_BODY_ATTR = "body";
    private static final String ITEM_CREATE_AT_ATTR = "createdAt";

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        LambdaLogger logger = context.getLogger();
        try {
            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
                    .withRegion(Regions.EU_CENTRAL_1)
                    .build();
            DynamoDB dynamoDB = new DynamoDB(client);
            Table table = dynamoDB.getTable(TABLE_NAME);

            ObjectMapper objectMapper = new ObjectMapper();
            EventRequest eventRequest = objectMapper.readValue(request.getBody(), EventRequest.class);

            String id = UUID.randomUUID().toString();
			String createAt = Instant.now().toString();

            Item item = new Item()
                    .withPrimaryKey(ITEM_ID_ATTR, id)
                    .withNumber(ITEM_PRINCIPAL_ID_ATTR, eventRequest.getPrincipalId())
                    .withMap(ITEM_BODY_ATTR, eventRequest.getContent())
                    .withString(ITEM_CREATE_AT_ATTR, createAt);
            PutItemOutcome outcome = table.putItem(item);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    .withBody(objectMapper.writeValueAsString(outcome));
        } catch (Exception e) {
            logger.log("Exception: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("An error occurred: " + e.getMessage());
        }
    }
}
