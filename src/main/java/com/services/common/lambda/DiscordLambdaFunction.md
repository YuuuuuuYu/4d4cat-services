## build.gradle
```
plugins {
    id 'java'
    id 'com.github.johnrengelman.shadow' version '8.1.1'
}

group = 'site'
version = '1.0-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

shadowJar {
    archiveClassifier.set('')
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'com.fasterxml.jackson.core:jackson-databind:2.15.3'

    compileOnly 'org.projectlombok:lombok:1.18.34'
    annotationProcessor 'org.projectlombok:lombok:1.18.34'

    implementation 'com.amazonaws:aws-lambda-java-events:3.11.4'
    implementation 'com.amazonaws:aws-lambda-java-core:1.2.3'
}
```

## code
```
package site;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;

public class DiscordLambdaFunction implements RequestHandler<Map<String, Object>, String> {

    private static final String DISCORD_WEBHOOK_URL = System.getenv("DISCORD_WEBHOOK_URL_ENV");
    private static final int DISCORD_COLOR_INFO = 3066993; // Green
    private static final int DISCORD_COLOR_ERROR = 15158332; // Red
    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        // Configure ObjectMapper to use fields for serialization since some classes (like Footer) might lack getters
        objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
    }

    @Override
    public String handleRequest(Map<String, Object> event, Context context) {
        var logger = context.getLogger();

        try {
            String alarmName = (String) event.get("AlarmName");
            String newState = (String) event.get("NewStateValue"); // ALARM, OK, INSUFFICIENT_DATA
            String reason = (String) event.get("NewStateReason");

            logger.log("Processing Alarm: " + alarmName + " (State: " + newState + ")");

            String title = "ALARM".equals(newState) ? "🚨 [Warning] " + alarmName : "✅ [OK] " + alarmName;
            int color = "ALARM".equals(newState) ? DISCORD_COLOR_ERROR : DISCORD_COLOR_INFO;

            Footer footer = Footer.builder()
                    .text("AWS CloudWatch Alarm System")
                    .build();

            Embed embed = Embed.builder()
                    .title(title)
                    .description(reason)
                    .color(color)
                    .timestamp(Instant.now().toString())
                    .footer(footer)
                    .build();

            DiscordWebhookPayload payload = DiscordWebhookPayload.builder()
                    .username("CloudWatch Monitor")
                    .embeds(Collections.singletonList(embed))
                    .build();

            sendWebhook(DISCORD_WEBHOOK_URL, payload);

            return "Successfully processed " + alarmName;

        } catch (Exception e) {
            logger.log("Error: " + e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private void sendWebhook(String webhookUrl, DiscordWebhookPayload payload) throws IOException {
        URL url = new URL(webhookUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String jsonPayload = objectMapper.writeValueAsString(payload);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            System.out.println("Discord webhook sent successfully. Response code: " + responseCode);
        } else {
            System.err.println("Failed to send Discord webhook. Response code: " + responseCode);
            try (InputStream errorStream = connection.getErrorStream()) {
                if (errorStream != null) {
                    String errorResponse = new String(errorStream.readAllBytes(), StandardCharsets.UTF_8);
                    System.err.println("Error response: " + errorResponse);
                }
            }
        }
    }
}
```
