#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <WebSocketsServer.h>

#include <ArduinoJson.h>
#include <bsec.h>
#include <BH1750.h>

// Elecrow board pins
#define D0 16
#define D1 5
#define D2 4
#define D3 0
#define D4 2
#define D5 14
#define D6 12
#define D7 13
#define D8 15

#define SEALEVELPRESSURE_HPA (1013.25)

BH1750 lightSensor;
Bsec iaqSensor;

struct iaqParametersStructure {
  float temperature;
  float pressure;
  float humidity;
  float iaq;
  float co2;
  float voc;
};

struct iaqParametersStructure iaqParameters;

const char* ssid = "TP-Link_1142";
const char* password = "43043224";
const int webSocketPort = 8080; // WebSocket port

ESP8266WebServer server(80);
WebSocketsServer webSocket = WebSocketsServer(webSocketPort);

void handleWebSocketEvent(uint8_t num, WStype_t type, uint8_t* payload, size_t length) {
  if (type == WStype_CONNECTED) {
    Serial.print("Connected client with number ");
    Serial.print(num);

    webSocket.sendTXT(num, "Connected to server");
  }

  if (type == WStype_TEXT) {
    Serial.print("Received message from client ");
    Serial.print(num);
    Serial.print(": ");
    Serial.println((char*)payload);

    // response sent to client
    webSocket.broadcastTXT(payload);
  }

  if (type == WStype_BIN) {
    Serial.print("Received command from client ");
    Serial.print(num);
    Serial.print(": ");
    Serial.println(*payload);

    // response sent to client
    webSocket.broadcastBIN(payload, sizeof(uint8_t));
  }
}

void handleLightSensor() {
  String response;
  StaticJsonDocument<200> jsonDocument;

  uint16_t lux = lightSensor.readLightLevel();

  jsonDocument["lux"] = String(lux);
  serializeJson(jsonDocument, response);

  server.send(200, "application/json", response);
}

void handleGasSensor() {
  String response;
  StaticJsonDocument<200> jsonDocument;

  // iaqParameters structure updates in loop
  jsonDocument["temperature"] = String(iaqParameters.temperature);
  jsonDocument["pressure"] = String(iaqParameters.pressure);
  jsonDocument["humidity"] = String(iaqParameters.humidity);
  jsonDocument["iaq"] = String(iaqParameters.iaq);
  jsonDocument["co2"] = String(iaqParameters.co2);
  jsonDocument["voc"] = String(iaqParameters.voc);
  serializeJson(jsonDocument, response);

  server.send(200, "application/json", response);
}

void handleNotFound() {
  String message = "File Not Found\n\n";
  message += "URI: ";
  message += server.uri();
  message += "\nMethod: ";
  message += (server.method() == HTTP_GET) ? "GET" : "POST";
  message += "\nArguments: ";
  message += server.args();
  message += "\n";

  for (uint8_t i = 0; i < server.args(); i++) {
    message += " " + server.argName(i) + ": " + server.arg(i) + "\n";
  }

  server.send(404, "text/plain", message);
}

void initBH1750() { lightSensor.begin(); }

void initBME680() {
  iaqSensor.begin(BME68X_I2C_ADDR_HIGH, Wire);
  checkIaqSensorStatus();

  bsec_virtual_sensor_t sensorList[13] = {
    BSEC_OUTPUT_IAQ,
    BSEC_OUTPUT_STATIC_IAQ,
    BSEC_OUTPUT_CO2_EQUIVALENT,
    BSEC_OUTPUT_BREATH_VOC_EQUIVALENT,
    BSEC_OUTPUT_RAW_TEMPERATURE,
    BSEC_OUTPUT_RAW_PRESSURE,
    BSEC_OUTPUT_RAW_HUMIDITY,
    BSEC_OUTPUT_RAW_GAS,
    BSEC_OUTPUT_STABILIZATION_STATUS,
    BSEC_OUTPUT_RUN_IN_STATUS,
    BSEC_OUTPUT_SENSOR_HEAT_COMPENSATED_TEMPERATURE,
    BSEC_OUTPUT_SENSOR_HEAT_COMPENSATED_HUMIDITY,
    BSEC_OUTPUT_GAS_PERCENTAGE
  };

  iaqSensor.updateSubscription(sensorList, 13, BSEC_SAMPLE_RATE_LP);
  checkIaqSensorStatus();
}

void checkIaqSensorStatus()
{
  String output;

  if (iaqSensor.bsecStatus != BSEC_OK) {
    if (iaqSensor.bsecStatus < BSEC_OK) {
      output = "BSEC error code : " + String(iaqSensor.bsecStatus);
      Serial.println(output);
      for (;;);
    } else {
      output = "BSEC warning code : " + String(iaqSensor.bsecStatus);
      Serial.println(output);
    }
  }

  if (iaqSensor.bme68xStatus != BME68X_OK) {
    if (iaqSensor.bme68xStatus < BME68X_OK) {
      output = "BME68X error code : " + String(iaqSensor.bme68xStatus);
      Serial.println(output);
      for (;;);
    } else {
      output = "BME68X warning code : " + String(iaqSensor.bme68xStatus);
      Serial.println(output);
    }
  }
}

void connectToWIFi() {
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi...");
  }

  IPAddress staticIP(192, 168, 0, 200);
  IPAddress gateway(192, 168, 0, 1);
  IPAddress subnet(255, 255, 255, 0);

  WiFi.config(staticIP, gateway, subnet);

  Serial.println("Connected to WiFi");
  Serial.print("IP:");
  Serial.println(WiFi.localIP());
}

void webSocketInit() {
  webSocket.begin();
  webSocket.onEvent(handleWebSocketEvent);
}

void httpServerInit() {
  // HTTP requests to server
  server.on("/", [](){
    server.send(200, "text/html", "Hello from ESP8266!");
  });
  server.on("/light", handleLightSensor);
  server.on("/gasParam", handleGasSensor);
  server.onNotFound(handleNotFound);

  // Server start
  server.begin();
}

void setup() {
  Serial.begin(115200);
  Wire.begin();
  initBH1750();
  initBME680();
  connectToWIFi();
  webSocketInit();
  httpServerInit();
}

void loop() {
  webSocket.loop();
  server.handleClient();

  if (iaqSensor.run()) {
    iaqParameters.temperature = iaqSensor.temperature;
    iaqParameters.pressure = iaqSensor.pressure / 100;
    iaqParameters.humidity = iaqSensor.humidity;
    iaqParameters.iaq = iaqSensor.iaq;
    iaqParameters.co2 = iaqSensor.co2Equivalent;
    iaqParameters.voc = iaqSensor.breathVocEquivalent;
  }
}