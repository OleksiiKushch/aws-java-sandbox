package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.io.IOException;
import java.util.*;

import static com.task10.MyApiHandlerUtils.*;

@LambdaHandler(
		lambdaName = "api_handler",
		roleName = "api_handler-role"
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	private static final String RESOURCE_PROFIX = "cmtr-4df2c6a7-";
	private static final String RESOURCE_SUFFIX = "-test";

	private static final String TABLES_TABLE_NAME = RESOURCE_PROFIX + "Tables" + RESOURCE_SUFFIX;
	private static final String RESERVATIONS_TABLE_NAME = RESOURCE_PROFIX + "Reservations" + RESOURCE_SUFFIX;
	private static final String COGNITO_NAME = RESOURCE_PROFIX + "simple-booking-userpool" + RESOURCE_SUFFIX;

	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
		String path = event.getPath();
		String httpMethod = event.getHttpMethod();
		context.getLogger().log("Handle request with path: " + path + ", and http method: " + httpMethod + ";");

		CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();
		String cognitoId = getCognitoIdByName(COGNITO_NAME, cognitoClient, context);
		createUserPoolApiClientIfNotExists(cognitoId, cognitoClient, context);

		switch(path) {
			case "/signup":
				if("POST".equals(httpMethod)) return handleSignUp(event, context, cognitoClient);
				break;
			case "/signin":
				if("POST".equals(httpMethod)) return handleSignIn(event, context, cognitoClient);
				break;
			case "/tables":
				if("POST".equals(httpMethod)) return handleCreateTable(event, context, cognitoClient);
				if("GET".equals(httpMethod)) return handleGetTables(event, context, cognitoClient);
				break;
			case "/reservations":
				if("POST".equals(httpMethod)) return handleCreateReservation(event, context, cognitoClient);
				if("GET".equals(httpMethod)) return handleGetReservations(event, context, cognitoClient);
				break;
		}

		context.getLogger().log("No handler found for path: " + path + ", and http method: " + httpMethod + ";");
		throw new RuntimeException("No handler found for path: " + path + ", and http method: " + httpMethod + ";");
	}

	private APIGatewayProxyResponseEvent handleSignUp(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		Map<String, Object> body = eventToBody(event, context);
		String firstName = (String) body.get("firstName");
		String lastName = (String) body.get("lastName");
		String email = (String) body.get("email");
		String password = (String) body.get("password");

		String cognitoId = getCognitoIdByName(COGNITO_NAME, cognitoClient, context);
		try {
			AdminCreateUserResponse creationResult = cognitoClient.adminCreateUser(AdminCreateUserRequest.builder()
					.userPoolId(cognitoId)
					.username(email)
					.temporaryPassword(password)
					.messageAction(MessageActionType.SUPPRESS)
					.userAttributes(
							AttributeType.builder().name("email").value(email).build(),
							AttributeType.builder().name("given_name").value(firstName).build(),
							AttributeType.builder().name("family_name").value(lastName).build(),
							AttributeType.builder().name("email_verified").value("true").build()
					)
					.build());
			context.getLogger().log("Create admin: " + creationResult);

			cognitoClient.adminSetUserPassword(AdminSetUserPasswordRequest.builder()
							.userPoolId(cognitoId)
							.username(email)
							.password(password)
							.permanent(true)
							.build());
			context.getLogger().log("Set new password successful.");
		} catch (CognitoIdentityProviderException e) {
			context.getLogger().log(e.getMessage());
			return new APIGatewayProxyResponseEvent()
					.withBody(e.getMessage())
					.withStatusCode(400);
		}
		return formSuccessResponse(null, context);
	}

	private APIGatewayProxyResponseEvent handleSignIn(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		Map<String, Object> body = eventToBody(event, context);
		String email = (String) body.get("email");
		String password = (String) body.get("password");

		String cognitoId = getCognitoIdByName(COGNITO_NAME, cognitoClient, context);
		UserPoolClientDescription appClient = getUserPoolApiDesc(cognitoId, cognitoClient, context);
		Map<String, String> authParameters = new HashMap<>();
		authParameters.put("USERNAME", email);
		authParameters.put("PASSWORD", password);
		AdminInitiateAuthResponse authResponse = null;
		try {
			authResponse = cognitoClient.adminInitiateAuth(AdminInitiateAuthRequest.builder()
					.userPoolId(cognitoId)
					.clientId(appClient.clientId())
					.authFlow(AuthFlowType.ADMIN_NO_SRP_AUTH)
					.authParameters(authParameters)
					.build());
		} catch (UserNotFoundException e) {
			context.getLogger().log(e.getMessage());
			return new APIGatewayProxyResponseEvent()
					.withBody(e.getMessage())
					.withStatusCode(400);
		}

		Map<String, String> responseBody = new HashMap<>();
		responseBody.put("accessToken", authResponse.authenticationResult().accessToken());

		return formSuccessResponse(responseBody, context);
	}

	private APIGatewayProxyResponseEvent handleCreateTable(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		Map<String, Object> body = eventToBody(event, context);
		Map<String, String> headers = event.getHeaders();
		context.getLogger().log("Request headers: " + headers);
		try {
			cognitoClient.getUser(GetUserRequest.builder()
					.accessToken(String.valueOf(headers.get("accessToken")))
					.build());

			String id = String.valueOf(body.get("id"));
			String minOrder = String.valueOf(body.get("minOrder"));
			Map<String, AttributeValue> item = new HashMap<>();
			item.put("id", new AttributeValue().withN(id));
			item.put("number", new AttributeValue().withN(String.valueOf(body.get("number"))));
			item.put("places", new AttributeValue().withN(String.valueOf(body.get("places"))));
			item.put("isVip", new AttributeValue().withBOOL((boolean) body.get("isVip")));
			if(minOrder != null) {
				item.put("minOrder", new AttributeValue().withN(minOrder));
			}

			PutItemRequest putItemRequest = new PutItemRequest(TABLES_TABLE_NAME, item);
			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
			dynamoDb.putItem(putItemRequest);

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("id", id);

			return formSuccessResponse(responseBody, context);
		} catch (NotAuthorizedException ex) {
			context.getLogger().log("Table can't be created because customer is unauthorized. Invalid Access Token.");
			return new APIGatewayProxyResponseEvent()
					.withBody("Table can't be created because customer is unauthorized. Invalid Access Token.")
					.withStatusCode(400);
		}
	}

	private APIGatewayProxyResponseEvent handleGetTables(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		Map<String, String> headers = event.getHeaders();
		context.getLogger().log("Request headers: " + headers);
		try {
			GetUserResponse userResponse = cognitoClient.getUser(GetUserRequest.builder()
					.accessToken(String.valueOf(headers.get("accessToken")))
					.build());

			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
			ScanRequest scanRequest = new ScanRequest().withTableName(TABLES_TABLE_NAME);

			Map<String, Object> responseBody = new HashMap<>();
			if (event.getResource().equals("/tables/{tableId}")) {
				Map<String, AttributeValue> keyToGet = new HashMap<>();
				String tableId = event.getPathParameters().get("tableId");
				keyToGet.put("id", new AttributeValue(tableId));
				GetItemRequest request = new GetItemRequest().withKey(keyToGet).withTableName("Tables");
				Map<String, AttributeValue> item = dynamoDb.getItem(request).getItem();
				if (Objects.isNull(item)) {
					return new APIGatewayProxyResponseEvent().withBody("No item found for " + tableId).withStatusCode(404);
				}
				putTableItemToMap(responseBody, item);
			} else {
				responseBody.put("tables", getAllTables(dynamoDb, scanRequest));
			}

			return formSuccessResponse(responseBody, context);
		} catch (NotAuthorizedException ex) {
			context.getLogger().log("Can't get tables because customer is unauthorized. Invalid Access Token.");
			return new APIGatewayProxyResponseEvent()
					.withBody("Can't get tables because customer is unauthorized. Invalid Access Token.")
					.withStatusCode(400);
		}
	}

	private List<Map<String, Object>> getAllTables(AmazonDynamoDB dynamoDb, ScanRequest scanRequest) {
		ScanResult result = dynamoDb.scan(scanRequest);
		List<Map<String, Object>> tables = new ArrayList<>();
		for (Map<String, AttributeValue> item : result.getItems()) {
			Map<String, Object> table = new HashMap<>();
			putTableItemToMap(table, item);
			tables.add(table);
		}
		return tables;
	}

	private void putTableItemToMap(Map<String, Object> map, Map<String, AttributeValue> item) {
		map.put("id", Integer.parseInt(item.get("id").getN()));
		map.put("number", Integer.parseInt(item.get("number").getN()));
		map.put("places", Integer.parseInt(item.get("places").getN()));
		map.put("isVip", item.get("isVip").getBOOL());
		if(item.containsKey("minOrder")) {
			map.put("minOrder", Integer.parseInt(item.get("minOrder").getN()));
		}
	}

	private APIGatewayProxyResponseEvent handleCreateReservation(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		Map<String, Object> body = eventToBody(event, context);
		Map<String, String> headers = event.getHeaders();
		context.getLogger().log("Request headers: " + headers);
		try {
			GetUserResponse userResponse = cognitoClient.getUser(GetUserRequest.builder()
					.accessToken(String.valueOf(headers.get("accessToken")))
					.build());

			String reservationId = UUID.randomUUID().toString();
			Map<String, AttributeValue> item = new HashMap<>();
			item.put("tableNumber", new AttributeValue(String.valueOf(body.get("tableNumber"))));
			item.put("clientName", new AttributeValue(String.valueOf(body.get("clientName"))));
			item.put("phoneNumber", new AttributeValue(String.valueOf(body.get("phoneNumber"))));
			item.put("date", new AttributeValue(String.valueOf(body.get("date"))));
			item.put("slotTimeStart", new AttributeValue(String.valueOf(body.get("slotTimeStart"))));
			item.put("slotTimeEnd", new AttributeValue(String.valueOf(body.get("slotTimeEnd"))));
			item.put("reservationId", new AttributeValue(reservationId));

			PutItemRequest putItemRequest = new PutItemRequest(RESERVATIONS_TABLE_NAME, item);
			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
			dynamoDb.putItem(putItemRequest);

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("reservationId", reservationId);

			return formSuccessResponse(responseBody, context);
		} catch (NotAuthorizedException ex) {
			context.getLogger().log("Reservation can't be created because customer is unauthorized. Invalid Access Token.");
			return new APIGatewayProxyResponseEvent()
					.withBody("Reservation can't be created because customer is unauthorized. Invalid Access Token.")
					.withStatusCode(400);
		}
	}

	private APIGatewayProxyResponseEvent handleGetReservations(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		Map<String, String> headers = event.getHeaders();
		context.getLogger().log("Request headers: " + headers);
		try {
			GetUserResponse userResponse = cognitoClient.getUser(GetUserRequest.builder()
					.accessToken(String.valueOf(headers.get("accessToken")))
					.build());

			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
			ScanRequest scanRequest = new ScanRequest().withTableName(RESERVATIONS_TABLE_NAME);
			ScanResult result = dynamoDb.scan(scanRequest);

			List<Map<String, Object>> reservations = new ArrayList<>();
			for (Map<String, AttributeValue> item : result.getItems()) {
				Map<String, Object> reservation = new HashMap<>();
				reservation.put("tableNumber", Integer.parseInt(item.get("tableNumber").getN()));
				reservation.put("clientName", item.get("clientName").getS());
				reservation.put("phoneNumber", item.get("phoneNumber").getS());
				reservation.put("date", item.get("date").getS());
				reservation.put("slotTimeStart", item.get("slotTimeStart").getS());
				reservation.put("slotTimeEnd", item.get("slotTimeEnd").getS());
				reservations.add(reservation);
			}

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("reservations", reservations);

			return formSuccessResponse(responseBody, context);
		} catch (NotAuthorizedException ex) {
			context.getLogger().log("Can't get reservations because customer is unauthorized. Invalid Access Token.");
			return new APIGatewayProxyResponseEvent()
					.withBody("Can't get reservations because customer is unauthorized. Invalid Access Token.")
					.withStatusCode(400);
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> eventToBody(APIGatewayProxyRequestEvent event, Context context) {
		ObjectMapper mapper = new ObjectMapper();
		Map<String, Object> body = null;
		try {
			body = mapper.readValue(event.getBody(), Map.class);
		} catch (IOException e) {
			context.getLogger().log(e.getMessage());
			throw new RuntimeException(e);
		}
		context.getLogger().log("Request body: " + body);
		return body;
	}

	private APIGatewayProxyResponseEvent formSuccessResponse(Object responseBody, Context context) {
		ObjectMapper mapper = new ObjectMapper();
		APIGatewayProxyResponseEvent response = null;
		try {
			if (Objects.nonNull(responseBody)) {
				response = new APIGatewayProxyResponseEvent().withBody(mapper.writeValueAsString(responseBody))
						.withStatusCode(200);
			} else {
				response = new APIGatewayProxyResponseEvent()
						.withStatusCode(200);
			}
		} catch (JsonProcessingException e) {
			context.getLogger().log(e.getMessage());
			throw new RuntimeException(e);
		}
		context.getLogger().log("Response: " + response);
		return response;
	}
}
