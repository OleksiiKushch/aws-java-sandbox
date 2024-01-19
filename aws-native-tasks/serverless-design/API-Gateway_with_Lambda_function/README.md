Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

# Create my HTTP api gateway
aws apigatewayv2 create-api --name cmtr-4df2c6a7-my-api-gateway --protocol-type HTTP --target arn:aws:lambda:${region}:${account-id}:function:${lambda-function} --route-key "GET /contacts"
aws apigatewayv2 create-api --name cmtr-4df2c6a7-my-api-gateway --protocol-type HTTP --target arn:aws:lambda:eu-central-1:196241772369:function:cmtr-4df2c6a7-api-gwlp-lambda-contacts --route-key "GET /contacts" --cors-configuration AllowCredentials=true,AllowHeaders="*",AllowMethods="*",MaxAge=3000

#
aws lambda add-permission --function-name ${lambda-function} --statement-id my-apigateway-get --action lambda:InvokeFunction --principal apigateway.amazonaws.com --source-arn "arn:aws:execute-api:${region}:${account-id}:${api-id}/*/GET/contacts"
aws lambda add-permission --function-name "arn:aws:lambda:eu-central-1:196241772369:function:cmtr-4df2c6a7-api-gwlp-lambda-contacts" --statement-id 8b6e0301-5d40-560f-9308-cc953f4aada5 --action lambda:* --principal apigateway.amazonaws.com --source-arn "arn:aws:execute-api:eu-central-1:196241772369:m4kmfw4546/*/*/contacts"
