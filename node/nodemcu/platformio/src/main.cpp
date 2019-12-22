#include "Arduino.h"
#include <ESP8266WiFi.h>
#include <ESP8266WiFiMulti.h>
#include <WiFiClient.h>
#include "DHT.h"
#include "Adafruit_MQTT.h"
#include "Adafruit_MQTT_Client.h"
#include <ArduinoJson.h>

#define DHTPIN 4
#define DHTTYPE DHT22

void ds18b20();

#include <WEMOS_SHT3X.h>

SHT3X sht30(0x45);

#define SECONDS_BETWEEN_READINGS 5

// Setup a DHT22 instance
DHT dht(DHTPIN, DHTTYPE);

bool connected = false;
unsigned long wifi_start_time;

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

void onDisconnect(const WiFiEventStationModeDisconnected &event) {
	Serial.printf("disconnect reason: %d\n", event.reason);
}

ESP8266WiFiMulti wifiMulti;
boolean connectionWasAlive = true;

void setup()
{
	//ESP.eraseConfig();
	pinMode(LED_BUILTIN, OUTPUT);
	Serial.begin(115200);
	Serial.setDebugOutput(true);
	WiFiEventHandler disconnectedEventHandler = WiFi.onStationModeDisconnected([](const WiFiEventStationModeDisconnected& event)
	{
	  Serial.println("Station disconnected");
  });


	//wifiMulti.addAP(WIFI_SSID, WIFI_PASSWORD);
	return;
	//Serial.printf("Initial status: %d\n", status);
	wifi_start_time = millis();
	//dht.begin();

	byte mac[6];
	WiFi.mode(WIFI_STA);
	//WiFi.onStationModeDisconnected(onDisconnect);
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

void printEncryptionType(int thisType) {
  // read the encryption type and print out the name:
  switch (thisType) {
    case ENC_TYPE_WEP:
      Serial.println("WEP");
      break;
    case ENC_TYPE_TKIP:
      Serial.println("WPA");
      break;
    case ENC_TYPE_CCMP:
      Serial.println("WPA2");
      break;
    case ENC_TYPE_NONE:
      Serial.println("None");
      break;
    case ENC_TYPE_AUTO:
      Serial.println("Auto");
      break;
  }
}

void listNetworks() {
  // scan for nearby networks:
  Serial.println("** Scan Networks **");
  int numSsid = WiFi.scanNetworks();
  if (numSsid == -1) {
    Serial.println("Couldn't get a wifi connection");
    while (true);
  }

  // print the list of networks seen:
  Serial.print("number of available networks:");
  Serial.println(numSsid);

  // print the network number and name for each network found:
  for (int thisNet = 0; thisNet < numSsid; thisNet++) {
    Serial.print(thisNet);
    Serial.print(") ");
    Serial.print(WiFi.SSID(thisNet));
    Serial.print("\tSignal: ");
    Serial.print(WiFi.RSSI(thisNet));
    Serial.print(" dBm");
    Serial.print("\tEncryption: ");
    printEncryptionType(WiFi.encryptionType(thisNet));
  }
}

void retryWifi() {
	WiFi.disconnect();
	Serial.printf("Wifi mode: %d\n", WiFi.getMode());
	blink(1000);
	connected = false;
	//listNetworks();
	//blink(1000);
	wl_status_t status = WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
	Serial.printf("Initial status: %d\n", status);
	wifi_start_time = millis();
}

void loop()
{
	// if (wifiMulti.run() != WL_CONNECTED)
  //   {
  //     if (connectionWasAlive == true)
  //     {
  //       connectionWasAlive = false;
  //       Serial.print("Looking for WiFi ");
  //     }
  //     Serial.print(".");
  //     delay(500);
	//   return;
  //   }
  //   else if (connectionWasAlive == false)
  //   {
  //     connectionWasAlive = true;
  //     Serial.printf(" connected to %s\n", WiFi.SSID().c_str());
  //   }

	if (connected == false) {
		Serial.print("Connected to ");
		Serial.println(WIFI_SSID);
		Serial.print("IP address: ");
		Serial.println(WiFi.localIP());
		connected = true;
	}

	/*if (!mqtt.connected()) {
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
			Serial.println("Connected");
		}
	}*/

	digitalWrite(LED_BUILTIN, HIGH);
	// Reading temperature or humidity takes about 250 milliseconds!
	// Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
	// float h = dht.readHumidity();
	// // Read temperature as Celsius (the default)
	// float t = dht.readTemperature();

	// sht30.get();
	// float t = sht30.cTemp;
	// float h = sht30.humidity;
	// //digitalWrite(LED_BUILTIN, LOW);

	// StaticJsonBuffer<200> jsonBuffer;
	// JsonObject& root = jsonBuffer.createObject();
	// if (isnan(t))
	// 	root["temp"] = NULL;
	// else
	// 	root["temp"] = t;
	// if (isnan(h))
	// 	root["humid"] = NULL;
	// else
	// 	root["humid"] = h;
	// root["id"] = name;

	// char buffer[256];
	// root.printTo(buffer, sizeof(buffer));

	// Serial.printf("Buffer: %s\n", buffer);

	// /*if (!temp.publish(buffer)) {
	// 	Serial.println(F("Failed MQTT"));
	// } else {
	// 	Serial.println(F("MQTT OK!"));
	// }*/

	// if (isnan(h) || isnan(t)) {
	// 	Serial.println("Failed to read from DHT sensor!");
	// }
	ds18b20();
	delay(SECONDS_BETWEEN_READINGS * 1000);
}
