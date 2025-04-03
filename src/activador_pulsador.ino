#include <ESP8266WiFi.h>
#include <PubSubClient.h>

const char* ssid = "oPPo";
const char* password = "DAD_G10_TieneNota10";
const char* mqtt_server = "192.168.69.14";  // IP del broker MQTT

// Definición del pin del pulsador:
// Usamos GPIO0 (comúnmente etiquetado como D3 en NodeMCU).
// IMPORTANTE: Asegúrate de no presionar el botón al reiniciar la placa, ya que GPIO0 se usa para el modo de programación.
#define PULSADOR_PIN 14  // Ejemplo usando GPIO14 (D5)
 

WiFiClient espClient;
PubSubClient client(espClient);

bool estado_anterior = HIGH;  // El pin arranca en HIGH gracias al INPUT_PULLUP
unsigned long ultimo_tiempo = 0;

void setup() {
  pinMode(PULSADOR_PIN, INPUT_PULLUP);
  Serial.begin(9600);
  setup_wifi();
  client.setServer(mqtt_server, 1883);
}

void setup_wifi() {
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
  }
  Serial.println("WiFi conectado");
}

void reconnect() {
  while (!client.connected()) {
    // Se pasan las credenciales: usuario y contraseña "admin"
    if (client.connect("PulsadorClient", "admin", "admin")) {
      Serial.println("Conectado al broker MQTT");
    } else {
      delay(5000);
    }
  }
}

void loop() {
  if (!client.connected()) {
    reconnect();
  }
  client.loop();

  // Lee el estado actual del pulsador
  bool estado_actual = digitalRead(PULSADOR_PIN);
  
  
  // Si hay un cambio de estado
  if (estado_actual != estado_anterior) {
    // Si detectamos que el botón se ha presionado (paso de HIGH a LOW)
    if (estado_actual == LOW) {
     
      client.publish("Grupo/Placa/SensorActuador", "Señal");
      Serial.println("Pulsación detectada");
    }
    // Actualiza el estado anterior y espera un poco para debouncing
    estado_anterior = estado_actual;
    delay(50);  // Retardo para evitar rebotes
  }
}
