## package.json
```json
{
  "name": "discord-lambda-function",
  "version": "1.0.0",
  "description": "AWS Lambda function to send CloudWatch Alarms to Discord",
  "main": "index.mjs",
  "type": "module",
  "scripts": {
    "test": "echo \"Error: no test specified\" && exit 1"
  },
  "dependencies": {}
}
```

## code (index.mjs)
```javascript
/**
 * AWS Lambda handler for CloudWatch Alarms
 * Sends notifications to Discord via Webhook (ES Module)
 */

const DISCORD_WEBHOOK_URL = process.env.DISCORD_WEBHOOK_URL_ENV;
const DISCORD_COLOR_INFO = 3066993; // Green
const DISCORD_COLOR_ERROR = 15158332; // Red

export const handler = async (event) => {
    console.log("Received event:", JSON.stringify(event, null, 2));

    try {
        const alarmName = event.AlarmName;
        const newState = event.NewStateValue; // ALARM, OK, INSUFFICIENT_DATA
        const reason = event.NewStateReason;

        if (!alarmName) {
            console.log("No AlarmName found in event. Skipping.");
            return "No AlarmName found";
        }

        console.log(`Processing Alarm: ${alarmName} (State: ${newState})`);

        const title = newState === "ALARM" ? `🚨 [Warning] ${alarmName}` : `✅ [OK] ${alarmName}`;
        const color = newState === "ALARM" ? DISCORD_COLOR_ERROR : DISCORD_COLOR_INFO;

        const payload = {
            username: "CloudWatch Monitor",
            embeds: [
                {
                    title: title,
                    description: reason,
                    color: color,
                    timestamp: new Date().toISOString(),
                    footer: {
                        text: "AWS CloudWatch Alarm System"
                    }
                }
            ]
        };

        // Use built-in fetch (available in Node.js 18+)
        const response = await fetch(DISCORD_WEBHOOK_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(payload),
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(`Discord Webhook failed with status ${response.status}: ${errorText}`);
        }

        console.log(`Discord webhook sent successfully. Status: ${response.status}`);
        return `Successfully processed ${alarmName}`;

    } catch (error) {
        console.error("Error processing alarm:", error.message);
        return `Error: ${error.message}`;
    }
};
```
