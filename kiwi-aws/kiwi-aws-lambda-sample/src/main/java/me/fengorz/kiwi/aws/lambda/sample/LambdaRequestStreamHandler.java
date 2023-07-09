package me.fengorz.kiwi.aws.lambda.sample;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class LambdaRequestStreamHandler implements RequestStreamHandler {

    private final JSONParser parser = new JSONParser();

    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        LambdaLogger logger = context.getLogger();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        try {
            JSONObject request = (JSONObject) parser.parse(reader);
            JSONObject responseBody = new JSONObject();
            responseBody.put("message", request.get("message"));

            responseJson.put("statusCode", 200);
            responseJson.put("body", responseBody.toString());
        } catch (ParseException e) {
            logger.log("Error: " + e);
            responseJson.put("statusCode", 400);
            responseJson.put("exception", e);
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
        writer.write(responseJson.toString());
        writer.close();

    }
}
