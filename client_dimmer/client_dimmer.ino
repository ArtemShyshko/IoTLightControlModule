#include <ESP8266WiFi.h>
#include <ESP8266HTTPClient.h>
#include <WebSocketsClient.h>

#include <ArduinoJson.h>
#include <Ticker.h>
#include <PolledTimeout.h>

#define D0 16
#define D1 5
#define D2 4
#define D3 0
#define D4 2
#define D5 14
#define D6 12
#define D7 13
#define D8 15

#define INTERRUPT_PIN D7 // M2 pin
#define DIM_PIN D5       // M1 pin

#define MIN_BRIGHTNESS 500
#define MAX_BRIGHTNESS 2000
#define PERCENTAGE_SCALE ((MAX_BRIGHTNESS - MIN_BRIGHTNESS) / 100)
#define NORMAL_LUX 600

// WebSocket commands
#define LIGHTS_OFF 1
#define LIGHTS_ON 2
#define CHANGE_BRIGHTNESS 3
#define CURRENT_BRIGHTNESS 4
#define TOGGLE_ALARM 5
#define TOGGLE_AUTO_MODE 6

const char* ssid = "TP-Link_1142";
const char* password = "43043224";
const char* serverAddress = "192.168.0.200";
const int serverPort = 8080; // WebSocket port

WiFiClient client;
WebSocketsClient webSocket;

uint16_t currentBrightness = 0;
uint16_t rememberedBrightness = 0;
bool isBrightnessPassed = false;
bool isLightsOff = false;
bool isAlarmOn = false;
bool isAutoModeOn = false;

/* Dimmer control */
uint16_t power = 0;
bool isRise = false;

void ICACHE_RAM_ATTR handleInterrupt();

void ICACHE_RAM_ATTR onTimerISR() {
  if (currentBrightness != 0) {
    digitalWrite(DIM_PIN, HIGH);
    delayMicroseconds(40);
    digitalWrite(DIM_PIN, LOW);
    timer1_write(50000);
  }
}

void handleInterrupt() {
  if (currentBrightness == 0) {
    power = 0;
  } else {
    power = 49000 - 4.785 * currentBrightness;
  }
  timer1_write(power);

  if (isAlarmOn) {
    if (currentBrightness < MAX_BRIGHTNESS && isRise)
      currentBrightness += 500;   // плавное нарастание
    else
      isRise = false;

    if (currentBrightness > MIN_BRIGHTNESS && !isRise)
      currentBrightness -= 500;  // плавное угасание
    else
      isRise = true;
  }

}
/* Dimmer control */

/* WebSocket handling */
void webSocketEvent(WStype_t type, uint8_t* payload, size_t length) {
  if (type == WStype_BIN) {

    switch (*payload) {
      case LIGHTS_OFF:
        Serial.println("Command LIGHTS_OFF received");
        rememberedBrightness = currentBrightness;
        currentBrightness = 0;
        isLightsOff = true;
        break;
      case LIGHTS_ON:
        Serial.println("Command LIGHTS_ON received");
        if (isLightsOff) {
          currentBrightness = rememberedBrightness;
          isLightsOff = false;
        }
        break;
      case CHANGE_BRIGHTNESS:
        Serial.println("Command CHANGE_BRIGHTNESS received");
        isBrightnessPassed = true;
        break;
      case CURRENT_BRIGHTNESS: {
        Serial.println("Command CURRENT_BRIGHTNESS received");
        String str = String(mapToPercentage(currentBrightness));
        webSocket.sendTXT(str);
        break;
      }
      case TOGGLE_ALARM:
        Serial.println("Command TOGGLE_ALARM received");
        toggleAlarm();
        break;
      case TOGGLE_AUTO_MODE:
        Serial.println("Command TOGGLE_ALARM received");
        isAutoModeOn = !isAutoModeOn;
        break;
    }

  }

  if (type == WStype_TEXT && isBrightnessPassed) {
    uint8_t value = atoi((char*)payload);
    currentBrightness = mapToRange(value);
    Serial.print("Passed brightness is ");
    Serial.println(currentBrightness);
    isBrightnessPassed = false;
  }

}
/* WebSocket handling */

/* Helper functions */
void toggleAlarm() {
  isAlarmOn = !isAlarmOn;
  if (isAlarmOn) {
    rememberedBrightness = currentBrightness;
  } else {
    currentBrightness = rememberedBrightness;
  }
}

uint8_t mapToPercentage(uint16_t value) {
  if (value < MIN_BRIGHTNESS) {
    value = MIN_BRIGHTNESS;
  } else if (value > MAX_BRIGHTNESS) {
    value = MAX_BRIGHTNESS;
  }

  float percentage = (value - MIN_BRIGHTNESS) / PERCENTAGE_SCALE;
  return static_cast<uint8_t>(percentage);
}

uint16_t mapToRange(uint8_t percentage) {
  if (percentage < 0) {
    percentage = 0;
  } else if (percentage > 100) {
    percentage = 100;
  }

  uint16_t value = MIN_BRIGHTNESS + static_cast<uint16_t>(percentage * PERCENTAGE_SCALE);
  return value;
}

void regulateBrightness() {
  while (readLightLevel() > NORMAL_LUX + 100 &&
    currentBrightness >= MIN_BRIGHTNESS) {
    currentBrightness -= 50;
    Serial.println("going down");
    delay(50);
  }

  while (readLightLevel() < NORMAL_LUX - 50 &&
    currentBrightness < MAX_BRIGHTNESS) {
    currentBrightness += 50;
    Serial.println("going up");
    delay(50);
  }
}

void sendGetRequest(String reqUrl, String& response) {
  WiFiClient client;
  HTTPClient http;
  String url = "http://" + String(serverAddress) + reqUrl;

  if (http.begin(client, url)) {

    int httpCode = http.GET();

    if (httpCode > 0) {
      // File found at server
      if (httpCode == HTTP_CODE_OK || httpCode == HTTP_CODE_MOVED_PERMANENTLY) {
        response = http.getString();
      }
    } else {
      Serial.printf("[HTTP] GET... failed, error: %s\n", http.errorToString(httpCode).c_str());
    }

    http.end();
  } else {
    Serial.printf("[HTTP} Unable to connect\n");
  }
}

template <typename T>
void parseJsonData(const String jsonData, const String jsonValue, T& data) {
  DynamicJsonDocument jsonDoc(256);
  DeserializationError error = deserializeJson(jsonDoc, jsonData);

  if (error) {
    Serial.print("Error parsing JSON: ");
    Serial.println(error.c_str());
  } else {
    data = jsonDoc[jsonValue].as<T>();
  }
}

uint16_t readLightLevel() {
  String response = "";
  uint16_t lux = 0;
  sendGetRequest("/light", response);
  parseJsonData(response, "lux", lux);
  return lux;
}

bool readAlarmStatus() {
  String response = "";
  float iaq = 0;
  float co2 = 0;
  float voc = 0;
  sendGetRequest("/gasParam", response);
  parseJsonData(response, "iaq", iaq);
  parseJsonData(response, "co2", co2);
  parseJsonData(response, "voc", voc);
  return (iaq >= 120) || (co2 >= 1400) || (voc >= 1.5);
}

/* Helper functions */

/* Init functions */
void initDimmer() {
  pinMode(INTERRUPT_PIN, INPUT_PULLUP);
  pinMode(DIM_PIN, OUTPUT);
  attachInterrupt(digitalPinToInterrupt(INTERRUPT_PIN), handleInterrupt, RISING);
  timer1_attachInterrupt(onTimerISR);
  timer1_enable(TIM_DIV16, TIM_EDGE, TIM_SINGLE);
}

void wifiConnection() {
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }
  Serial.println("Connected to WiFi"); 
}

void initWebSocket() {
  webSocket.begin(serverAddress, serverPort);
  webSocket.onEvent(webSocketEvent);
}
/* Init functions */

void setup() {
  Serial.begin(115200);
  initDimmer();
  wifiConnection();
  initWebSocket();
}

void loop() {
  webSocket.loop();

  using periodic = esp8266::polledTimeout::periodicMs;
  static periodic regulationPing(2000);
  static periodic airQualityPing(3000);

  if (regulationPing && isAutoModeOn) {
    regulateBrightness();
  }

  if (airQualityPing) {
    if (readAlarmStatus()) {
      isAlarmOn = true;
    } else {
      isAlarmOn = false;
    }  
  }
}
