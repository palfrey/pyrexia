#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include "DHT.h"
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"
#include <ArduinoJson.h>

#define DHTPIN 4
#define DHTTYPE DHT22

// Setup a DHT22 instance
DHT dht(DHTPIN, DHTTYPE);

bool connected = false;

// Store the MQTT server, username, and password in flash memory.
// This is required for using the Adafruit MQTT library.
const char MQTT_SERVER[] PROGMEM    = MQTT_HOST;
const char MQTT_USERNAME[] PROGMEM  = MQTT_USER;
const char MQTT_PASS[] PROGMEM  = MQTT_PASSWORD;

WiFiClient client;
Adafruit_MQTT_Client mqtt(&client, MQTT_SERVER, MQTT_PORT, MQTT_USERNAME, MQTT_PASS);
const char TEMP_FEED[] PROGMEM = "/temp";
Adafruit_MQTT_Publish temp = Adafruit_MQTT_Publish(&mqtt, TEMP_FEED);

char name[29];

void setup()
{
	pinMode(LED_BUILTIN, OUTPUT);
	Serial.begin(115200);
	WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
	dht.begin();

	byte mac[6];
	WiFi.macAddress(mac);
	sprintf(name, "temp-%02x:%02x:%02x:%02x:%02x:%02x", mac[0],mac[1],mac[2],mac[3],mac[4],mac[5]);
	Serial.printf("Name: %s\n", name);
}

void blink(int time) {
	digitalWrite(LED_BUILTIN, HIGH);
	delay(time);
	digitalWrite(LED_BUILTIN, LOW);
	delay(time);
}

void retryWifi() {
	blink(1000);
	connected = false;
	WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
}

void loop()
{
	wl_status_t status = WiFi.status();
	switch (status) {
		case WL_CONNECTED: // all good
			break;
		case WL_SCAN_COMPLETED:
		case WL_IDLE_STATUS:
			Serial.printf("Connecting to wifi (%d)\n", status);
			blink(500);
			return;

		case WL_NO_SSID_AVAIL:
			Serial.printf("Problem connecting to wifi: can't find SSID " WIFI_SSID "\n");
			retryWifi();
			return;

		case WL_DISCONNECTED:
		case WL_CONNECT_FAILED:
		case WL_CONNECTION_LOST:
			Serial.printf("Problem connecting to wifi: %d\n", status);
			retryWifi();
			return;

		case WL_NO_SHIELD:
			Serial.println("Can't find WiFi Shield!\n");
			blink(2000);
			return;
	}

	if (connected == false) {
		Serial.print("Connected to ");
		Serial.println(WIFI_SSID);
		Serial.print("IP address: ");
		Serial.println(WiFi.localIP());
		connected = true;
	}

	if (!mqtt.connected()) {
		Serial.println("Connecting to MQTT... " MQTT_HOST " " MQTT_USER);
		int ret = mqtt.connect();
		if (ret != 0) {
			Serial.println(mqtt.connectErrorString(ret));
			Serial.println("Retrying MQTT connection in 5 seconds...");
			mqtt.disconnect();
			delay(5000);
			return;
		}
		else {
			Serial.println("Connected to " MQTT_HOST " " MQTT_USER);
		}
	}

	digitalWrite(LED_BUILTIN, HIGH);
	// Reading temperature or humidity takes about 250 milliseconds!
	// Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
	float h = dht.readHumidity();
	// Read temperature as Celsius (the default)
	float t = dht.readTemperature();
	// Read temperature as Fahrenheit (isFahrenheit = true)
	float f = dht.readTemperature(true);
	digitalWrite(LED_BUILTIN, LOW);

	StaticJsonBuffer<200> jsonBuffer;
	JsonObject& root = jsonBuffer.createObject();
	root["temp"] = t;
	root["humid"] = h;
	root["id"] = name;

	char buffer[256];
	root.printTo(buffer, sizeof(buffer));

	Serial.printf("Buffer: %s\n", buffer);

	if (!temp.publish(buffer)) {
		Serial.println(F("Failed MQTT"));
	} else {
		Serial.println(F("MQTT OK!"));
	}

	// Check if any reads failed and exit early (to try again).
	if (isnan(h) || isnan(t) || isnan(f)) {
		Serial.println("Failed to read from DHT sensor!");
	}
	else {
		// Compute heat index in Fahrenheit (the default)
		float hif = dht.computeHeatIndex(f, h);
		// Compute heat index in Celsius (isFahreheit = false)
		float hic = dht.computeHeatIndex(t, h, false);

		Serial.print("Humidity: ");
		Serial.print(h);
		Serial.print(" %\t");
		Serial.print("Temperature: ");
		Serial.print(t);
		Serial.print(" *C ");
		Serial.print(f);
		Serial.print(" *F\t");
		Serial.print("Heat index: ");
		Serial.print(hic);
		Serial.print(" *C ");
		Serial.print(hif);
		Serial.println(" *F");
	}
	delay(5000);
}
