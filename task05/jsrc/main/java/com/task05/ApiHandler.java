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

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            LambdaLogger logger = context.getLogger();

            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
            DynamoDB dynamoDB = new DynamoDB(client);
            Table table = dynamoDB.getTable("Events");

            ObjectMapper objectMapper = new ObjectMapper();
            EventRequest eventRequest = objectMapper.readValue(request.getBody(), EventRequest.class);

            String id = UUID.randomUUID().toString();
			/*String principalId = String.valueOf(eventRequest.getPrincipalId());
			String body = objectMapper.writeValueAsString(eventRequest.getContent());*/
			String createAt = Instant.now().toString();

			/*Map<String, AttributeValue> item = new HashMap<>();
			item.put("id", new AttributeValue(id));
            item.put("principalId", new AttributeValue(principalId));
            item.put("body", new AttributeValue(body));
            item.put("createdAt", new AttributeValue(createAt));*/

            Item item = new Item()
                    .withPrimaryKey("id", id)
                    .withNumber("principalId", eventRequest.getPrincipalId())
                    .withMap("body", eventRequest.getContent())
                    .withString("createdAt", createAt);
            logger.log("Item: " + item.toString());
            PutItemOutcome outcome = table.putItem(item);
            logger.log("Outcome item: " + outcome);

            /*PutItemRequest putItemRequest = new PutItemRequest("Events", item);
            PutItemResult putItemResult = ddb.putItem(putItemRequest);*/

            /*EventResponse eventResponse = new EventResponse();
            eventResponse.setId(id);
            eventResponse.setPrincipalId(eventRequest.getPrincipalId());
            eventResponse.setBody(eventRequest.getContent());
            eventResponse.setCreatedAt(createAt);*/

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(201)
                    /*.withBody(objectMapper.writeValueAsString(eventResponse));*/
                    .withBody(objectMapper.writeValueAsString(outcome));
        } catch (Exception e) {
            logger.log("Exception: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("An error occurred: " + e.getMessage());
        }
    }
}
