package com.task10;

import com.amazonaws.services.lambda.runtime.Context;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.*;

import java.util.List;

public class MyApiHandlerUtils {

    public static void createUserPoolApiClientIfNotExists(String cognitoName, CognitoIdentityProviderClient cognitoClient, Context context) {
        String cognitoId = getCognitoIdByName(cognitoName, cognitoClient, context);
        boolean atLeastOneApiClient = !cognitoClient.listUserPoolClients(ListUserPoolClientsRequest.builder()
                .userPoolId(cognitoId)
                .build()).userPoolClients().isEmpty();
        if (atLeastOneApiClient) {
            CreateUserPoolClientResponse createUserPoolClientResponse = cognitoClient.createUserPoolClient(CreateUserPoolClientRequest.builder()
                    .userPoolId(cognitoId)
                    .clientName("task10_app_client_id")
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
        UserPoolClientDescription result = response.userPoolClients().iterator().next();
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
}
