Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

# Set policy that able to use kms
aws iam put-role-policy --role-name ${role} --policy-name KMSPolicy --policy-document file://policy-task4.json

# Set encryption for s3 bucket
aws s3api put-bucket-encryption --bucket ${bucket-2} --server-side-encryption-configuration file://server-side-encryption.json

# Copy object from one s3 bucket to another and encrypt it.
aws s3 cp s3://${bucket-1}/confidential_credentials.csv s3://${bucket-2}/ --sse aws:kms --sse-kms-key-id ${kms-key}
