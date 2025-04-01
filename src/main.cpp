#include <HTTPClient.h>
#include "ArduinoJson.h"
#include <NTPClient.h>
#include <WiFiUdp.h>
#include <pwmWrite.h>
#include <PubSubClient.h>
#include <Adafruit_Sensor.h>
#include <DHT.h>
#include <DHT_U.h>

int idSensor;
float actu;


// Replace 0 by ID of this current device
const int DEVICE_ID = 124;

int test_delay = 400; // so we don't spam the API
boolean describe_tests = true;

// Replace 0.0.0.0 by your server local IP (ipconfig [windows] or ifconfig [Linux o MacOS] gets IP assigned to your PC)
//String serverName = "http://192.168.59.107:8080/";



//****************PEPE*********************
//String serverName = "http://192.168.0.94:8080/";

String serverName = "http://192.168.69.14:8080/";
//****************FIN PEPE********************** */

HTTPClient http;

// Replace WifiName and WifiPassword by your WiFi credentials
#define STASSID "oPPo"    //"Your_Wifi_SSID"
#define STAPSK "DAD_G10_TieneNota10" //"Your_Wifi_PASSWORD"

// Replace WifiName and WifiPassword by your WiFi credentials
//#define STASSID "Galaxy Z Fold2 5G303F"    //"Your_Wifi_SSID"
//#define STAPSK "alberto12" //"Your_Wifi_PASSWORD"


#define DHTTYPE DHT11
#define DHTPIN 26 // pin de placa, donde pone G26  //******************CAMBIAR CUANDO MONTE CIRCUITO**************************** */
#define BUZZER_PIN 27 // pin de placa, donde pone G27   //******************CAMBIAR CUANDO MONTE CIRCUITO**************************** */
#define TEMPERATURE_THRESHOLD 30   //******************CAMBIAR CUANDO SEPAMOS UMBRAL**************************** */

// NTP (Net time protocol) settings
WiFiUDP ntpUDP;
NTPClient timeClient(ntpUDP);

// MQTT configuration
WiFiClient espClient;
PubSubClient client(espClient);


//****************PEPE*********************

// Server IP, where de MQTT broker is deployed
//const char *MQTT_BROKER_ADRESS = "192.168.0.94";
const char *MQTT_BROKER_ADRESS = "192.168.69.14"
const uint16_t MQTT_PORT = 1883;

//****************FIN PEPE*********************

// Name for this MQTT client
const char *MQTT_CLIENT_NAME = "ArduinoClient_1";        //************** BUENA PREGUNTA ********************* */

// Pinout settings
const int analogSensorPin = 34;   //******************CAMBIAR CUANDO MONTE CIRCUITO**************************** */
const int digitalSensorPin = 13;   //******************CAMBIAR CUANDO MONTE CIRCUITO**************************** */
const int actuatorPin = 15;       //******************CAMBIAR CUANDO MONTE CIRCUITO**************************** */
const int analogActuatorPin = 16;  //******************CAMBIAR CUANDO MONTE CIRCUITO**************************** */

// Property initialization for servo movement using PWM signal
Pwm pwm = Pwm();

// callback a ejecutar cuando se recibe un mensaje
// en este ejemplo, muestra por serial el mensaje recibido



void deserializeActuatorsFromDevice(int httpResponseCode);


String mensaje="";
void OnMqttReceived(char *topic, byte *payload, unsigned int length)
{
  Serial.print("Received on ");
  Serial.print(topic);
  Serial.print(": ");

  String content = "";
  for (size_t i = 0; i < length; i++)
  {
    content.concat((char)payload[i]);
  }
  Serial.print(content);
  Serial.println();
  if (content == "0" || content == "1"){
  mensaje = content;

  }
}

// inicia la comunicacion MQTT
// inicia establece el servidor y el callback al recibir un mensaje
void InitMqtt()
{
  client.setServer(MQTT_BROKER_ADRESS, MQTT_PORT);
  client.setCallback(OnMqttReceived);
}



DHT_Unified dht(DHTPIN, DHTTYPE);

uint32_t delayMS;
// Setup

unsigned long startTime;
void setup()
{
  Serial.begin(9600);
  Serial.println();
  Serial.print("Connecting to ");
  Serial.println(STASSID);
  pinMode(BUZZER_PIN, OUTPUT);

  /* Explicitly set the ESP32 to be a WiFi-client, otherwise, it by default,
     would try to act as both a client and an access-point and could cause
     network-issues with your other WiFi-devices on your WiFi-network. */
  WiFi.mode(WIFI_STA);
  WiFi.begin(STASSID, STAPSK);

  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }

  InitMqtt();

  Serial.println("");
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
  Serial.println("Setup!");

  // Configure pin modes for actuators (output mode) and sensors (input mode). Pin numbers should be described by GPIO number (https://www.upesy.com/blogs/tutorials/esp32-pinout-reference-gpio-pins-ultimate-guide)
  // For ESP32 WROOM 32D https://uelectronics.com/producto/esp32-38-pines-esp-wroom-32/
  // You must find de pinout for your specific board version
  pinMode(actuatorPin, OUTPUT);
  pinMode(analogSensorPin, INPUT);
  pinMode(digitalSensorPin, INPUT);
  pinMode(analogActuatorPin, OUTPUT);

   String response;

  // Init and get the time
  timeClient.begin();

 


  // Declarar la variable global para almacenar el valor del sensor de temperatura



}

String response;

String serializeSensorValueBody(float value, int idSensor, long timestamp)
{
  // StaticJsonObject allocates memory on the stack, it can be
  // replaced by DynamicJsonDocument which allocates in the heap.
  
  //
  DynamicJsonDocument doc(2048);

  // Add values in the document
  //
  doc["idSensor"] = idSensor;
  doc["timestamp"] = timestamp;
  doc["value"] = value;
  doc["removed"] = false;

  // Generate the minified JSON and send it to the Serial port.
  //
  String output;
  serializeJson(doc, output);
  Serial.println(output);

  return output;
}


String serializeActuatorStatusBody(float status, bool statusBinary, int idActuator, long timestamp)
{
  DynamicJsonDocument doc(2048);

  doc["status"] = status;
  doc["statusBinary"] = statusBinary;
  doc["idActuator"] = idActuator;
  doc["timestamp"] = timestamp;
  doc["removed"] = false;

  String output;
  serializeJson(doc, output);
  return output;

}

String serializeDeviceBody(String deviceSerialId, String name, String mqttChannel, int idGroup)
{
  DynamicJsonDocument doc(2048);

  doc["deviceSerialId"] = deviceSerialId;
  doc["name"] = name;
  doc["mqttChannel"] = mqttChannel;
  doc["idGroup"] = idGroup;

  String output;
  serializeJson(doc, output);
  return output;
}



void deserializeActuatorStatusBody(String responseJson)
{
  if (responseJson != "")
  {
    DynamicJsonDocument doc(2048);

    // Deserialize the JSON document
    DeserializationError error = deserializeJson(doc, responseJson);

    // Test if parsing succeeds.
    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // Fetch values.
    int idActuatorState = doc["idActuatorState"];
    float status = doc["status"];
    bool statusBinary = doc["statusBinary"];
    int idActuator = doc["idActuator"];
    long timestamp = doc["timestamp"];

    Serial.println(("Actuator status deserialized: [idActuatorState: " + String(idActuatorState) + ", status: " + String(status) + ", statusBinary: " + String(statusBinary) + ", idActuator" + String(idActuator) + ", timestamp: " + String(timestamp) + "]").c_str());
  }
}

void deserializeDeviceBody(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    DynamicJsonDocument doc(2048);

    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    int idDevice = doc["idDevice"];
    String deviceSerialId = doc["deviceSerialId"];
    String name = doc["name"];
    String mqttChannel = doc["mqttChannel"];
    int idGroup = doc["idGroup"];

    Serial.println(("Device deserialized: [idDevice: " + String(idDevice) + ", name: " + name + ", deviceSerialId: " + deviceSerialId + ", mqttChannel" + mqttChannel + ", idGroup: " + idGroup + "]").c_str());
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void deserializeSensorsFromDevice(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    // allocate the memory for the document
    DynamicJsonDocument doc(ESP.getMaxAllocHeap());

    // parse a JSON array
    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // extract the values
    JsonArray array = doc.as<JsonArray>();
    for (JsonObject sensor : array)
    {
      int idSensor = sensor["idSensor"];
      String name = sensor["name"];
      String sensorType = sensor["sensorType"];
      int idDevice = sensor["idDevice"];

      Serial.println(("Sensor deserialized: [idSensor: " + String(idSensor) + ", name: " + name + ", sensorType: " + sensorType + ", idDevice: " + String(idDevice) + "]").c_str());
    }
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}



void deserializeActuatorsFromDevice(int httpResponseCode)
{

  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String responseJson = http.getString();
    // allocate the memory for the document
    DynamicJsonDocument doc(ESP.getMaxAllocHeap());

    // parse a JSON array
    DeserializationError error = deserializeJson(doc, responseJson);

    if (error)
    {
      Serial.print(F("deserializeJson() failed: "));
      Serial.println(error.f_str());
      return;
    }

    // extract the values
    JsonArray array = doc.as<JsonArray>();
    for (JsonObject sensor : array)
    {
      
      int idActuator = sensor["idActuator"];
      String name = sensor["name"];
      String actuatorType = sensor["actuatorType"];
      int idDevice = sensor["idDevice"];
      
      Serial.println(("Actuator deserialized: [idActuator: " + String(idActuator) + ", name: " + name + ", actuatorType: " + actuatorType + ", idDevice: " + String(idDevice) + "]").c_str());
  }
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }


}

void test_response(int httpResponseCode)
{
  //delay(test_delay);
  if (httpResponseCode > 0)
  {
    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);
    String payload = http.getString();
    Serial.println(payload);
  }
  else
  {
    Serial.print("Error code: ");
    Serial.println(httpResponseCode);
  }
}

void describe(char *description)
{
  if (describe_tests)
    Serial.println(description);
}

void GET_tests()
{
  describe("Test GET full device info");
  String serverPath = serverName + "api/devices/" + String(DEVICE_ID);
  http.begin(serverPath.c_str());
  test_response(http.GET());
  deserializeDeviceBody(http.GET());

  describe("Test GET sensors from deviceID");
  serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/sensors";
  http.begin(serverPath.c_str());
  deserializeSensorsFromDevice(http.GET());

  describe("Test GET actuators from deviceID");
  serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/actuators";
  http.begin(serverPath.c_str());
  deserializeActuatorsFromDevice(http.GET());

  describe("Test GET sensors from deviceID and Type");
  serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/sensors/Temperature";
  http.begin(serverPath.c_str());
  deserializeSensorsFromDevice(http.GET());

  describe("Test GET actuators from deviceID");
  serverPath = serverName + "api/devices/" + String(DEVICE_ID) + "/actuators/Buzzer";
  http.begin(serverPath.c_str());
  deserializeActuatorsFromDevice(http.GET());
}

void POST_tests()
{
  sensors_event_t event;
  dht.temperature().getEvent(&event);
  float temperatura = event.temperature;

  String sensor_value_body = serializeSensorValueBody(temperatura,72,millis());
  describe("Test POST with sensor value");
  String serverPath = serverName + "api/sensor_values";
  http.begin(serverPath.c_str());
  test_response(http.POST(sensor_value_body));


  String actuator_states_body = serializeActuatorStatusBody(actu,false,1,millis());
  describe("Test POST with actuator state");
  serverPath = serverName + "api/actuator_states";
  http.begin(serverPath.c_str());
  test_response(http.POST(actuator_states_body));


  //String device_body = serializeDeviceBody(String(DEVICE_ID), ("Name_" + String(DEVICE_ID)).c_str(), ("mqtt_" + String(DEVICE_ID)).c_str(), 12);
  //describe("Test POST with path and body and response");
  //serverPath = serverName + "api/device";
  //http.begin(serverPath.c_str());
  //test_response(http.POST(actuator_states_body));
}

// conecta o reconecta al MQTT
// consigue conectar -> suscribe a topic y publica un mensaje
// no -> espera 5 segundos
void ConnectMqtt()
{
  Serial.print("Starting MQTT connection...");
  if (client.connect(MQTT_CLIENT_NAME))
  {
    client.subscribe("mqttChannelGroup2");                                    
    client.publish("dad", "connected");
  }
  else
  {
    Serial.print("Failed MQTT connection, rc=");
    Serial.print(client.state());
    Serial.println(" try again in 5 seconds");

    delay(500);
  }
}

// gestiona la comunicación MQTT
// comprueba que el cliente está conectado
// no -> intenta reconectar
// si -> llama al MQTT loop
void HandleMqtt()
{
  if (!client.connected())
  {
    ConnectMqtt();
  }
  client.loop();
}



// Variables para controlar el tiempo transcurrido
// Run the tests!
// Run the tests!



int repeticiones;
void loop()
{
if(repeticiones == 10){
  sensors_event_t event;
  dht.temperature().getEvent(&event);
  
  if (isnan(event.temperature)) {
    Serial.println(F("Error leyendo la temperatura!"));
  } else {
    Serial.print(F("Temperatura: "));
    Serial.print(event.temperature);
    Serial.println(F("°C"));
    // Verificar si la temperatura supera el umbral predefinido
    if (mensaje == "1") {
      // Activar el buzzer
      digitalWrite(BUZZER_PIN, HIGH);
      actu=1;
      delay(1000);  // Duración del sonido del buzzer
      digitalWrite(BUZZER_PIN, LOW);
      }else{
      actu=0;
      digitalWrite(BUZZER_PIN,LOW);  
      }
  }
  //GET_tests();
    POST_tests();
    repeticiones = 0;
}else{
    repeticiones++;
}
timeClient.update();
HandleMqtt();
 }

  
