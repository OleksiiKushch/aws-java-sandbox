package com.task01;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "hello_world",
	roleName = "hello_world-role",
	isPublishVersion = true,
	aliasName = "${lambdas_alias_name}"
)
public class HelloWorld implements RequestHandler<Object, Map<String, Object>> {

	private static final String STATUS_CODE_ATTR = "statusCode";
	private static final String MESSAGE_ATTR = "message";
	private static final int SUCCESS_HTTP_STATUS = 200;
	private static final String MESSAGE = "Hello from Lambda";

	public Map<String, Object> handleRequest(Object request, Context context) {
		Map<String, Object> resultMap = new HashMap<>();
		resultMap.put(STATUS_CODE_ATTR, SUCCESS_HTTP_STATUS);
		resultMap.put(MESSAGE_ATTR, MESSAGE);
		return resultMap;
	}
}
