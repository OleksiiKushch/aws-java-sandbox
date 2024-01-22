package com.task07;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.util.StringInputStream;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.syndicate.deployment.annotations.events.RuleEventSource;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;

import java.io.InputStream;
import java.time.Instant;
import java.util.*;

@LambdaHandler(
		lambdaName = "uuid_generator",
		roleName = "uuid_generator-role"
)
@RuleEventSource(
		targetRule = "uuid_trigger"
)
public class UuidGenerator implements RequestHandler<Object, String> {

	private static final String PREFIX = "cmtr-4df2c6a7-";
	private static final String SUFFIX = "-test";
	private static final String BACKET_NAME = PREFIX + "uuid-storage" + SUFFIX;

	public String handleRequest(Object request, Context context) {
		List<String> uuids = new ArrayList<>();
		for(int i=0; i<10; i++) {
			uuids.add(UUID.randomUUID().toString());
		}

		Map<String, Object> data = new HashMap<>();
		data.put("ids", uuids);
		ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
		String jsonResul = "";
		try {
			jsonResul = ow.writeValueAsString(data);
		} catch (JsonProcessingException exception) {
			context.getLogger().log("Exception: " + exception.getMessage());
		}

		try {
			AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
			InputStream stream = new StringInputStream(jsonResul);
			ObjectMetadata meta = new ObjectMetadata();
			s3Client.putObject(BACKET_NAME, Instant.now().toString(), stream, meta);
		} catch(Exception exception) {
			context.getLogger().log("Exception: " + exception.getMessage());
		}
		context.getLogger().log("10 UUIDs successfully stored in S3 bucket");
		return "10 UUIDs successfully stored in S3 bucket";
	}
}
