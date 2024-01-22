package com.task02;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.util.HashMap;
import java.util.Map;

@LambdaHandler(lambdaName = "hello_world",
	roleName = "hello_world-role"
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class HelloWorld implements RequestHandler<Object, Map<String, Object>> {

	private static final String STATUS_CODE_ATTR = "statusCode";
	private static final String BODY_ATTR = "body";
	private static final int SUCCESS_HTTP_STATUS = 200;
	private static final String BODY_CONTENT = "{'statusCode': 200, 'message': 'Hello from Lambda'}";

	public Map<String, Object> handleRequest(Object request, Context context) {
		Map<String, Object> response = new HashMap<>();
		response.put(STATUS_CODE_ATTR, SUCCESS_HTTP_STATUS);
		response.put(BODY_ATTR, BODY_CONTENT);
		return response;
	}
}
