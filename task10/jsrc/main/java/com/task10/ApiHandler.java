package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.*;

import static com.task10.ApiHandlerConstants.*;
import static com.task10.MyApiHandlerUtils.*;

@LambdaHandler(
		lambdaName = "api_handler",
		roleName = "api_handler-role"
)
public class ApiHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

	public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
		String path = event.getPath();
		String httpMethod = event.getHttpMethod();
		context.getLogger().log("Handle request with path: " + path + ", and http method: " + httpMethod + ";");

		CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.create();
		String cognitoId = getCognitoIdByName(COGNITO_NAME, cognitoClient, context);
		createUserPoolApiClientIfNotExists(cognitoId, COGNITO_CLIENT_API_NAME, cognitoClient, context);

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
		String firstName = (String) body.get(FIRST_NAME_ATTR);
		String lastName = (String) body.get(LAST_NAME_ATTR);
		String email = (String) body.get(EMAIL_NAME_ATTR);
		String password = (String) body.get(PASSWORD_NAME_ATTR);

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
		String email = (String) body.get(EMAIL_NAME_ATTR);
		String password = (String) body.get(PASSWORD_NAME_ATTR);

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
		responseBody.put(ACCESS_TOKEN_ATTR, authResponse.authenticationResult().accessToken());

		return formSuccessResponse(responseBody, context);
	}

	private APIGatewayProxyResponseEvent handleCreateTable(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		Map<String, Object> body = eventToBody(event, context);
		try {
			cognitoClient.getUser(GetUserRequest.builder()
					.accessToken(getAccessToken(getHeadersFromEvent(event, context), context))
					.build());

			Map<String, AttributeValue> item = new HashMap<>();
			String id = String.valueOf(body.get(TABLE_ID));
			item.put(TABLE_ID, new AttributeValue().withN(id));
			item.put(TABLE_NUMBER, new AttributeValue().withN(String.valueOf(body.get(TABLE_NUMBER))));
			item.put(TABLE_PLACES, new AttributeValue().withN(String.valueOf(body.get(TABLE_PLACES))));
			item.put(TABLE_IS_VIP, new AttributeValue().withBOOL((boolean) body.get(TABLE_IS_VIP)));
			String minOrder = String.valueOf(body.get(TABLE_MIN_ORDER));
			if(minOrder != null) {
				item.put(TABLE_MIN_ORDER, new AttributeValue().withN(minOrder));
			}

			PutItemRequest putItemRequest = new PutItemRequest(TABLES_TABLE_NAME, item);
			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
			dynamoDb.putItem(putItemRequest);

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put(ID_ATTR, Integer.parseInt(id));

			return formSuccessResponse(responseBody, context);
		} catch (NotAuthorizedException ex) {
			context.getLogger().log("Table can't be created because customer is unauthorized. Invalid Access Token.");
			return new APIGatewayProxyResponseEvent()
					.withBody("Table can't be created because customer is unauthorized. Invalid Access Token.")
					.withStatusCode(400);
		}
	}

	private APIGatewayProxyResponseEvent handleGetTables(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		try {
			cognitoClient.getUser(GetUserRequest.builder()
					.accessToken(getAccessToken(getHeadersFromEvent(event, context), context))
					.build());

			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();

			Map<String, Object> responseBody = new HashMap<>();
			Map<String, String> pathParameters = event.getPathParameters();
			context.getLogger().log("Path parameter: " + pathParameters);
			String tableId = pathParameters != null ? pathParameters.get(TABLE_ID_PATHVAR) : null;
			context.getLogger().log("Table id: " + tableId);
			if (Objects.isNull(tableId) || tableId.isEmpty()) {
				ScanRequest scanRequest = new ScanRequest().withTableName(TABLES_TABLE_NAME);
				responseBody.put(TABLES_ATTR, getAllTables(dynamoDb, scanRequest));
			} else {
				context.getLogger().log("Handle request with tableId path parameter: " + tableId);
				Map<String, AttributeValue> keyToGet = new HashMap<>();
				keyToGet.put(TABLE_ID, new AttributeValue(tableId));
				GetItemRequest request = new GetItemRequest().withKey(keyToGet).withTableName(TABLES_TABLE_NAME);
				Map<String, AttributeValue> item = dynamoDb.getItem(request).getItem();
				if (Objects.isNull(item)) {
					return new APIGatewayProxyResponseEvent().withBody("No item found for " + tableId).withStatusCode(404);
				}
				putTableItemToMap(responseBody, item);
			}

			return formSuccessResponse(responseBody, context);
		} catch (NotAuthorizedException ex) {
			context.getLogger().log("Can't get tables because customer is unauthorized. Invalid Access Token.");
			return new APIGatewayProxyResponseEvent()
					.withBody("Can't get tables because customer is unauthorized. Invalid Access Token.")
					.withStatusCode(400);
		}
	}

	private APIGatewayProxyResponseEvent handleCreateReservation(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		Map<String, Object> body = eventToBody(event, context);
		try {
			cognitoClient.getUser(GetUserRequest.builder()
					.accessToken(getAccessToken(getHeadersFromEvent(event, context), context))
					.build());

			Map<String, AttributeValue> item = new HashMap<>();
			String tableNumber = String.valueOf(body.get("tableNumber"));
			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
			if (checkIfTableExists(tableNumber, dynamoDb)) {
				context.getLogger().log("Table with number: " + tableNumber + " dose not exists.");
				return new APIGatewayProxyResponseEvent()
						.withStatusCode(400);
			}

			String reservationId = UUID.randomUUID().toString();
			item.put(RESERVATION_ID, new AttributeValue().withS(reservationId));
			item.put(RESERVATION_TABLE_NUMBER, new AttributeValue().withN(tableNumber));
			item.put(RESERVATION_CLIENT_NAME, new AttributeValue().withS(String.valueOf(body.get(RESERVATION_CLIENT_NAME))));
			item.put(RESERVATION_PHONE_NUMBER, new AttributeValue().withS(String.valueOf(body.get(RESERVATION_PHONE_NUMBER))));
			item.put(RESERVATION_DATE, new AttributeValue().withS(String.valueOf(body.get(RESERVATION_DATE))));
			item.put(RESERVATION_SLOT_TIME_START, new AttributeValue().withS(String.valueOf(body.get(RESERVATION_SLOT_TIME_START))));
			item.put(RESERVATION_SLOT_TIME_END, new AttributeValue().withS(String.valueOf(body.get(RESERVATION_SLOT_TIME_END))));

			PutItemRequest putItemRequest = new PutItemRequest(RESERVATIONS_TABLE_NAME, item);
			dynamoDb.putItem(putItemRequest);

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put(RESERVATION_ID_ATTR, reservationId);

			return formSuccessResponse(responseBody, context);
		} catch (NotAuthorizedException ex) {
			context.getLogger().log("Reservation can't be created because customer is unauthorized. Invalid Access Token.");
			return new APIGatewayProxyResponseEvent()
					.withBody("Reservation can't be created because customer is unauthorized. Invalid Access Token.")
					.withStatusCode(400);
		}
	}

	private APIGatewayProxyResponseEvent handleGetReservations(APIGatewayProxyRequestEvent event, Context context, CognitoIdentityProviderClient cognitoClient) {
		try {
			cognitoClient.getUser(GetUserRequest.builder()
					.accessToken(getAccessToken(getHeadersFromEvent(event, context), context))
					.build());

			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
			ScanRequest scanRequest = new ScanRequest().withTableName(RESERVATIONS_TABLE_NAME);
			ScanResult result = dynamoDb.scan(scanRequest);

			List<Map<String, Object>> reservations = new ArrayList<>();
			for (Map<String, AttributeValue> item : result.getItems()) {
				Map<String, Object> reservation = new HashMap<>();
				reservation.put(RESERVATION_TABLE_NUMBER, Integer.parseInt(item.get(RESERVATION_TABLE_NUMBER).getN()));
				reservation.put(RESERVATION_CLIENT_NAME, item.get(RESERVATION_CLIENT_NAME).getS());
				reservation.put(RESERVATION_PHONE_NUMBER, item.get(RESERVATION_PHONE_NUMBER).getS());
				reservation.put(RESERVATION_DATE, item.get(RESERVATION_DATE).getS());
				reservation.put(RESERVATION_SLOT_TIME_START, item.get(RESERVATION_SLOT_TIME_START).getS());
				reservation.put(RESERVATION_SLOT_TIME_END, item.get(RESERVATION_SLOT_TIME_END).getS());
				reservations.add(reservation);
			}

			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put(RESERVATIONS_ATTR, reservations);

			return formSuccessResponse(responseBody, context);
		} catch (NotAuthorizedException ex) {
			context.getLogger().log("Can't get reservations because customer is unauthorized. Invalid Access Token.");
			return new APIGatewayProxyResponseEvent()
					.withBody("Can't get reservations because customer is unauthorized. Invalid Access Token.")
					.withStatusCode(400);
		}
	}
}
