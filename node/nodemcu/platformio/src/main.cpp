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

const char* ssid = "******";
const char* password = "******";
bool connected = false;

void setup()
{
  pinMode(LED_BUILTIN, OUTPUT);
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  dht.begin();
}

void loop()
{
	wl_status_t status = WiFi.status();
	switch (status) {
		case WL_CONNECTED: // all good
			break;
		case WL_SCAN_COMPLETED:
		case WL_IDLE_STATUS:
			// in the process of connecting...
			Serial.printf("Connecting to wifi (%d)\n", status);
			delay(1000);
			return;

		case WL_DISCONNECTED:
		case WL_NO_SSID_AVAIL:
		case WL_CONNECT_FAILED:
		case WL_CONNECTION_LOST:
			// wait briefly, then have another go
			Serial.printf("Problem connecting to wifi: %d\n", status);
			delay(1000);
			WiFi.begin(ssid, password);
			return;
		case WL_NO_SHIELD:
			Serial.println("Can't find WiFi Shield!\n");
			delay(5000);
			return;
	}

    Serial.print("Connected to ");
    Serial.println(ssid);
    Serial.print("IP address: ");
    Serial.println(WiFi.localIP());

    // turn the LED on (HIGH is the voltage level)
    digitalWrite(LED_BUILTIN, HIGH);

    // wait for a second
    delay(1000);

    // turn the LED off by making the voltage LOW
    digitalWrite(LED_BUILTIN, LOW);

     // wait for a second
    delay(1000);

    // Reading temperature or humidity takes about 250 milliseconds!
    // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
    float h = dht.readHumidity();
    // Read temperature as Celsius (the default)
    float t = dht.readTemperature();
    // Read temperature as Fahrenheit (isFahrenheit = true)
    float f = dht.readTemperature(true);

    // Check if any reads failed and exit early (to try again).
    if (isnan(h) || isnan(t) || isnan(f)) {
      Serial.println("Failed to read from DHT sensor!");
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
