Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

aws iam attach-role-policy --role-name ${put-function} --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBFullAccess
aws iam attach-role-policy --role-name ${put-function} --policy-arn arn:aws:iam::aws:policy/AWSOpsWorksCloudWatchLogs
aws iam attach-role-policy --role-name ${get-function} --policy-arn arn:aws:iam::aws:policy/AmazonDynamoDBReadOnlyAccess
aws iam attach-role-policy --role-name ${get-function} --policy-arn arn:aws:iam::aws:policy/AWSOpsWorksCloudWatchLogs

aws apigatewayv2 create-stage --api-id ${api-id} --stage-name v1 --description "v1 Stage" --auto-deploy

aws apigatewayv2 create-integration --api-id ${api-id} --integration-type AWS_PROXY --integration-method POST --payload-format-version 2.0 --integration-uri arn:aws:lambda:${region}:${account-id}:function:${put-function}
aws apigatewayv2 create-route --api-id ${api-id} --route-key "POST /products" --target integrations/${integration-id-post}
aws lambda add-permission --statement-id ${any-statement-id-post} --action lambda:InvokeFunction --function-name "arn:aws:lambda:${region}:${account-id}:function:${put-function}" --principal apigateway.amazonaws.com --source-arn "arn:aws:execute-api:${region}:${account-id}:${api-id}/*/POST/products"

aws apigatewayv2 create-integration --api-id ${api-id} --integration-type AWS_PROXY --integration-method GET --payload-format-version 2.0 --integration-uri arn:aws:lambda:${region}:${account-id}:function:${get-function}
aws apigatewayv2 create-route --api-id ${api-id} --route-key "GET /products" --target integrations/${integration-id-get}
aws lambda add-permission --statement-id ${any-statement-id-get} --action lambda:InvokeFunction --function-name "arn:aws:lambda:${region}:${account-id}:function:${get-function}" --principal apigateway.amazonaws.com --source-arn "arn:aws:execute-api:${region}:${account-id}:${api-id}/*/GET/products"

aws lambda update-function-code --function-name ${put-function} --zip-file fileb://create_product.zip
aws lambda update-function-code --function-name ${get-function} --zip-file fileb://get_products_list.zip
