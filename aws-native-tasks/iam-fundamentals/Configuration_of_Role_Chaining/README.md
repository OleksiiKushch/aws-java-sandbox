Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

aws iam put-role-policy --role-name ${role-assume} --policy-name AssumeReadonlyRolePolicy --policy-document file://policy-task3-assume.json

aws iam attach-role-policy --role-name ${role-readonly} --policy-arn arn:aws:iam::aws:policy/ReadOnlyAccess

aws iam update-assume-role-policy --role-name ${role-readonly} --policy-document file://policy-task3-readonly.json