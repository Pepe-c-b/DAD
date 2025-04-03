#include <ESP8266WiFi.h>
#include <PubSubClient.h>

const char* ssid = "oPPo";
const char* password = "DAD_G10_TieneNota10";
const char* mqtt_server = " 192.168.69.14";  // IP del broker MQTT

#define LED_PIN BUILTIN_LED  // LED integrado (normalmente D4 en NodeMCU)

WiFiClient espClient;
PubSubClient client(espClient);
bool estado_led = HIGH;  // Estado inicial (HIGH = apagado, debido a lógica inversa del LED integrado)

void setup() {
  pinMode(LED_PIN, OUTPUT);
  digitalWrite(LED_PIN, estado_led);
  Serial.begin(9600);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
  client.setCallback(callback);
}

void setup_wifi() {
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) delay(500);
  Serial.println("WiFi conectado");
}

void callback(char* topic, byte* payload, unsigned int length) {
  String mensaje;
  for (int i = 0; i < length; i++) {
    mensaje += (char)payload[i];
  }

  if (String(topic) == "Grupo/Placa/SensorActuador") {
    if (mensaje == "Señal") {
      estado_led = !estado_led;
      digitalWrite(LED_PIN, estado_led);
      Serial.println("Estado LED: " + String(estado_led ? "ENCENDIDO" : "APAGADO"));
    }
  }
}

void reconnect() {
  while (!client.connected()) {
    // Se pasan las credenciales: usuario y contraseña "admin"
    if (client.connect("LEDClient", "admin", "admin")) {
      client.subscribe("Grupo/Placa/SensorActuador");
      Serial.println("Suscrito a Grupo/Placa/SensorActuador");
    } else {
      delay(5000);
    }
  }
}

void loop() {
  if (!client.connected()) reconnect();
  client.loop();
}
