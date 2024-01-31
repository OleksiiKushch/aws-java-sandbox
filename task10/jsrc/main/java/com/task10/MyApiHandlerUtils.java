package com.task10;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.io.IOException;
import java.util.*;

import static com.task10.ApiHandlerConstants.*;
import static com.task10.ApiHandlerConstants.TABLE_MIN_ORDER;

public class MyApiHandlerUtils {

    public static void createUserPoolApiClientIfNotExists(String cognitoId, String clientName, CognitoIdentityProviderClient cognitoClient, Context context) {
        boolean noOneApiClient = cognitoClient.listUserPoolClients(ListUserPoolClientsRequest.builder()
                .userPoolId(cognitoId)
                .build()).userPoolClients().isEmpty();
        if (noOneApiClient) {
            CreateUserPoolClientResponse createUserPoolClientResponse = cognitoClient.createUserPoolClient(CreateUserPoolClientRequest.builder()
                    .userPoolId(cognitoId)
                    .clientName(clientName)
                    .explicitAuthFlows(ExplicitAuthFlowsType.ADMIN_NO_SRP_AUTH)
                    .generateSecret(false)
                    .build());
            context.getLogger().log("Create user pool client. Response: " + createUserPoolClientResponse);
        }
    }

    public static UserPoolClientDescription getUserPoolApiDesc(String cognitoId, CognitoIdentityProviderClient cognitoClient, Context context) {
        ListUserPoolClientsResponse response = cognitoClient.listUserPoolClients(ListUserPoolClientsRequest.builder()
                .userPoolId(cognitoId)
                .build());
        Iterator<UserPoolClientDescription> it = response.userPoolClients().iterator();
        UserPoolClientDescription result = null;
        if (it.hasNext()) {
            result = it.next();
        } else {
            createUserPoolApiClientIfNotExists(cognitoId, COGNITO_CLIENT_API_NAME, cognitoClient, context);
        }

        context.getLogger().log("User pool app client: " + result);
        return result;
    }

    public static String getCognitoIdByName(String name, CognitoIdentityProviderClient client, Context context) {
        ListUserPoolsResponse listUserPoolsResponse = client.listUserPools(ListUserPoolsRequest.builder().build());
        List<UserPoolDescriptionType> userPools = listUserPoolsResponse.userPools();
        for (UserPoolDescriptionType userPool : userPools) {
            if (name.equals(userPool.name())) {
                String cognitoId = userPool.id();
                context.getLogger().log("Founded cognito id: " + cognitoId);
                return cognitoId;
            }
        }
        return null;
    }

    public static List<Map<String, Object>> getAllTables(AmazonDynamoDB dynamoDb, ScanRequest scanRequest) {
        ScanResult result = dynamoDb.scan(scanRequest);
        List<Map<String, Object>> tables = new ArrayList<>();
        for (Map<String, AttributeValue> item : result.getItems()) {
            Map<String, Object> table = new HashMap<>();
            putTableItemToMap(table, item);
            tables.add(table);
        }
        return tables;
    }

    public static void putTableItemToMap(Map<String, Object> map, Map<String, AttributeValue> item) {
        map.put(TABLE_ID, Integer.parseInt(item.get(TABLE_ID).getN()));
        map.put(TABLE_NUMBER, Integer.parseInt(item.get(TABLE_NUMBER).getN()));
        map.put(TABLE_PLACES, Integer.parseInt(item.get(TABLE_PLACES).getN()));
        map.put(TABLE_IS_VIP, item.get(TABLE_IS_VIP).getBOOL());
        if(item.containsKey(TABLE_MIN_ORDER)) {
            map.put(TABLE_MIN_ORDER, Integer.parseInt(item.get(TABLE_MIN_ORDER).getN()));
        }
    }


    @SuppressWarnings("unchecked")
    public static Map<String, Object> eventToBody(APIGatewayProxyRequestEvent event, Context context) {
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

    public static APIGatewayProxyResponseEvent formSuccessResponse(Object responseBody, Context context) {
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

    public static Map<String, String> getHeadersFromEvent(APIGatewayProxyRequestEvent event, Context context) {
        Map<String, String> headers = event.getHeaders();
        context.getLogger().log("Request headers: " + headers);
        return headers;
    }

    public static String getAccessToken(Map<String, String> headers, Context context) {
        String accessToken = headers.get(AUTHORIZATION_ATTR).split(" ")[1];
        context.getLogger().log("Access token: " + accessToken);
        return accessToken;
    }

    public static boolean checkIfTableExists(String tableNumber, AmazonDynamoDB dynamoDb, Context context) {
        ScanRequest scanRequest = new ScanRequest().withTableName(TABLES_TABLE_NAME);
        List<Map<String, Object>> tables = getAllTables(dynamoDb, scanRequest);
        return tables.stream().anyMatch(table -> tableNumber.equals(String.valueOf(table.get(TABLE_NUMBER))));
    }

    public static boolean checkIfReservationWithTableAlreadyExists(String tableNumber, AmazonDynamoDB dynamoDb, Context context) {
        ScanRequest scanRequest = new ScanRequest().withTableName(RESERVATIONS_TABLE_NAME);
        List<Map<String, Object>> tables = getAllTables(dynamoDb, scanRequest);
        return tables.stream().anyMatch(reservation -> tableNumber.equals(String.valueOf(reservation.get(RESERVATION_TABLE_NUMBER))));
    }
}
