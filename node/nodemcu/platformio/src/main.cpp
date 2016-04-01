#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <WiFiClient.h>
#include "DHT.h"
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"

#define DHTPIN 4
#define DHTTYPE DHT22

// Setup a DHT22 instance
DHT dht(DHTPIN, DHTTYPE);

bool connected = false;

void setup()
{
	pinMode(LED_BUILTIN, OUTPUT);
	Serial.begin(115200);
	WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
	dht.begin();
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

	digitalWrite(LED_BUILTIN, HIGH);
	// Reading temperature or humidity takes about 250 milliseconds!
	// Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
	float h = dht.readHumidity();
	// Read temperature as Celsius (the default)
	float t = dht.readTemperature();
	// Read temperature as Fahrenheit (isFahrenheit = true)
	float f = dht.readTemperature(true);
	digitalWrite(LED_BUILTIN, LOW);

	// Check if any reads failed and exit early (to try again).
	if (isnan(h) || isnan(t) || isnan(f)) {
		Serial.println("Failed to read from DHT sensor!");
		delay(2000);
		return;
	}

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
