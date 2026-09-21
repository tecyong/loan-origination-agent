package com.demo.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

@Component("genericRestApi")
@Description("Perform an HTTP request (GET, POST, etc.) to any public or internal REST API endpoint and return the response")
public class GenericRestApiTool implements Function<GenericRestApiTool.Request, GenericRestApiTool.Response> {

    private static final Logger log = LoggerFactory.getLogger(GenericRestApiTool.class);
    private final RestClient restClient;

    public record Request(
            @JsonPropertyDescription("HTTP Method: 'GET', 'POST', 'PUT', 'DELETE' (default is 'GET')")
            String method,

            @JsonProperty(required = true)
            @JsonPropertyDescription("The target URL to invoke, e.g. 'https://api.github.com/zen' or 'https://httpbin.org/get'")
            String url,

            @JsonPropertyDescription("Optional JSON body for POST/PUT requests, or null for GET")
            String jsonBody
    ) {}

    public record Response(String output) {}

    public GenericRestApiTool() {
        this.restClient = RestClient.builder().build();
    }

    @Override
    public Response apply(Request request) {
        String method = request.method();
        String url = request.url();
        String jsonBody = request.jsonBody();

        log.info("Executing GenericRestApiTool: {} {}", method, url);

        if (url == null || url.isBlank()) {
            return new Response("Error: URL parameter is required.");
        }

        try {
            HttpMethod httpMethod = HttpMethod.valueOf(method != null && !method.isBlank() ? method.trim().toUpperCase() : "GET");

            var reqSpec = restClient.method(httpMethod).uri(url);

            if (jsonBody != null && !jsonBody.isBlank() && (httpMethod == HttpMethod.POST || httpMethod == HttpMethod.PUT)) {
                reqSpec.header("Content-Type", "application/json");
                reqSpec.body(jsonBody);
            }

            ResponseEntity<String> response = reqSpec.retrieve().toEntity(String.class);

            int status = response.getStatusCode().value();
            String body = response.getBody();

            // Truncate response if overly verbose to avoid context flooding
            if (body != null && body.length() > 4000) {
                body = body.substring(0, 4000) + "\n... [Response truncated, exceeds 4000 characters]";
            }

            return new Response(String.format("HTTP Response [%d %s]:\n%s", status, response.getStatusCode(), body));
        } catch (Exception e) {
            log.error("Failed to execute API call to {}: {}", url, e.getMessage());
            return new Response("API Execution Error: " + e.getMessage());
        }
    }
}
