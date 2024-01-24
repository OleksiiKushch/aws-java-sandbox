Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

# Create my HTTP api gateway (NOT NEED)
aws apigatewayv2 create-api --name ${your-api-name} --protocol-type HTTP --target arn:aws:lambda:${region}:${account-id}:function:${lambda-function} --route-key "GET /contacts"

# Update routes for some api-gateway
aws apigatewayv2 update-route --api-id ${api-id} --route-id ${route-id} --target integrations/${integration-id} --cli-input-json file://path-to-json-file.json

# Add permission
aws lambda add-permission --statement-id ${your-statement-id} --action lambda:InvokeFunction --function-name "arn:aws:lambda:${region}:${account-id}:function:${function}" --principal apigateway.amazonaws.com --source-arn "arn:aws:execute-api:${region}:${account-id}:${api-id}/*/GET/contacts"