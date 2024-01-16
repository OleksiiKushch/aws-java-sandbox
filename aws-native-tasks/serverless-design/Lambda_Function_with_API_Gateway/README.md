Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

# 
aws iam attach-role-policy --role-name ${role} --policy-arn arn:aws:iam::aws:policy/AWSLambda_ReadOnlyAccess

#
aws lambda add-permission --statement-id my-ip --action lambda:InvokeFunction --function-name "arn:aws:lambda:${region}:${account-id}:function:${lambda-function}" --principal apigateway.amazonaws.com --source-arn "arn:aws:execute-api:${region}:${account-id}:${api-id}/*/*/get_list"