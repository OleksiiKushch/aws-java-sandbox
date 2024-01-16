Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

# 
aws s3api put-bucket-notification-configuration --bucket ${bucket} --notification-configuration file://event-notification-config.json

#
aws lambda create-event-source-mapping --function-name ${lambda-function} --batch-size 10 --event-source-arn arn:aws:sqs:${region}:${account-id}:${queue}