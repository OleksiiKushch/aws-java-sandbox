Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

# Prohibit deletion of any objects inside some bucket
aws iam put-role-policy --role-name ${role} --policy-name ListBucketPolicy --policy-document file://policy-task1-list-all.json

# Add responsive policy for S3 bucket
aws s3api put-bucket-policy --bucket ${bucket} --policy file://policy-task1-my-bucket.json