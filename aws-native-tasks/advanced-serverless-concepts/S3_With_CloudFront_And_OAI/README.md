Note: Make sure that we use cmd (not PowerShell).

# Set config data
set AWS_ACCESS_KEY_ID=${aws-access-key-id}
set AWS_SECRET_ACCESS_KEY=${aws-secret-access-key}
set AWS_SESSION_TOKEN=${aws-session-token}

aws s3api put-public-access-block --bucket ${bucket} --public-access-block-configuration "BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true"

aws cloudfront get-distribution-config --id ${distribution-id} > config.json
Replace next: `"OriginAccessIdentity": "origin-access-identity/cloudfront/${origin-access-identity-id}"`
Remove: `"ETag": "${e-tag}",`
Replace next:
```
"ResponsePagePath": "/error.html",
"ResponseCode": "404",          
```
aws cloudfront update-distribution --id ${distribution-id} --if-match ${e-tag} --cli-input-json file://config.json
aws s3api put-bucket-policy --bucket ${bucket} --policy file://updated-s3-policy.json