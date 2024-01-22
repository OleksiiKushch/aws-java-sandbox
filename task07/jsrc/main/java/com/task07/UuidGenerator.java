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
public class UuidGenerator implements RequestHandler<Object, Void> {

    private static final String PREFIX = "cmtr-4df2c6a7-";
    private static final String SUFFIX = "-test";
    private static final String BACKET_NAME = PREFIX + "uuid-storage" + SUFFIX;
	private static final int NUMBER_OF_UUIDS = 10;
    private static final String SUCCESS_MESSAGE = NUMBER_OF_UUIDS + " UUIDs successfully stored in S3 bucket";
    private static final String IDS_ATTR = "ids";

    public Void handleRequest(Object request, Context context) {
        List<String> uuids = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_UUIDS; i++) {
            uuids.add(UUID.randomUUID().toString());
        }

        Map<String, Object> data = new HashMap<>();
        data.put(IDS_ATTR, uuids);
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
        } catch (Exception exception) {
            context.getLogger().log("Exception: " + exception.getMessage());
        }
        context.getLogger().log(SUCCESS_MESSAGE);
        return null;
    }
}
