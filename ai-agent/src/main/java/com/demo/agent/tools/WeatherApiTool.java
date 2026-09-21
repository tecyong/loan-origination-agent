package com.demo.agent.tools;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

@Component("weatherLookup")
@Description("Get current real-time weather and temperature for any city worldwide using Open-Meteo REST API")
public class WeatherApiTool implements Function<WeatherApiTool.Request, WeatherApiTool.Response> {

    private static final Logger log = LoggerFactory.getLogger(WeatherApiTool.class);
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("The city name to query, e.g. Tokyo, New York, London, Paris, Singapore")
            String city
    ) {}

    public record Response(String report) {}

    public WeatherApiTool() {
        this.restClient = RestClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public Response apply(Request request) {
        String city = request.city();
        log.info("Executing WeatherApiTool for city: {}", city);
        try {
            // Step 1: Geocoding lookup
            String geoUrl = "https://geocoding-api.open-meteo.com/v1/search?name="
                    + URLEncoder.encode(city, StandardCharsets.UTF_8)
                    + "&count=1&language=en&format=json";

            String geoResp = restClient.get().uri(geoUrl).retrieve().body(String.class);
            JsonNode geoJson = objectMapper.readTree(geoResp);

            if (!geoJson.has("results") || geoJson.get("results").isEmpty()) {
                return new Response("Could not find coordinates for city: " + city);
            }

            JsonNode location = geoJson.get("results").get(0);
            double latitude = location.get("latitude").asDouble();
            double longitude = location.get("longitude").asDouble();
            String name = location.path("name").asText(city);
            String country = location.path("country").asText("");

            // Step 2: Forecast & current weather
            String weatherUrl = String.format(
                    "https://api.open-meteo.com/v1/forecast?latitude=%.4f&longitude=%.4f&current=temperature_2m,relative_humidity_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&wind_speed_unit=kmh",
                    latitude, longitude);

            String weatherResp = restClient.get().uri(weatherUrl).retrieve().body(String.class);
            JsonNode weatherJson = objectMapper.readTree(weatherResp);
            JsonNode current = weatherJson.get("current");

            if (current == null) {
                return new Response("Failed to retrieve current weather data for " + city);
            }

            double temp = current.path("temperature_2m").asDouble();
            double feelsLike = current.path("apparent_temperature").asDouble();
            int humidity = current.path("relative_humidity_2m").asInt();
            double windSpeed = current.path("wind_speed_10m").asDouble();
            int weatherCode = current.path("weather_code").asInt();

            String condition = getWeatherDescription(weatherCode);

            String report = String.format(
                    "Weather Report for %s, %s (Lat: %.2f, Lon: %.2f):\n" +
                    "- Condition: %s\n" +
                    "- Temperature: %.1f°C (Feels like: %.1f°C)\n" +
                    "- Humidity: %d%%\n" +
                    "- Wind Speed: %.1f km/h",
                    name, country, latitude, longitude, condition, temp, feelsLike, humidity, windSpeed
            );
            return new Response(report);
        } catch (Exception e) {
            log.error("WeatherApiTool error for {}: {}", city, e.getMessage());
            return new Response("Error retrieving weather for " + city + ": " + e.getMessage());
        }
    }

    private String getWeatherDescription(int code) {
        return switch (code) {
            case 0 -> "Clear sky ☀️";
            case 1, 2, 3 -> "Mainly clear, partly cloudy ⛅";
            case 45, 48 -> "Foggy 🌫️";
            case 51, 53, 55 -> "Drizzle 🌦️";
            case 61, 63, 65 -> "Rain 🌧️";
            case 71, 73, 75 -> "Snow fall ❄️";
            case 80, 81, 82 -> "Rain showers 🌧️";
            case 95, 96, 99 -> "Thunderstorm ⛈️";
            default -> "Overcast / Variable ☁️";
        };
    }
}
