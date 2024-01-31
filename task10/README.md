# task10

High level project overview - business value it brings, non-detailed technical overview.

### Notice
All the technical details described below are actual for the particular
version, or a range of versions of the software.
### Actual for versions: 1.0.0

## task10 diagram

![task10](pics/task10_diagram.png)

## Lambdas descriptions

### Lambda `lambda-name`
Lambda feature overview.

### Required configuration
#### Environment variables
* environment_variable_name: description

#### Trigger event
```buildoutcfg
{
    "key": "value",
    "key1": "value1",
    "key2": "value3"
}
```
* key: [Required] description of key
* key1: description of key1

#### Expected response
```buildoutcfg
{
    "status": 200,
    "message": "Operation succeeded"
}
```
---

## Deployment from scratch
1. action 1 to deploy the software
2. action 2
...

## My steps

### Commands
1) syndicate generate project --name task09 && cd task09
2) syndicate generate config --name "personal-development-account" --region "eu-central-1" --bundle_bucket_name "syndicate-artefacts-MY_ACCOUNT_ID-eucentral1" --access_key "MY_ACCESS_KEY" --secret_key "MY_SECRET_KEY"
3) syndicate generate lambda --name api_handler --runtime java
4) syndicate generate meta dynamodb --resource_name Tables --hash_key_name id --hash_key_type N
5) syndicate generate meta dynamodb --resource_name Reservations --hash_key_name id --hash_key_type S
6) syndicate generate meta cognito_user_pool --resource_name simple-booking-userpool
7) syndicate generate meta api_gateway --resource_name task10_api --deploy_stage api
8) syndicate generate meta api_gateway_resource --api_name task10_api --path /signup
9) syndicate generate meta api_gateway_resource --api_name task10_api --path /signin
10) syndicate generate meta api_gateway_resource --api_name task10_api --path /tables
11) syndicate generate meta api_gateway_resource --api_name task10_api --path /tables/{tableId}
12) syndicate generate meta api_gateway_resource --api_name task10_api --path /reservations
13) syndicate generate meta api_gateway_resource_method --api_name task10_api --path /signup --method POST --integration_type lambda --lambda_name api_handler
14) syndicate generate meta api_gateway_resource_method --api_name task10_api --path /signin --method POST --integration_type lambda --lambda_name api_handler
15) syndicate generate meta api_gateway_resource_method --api_name task10_api --path /tables --method POST --integration_type lambda --lambda_name api_handler
16) syndicate generate meta api_gateway_resource_method --api_name task10_api --path /tables --method GET --integration_type lambda --lambda_name api_handler
17) syndicate generate meta api_gateway_resource_method --api_name task10_api --path /tables/{tableId} --method GET --integration_type lambda --lambda_name api_handler
18) syndicate generate meta api_gateway_resource_method --api_name task10_api --path /reservations --method POST --integration_type lambda --lambda_name api_handler
19) syndicate generate meta api_gateway_resource_method --api_name task10_api --path /reservations --method GET --integration_type lambda --lambda_name api_handler
20) add `"enable_proxy": true,` for `task10_api` in `deployment_resources.json` file
