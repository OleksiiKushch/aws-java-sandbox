Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

aws lambda update-function-code --function-name cmtr-4df2c6a7-lambda-fgufc-lambda --zip-file fileb://lambda_function.zip
