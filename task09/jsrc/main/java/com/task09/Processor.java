package com.task09;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.syndicate.deployment.annotations.LambdaUrlConfig;
import com.syndicate.deployment.annotations.lambda.LambdaHandler;
import com.syndicate.deployment.model.lambda.url.AuthType;
import com.syndicate.deployment.model.lambda.url.InvokeMode;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.net.URL;
import java.util.*;

@LambdaHandler(
		lambdaName = "processor",
		roleName = "processor-role"
)
@LambdaUrlConfig(
		authType = AuthType.NONE,
		invokeMode = InvokeMode.BUFFERED
)
public class Processor implements RequestHandler<Object, String> {

	private static final String MAIN_URL = "https://api.open-meteo.com/v1/forecast";
	private static final String LATITUDE = "latitude=50.4375";
	private static final String LONGITUDE = "longitude=30.5";
	private static final String CURRENT_ATTR = "current=temperature_2m,wind_speed_10m";
	private static final String HOURLY_ATTR = "hourly=temperature_2m,relative_humidity_2m,wind_speed_10m";

	private static final String TABLE_NAME = "Weather";

	public String handleRequest(Object request, Context context) {
		try {
			String response = getLatestWeatherForecast();

			Gson gson = new Gson();
			Type type = new TypeToken<Map<String, Object>>() {}.getType();
			Map<String, Object> inputMap = gson.fromJson(response, type);

			Map<String, AttributeValue> item = new HashMap<>();
			item.put("id", new AttributeValue(UUID.randomUUID().toString()));
			item.put("forecast", new AttributeValue().withM(convertToDynamoDBMap(inputMap)));

			AmazonDynamoDB dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
			dynamoDb.putItem(TABLE_NAME, item);

			return "Success";
		} catch (IOException e) {
            context.getLogger().log("Error: " + e.getMessage());
			return "Failure";
        }
	}

	private String getLatestWeatherForecast() throws IOException {
		URL url = new URL(MAIN_URL + "?" + LATITUDE + "&" + LONGITUDE + "&" + CURRENT_ATTR + "&" + HOURLY_ATTR);
		Scanner scanner = new Scanner((InputStream) url.getContent());
		StringBuilder temp = new StringBuilder();
		while (scanner.hasNext()){
			temp.append(scanner.nextLine());
		}
		return temp.toString();
	}

	private Map<String, AttributeValue> convertToDynamoDBMap(Map<String, Object> map) {
		Map<String, AttributeValue> item = new HashMap<>();
		for (String key : map.keySet()) {
			Object value = map.get(key);
			if (value instanceof Map) {
				Map<String, Object> subMap = (Map<String, Object>) value;
				item.put(key, new AttributeValue().withM(convertToDynamoDBMap(subMap)));
			} else if (value instanceof List) {
				List<Object> subList = (List<Object>) value;
				item.put(key, new AttributeValue().withL(convertToDynamoDBList(subList)));
			} else if (value instanceof String) {
				item.put(key, new AttributeValue((String) value));
			} else if (value instanceof Number) {
				item.put(key, new AttributeValue().withN(value.toString()));
			}
		}
		return item;
	}

	private List<AttributeValue> convertToDynamoDBList(List<Object> list) {
		List<AttributeValue> dynamoDBList = new ArrayList<>();
		for (Object value : list) {
			if (value instanceof Map) {
				Map<String, Object> subMap = (Map<String, Object>) value;
				dynamoDBList.add(new AttributeValue().withM(convertToDynamoDBMap(subMap)));
			} else if (value instanceof List) {
				List<Object> subList = (List<Object>) value;
				dynamoDBList.add(new AttributeValue().withL(convertToDynamoDBList(subList)));
			} else if (value instanceof String) {
				dynamoDBList.add(new AttributeValue((String) value));
			} else if (value instanceof Number) {
				dynamoDBList.add(new AttributeValue().withN(value.toString()));
			}
		}
		return dynamoDBList;
	}
}
