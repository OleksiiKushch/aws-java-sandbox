# task09

High level project overview - business value it brings, non-detailed technical overview.

### Notice
All the technical details described below are actual for the particular
version, or a range of versions of the software.
### Actual for versions: 1.0.0

## task09 diagram

![task09](pics/task09_diagram.png)

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
3) syndicate generate lambda --name processor --runtime java
4) syndicate generate meta dynamodb --resource_name Weather --hash_key_name id --hash_key_type S

add `tracingMode = TracingMode.Active` to @LambdaHandler
add responsive policies for lambda

