package es.us.dad.mysql.rest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class HighLevelAPIVerticle extends AbstractVerticle {

    private MqttClient mqttClient;
    private static final String MQTT_TOPIC_PREFIX = "dad/";
    private static final String EVENT_BUS_ADDRESS = "highlevel.api";

    @Override
    public void start(Promise<Void> startPromise) {
        // Configurar MQTT
        setupMqttClient();
        
        // Configurar el enrutador HTTP
        Router router = Router.router(vertx);
        
        // Configuración básica
        router.route().handler(BodyHandler.create());
        router.route().handler(this::setCommonHeaders);
        
        // ========= ENDPOINTS PARA DISPOSITIVOS ESP ==========
        router.post("/api/esp/devices").handler(this::registerESPDevice);
        router.get("/api/esp/devices/:deviceId").handler(this::getESPDeviceStatus);
        router.put("/api/esp/devices/:deviceId/config").handler(this::updateESPDeviceConfig);
        
        // ========= ENDPOINTS PARA COMANDOS MQTT ==========
        router.post("/api/mqtt/publish").handler(this::publishMqttCommand);
        router.get("/api/mqtt/subscribe/:topic").handler(this::subscribeMqttTopic);
        
        // Iniciar servidor HTTP
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8081, http -> {
                if (http.succeeded()) {
                    System.out.println("High Level API escuchando en puerto 8081");
                    startPromise.complete();
                } else {
                    startPromise.fail(http.cause());
                }
            });
    }

    // ========== CONFIGURACIÓN MQTT ==========
    private void setupMqttClient() {
        MqttClientOptions options = new MqttClientOptions()
            .setAutoKeepAlive(true)
            .setCleanSession(true);
        
        mqttClient = MqttClient.create(vertx, options);
        
        mqttClient.connect(1883, "broker.mqtt-dashboard.com", ar -> {
            if (ar.succeeded()) {
                System.out.println("Conectado al broker MQTT");
                setupMqttSubscriptions();
            } else {
                System.out.println("Error conectando a MQTT: " + ar.cause().getMessage());
            }
        });
    }
    
    private void setupMqttSubscriptions() {
        // Suscribirse a temas generales
        mqttClient.subscribe(MQTT_TOPIC_PREFIX + "+/status", 1, res -> {
            if (res.succeeded()) {
                System.out.println("Suscrito a temas de estado");
            }
        });
        
        mqttClient.publishHandler(message -> {
            String topic = message.topicName();
            String payload = message.payload().toString();
            
            // Procesar mensajes MQTT entrantes
            processIncomingMqttMessage(topic, payload);
        });
    }
    
    private void processIncomingMqttMessage(String topic, String payload) {
        // Aquí se procesan los mensajes MQTT entrantes
        System.out.println("Mensaje MQTT recibido - Tópico: " + topic + ", Payload: " + payload);
        
        // Ejemplo: Actualizar estado de dispositivo en la base de datos
        JsonObject message = new JsonObject(payload);
        vertx.eventBus().send(EVENT_BUS_ADDRESS + ".updateStatus", message);
    }

    // ========== MANEJADORES HTTP ==========
    private void registerESPDevice(RoutingContext ctx) {
        JsonObject deviceData = ctx.getBodyAsJson();
        
        // Validar datos básicos
        if (deviceData == null || !deviceData.containsKey("deviceId")) {
            ctx.fail(400, new IllegalArgumentException("Datos de dispositivo inválidos"));
            return;
        }
        
        // Publicar comando MQTT para configuración inicial
        String deviceId = deviceData.getString("deviceId");
        String configTopic = MQTT_TOPIC_PREFIX + deviceId + "/config";
        
        JsonObject configMessage = new JsonObject()
            .put("command", "initialize")
            .put("interval", deviceData.getInteger("interval", 5000));
        
        mqttClient.publish(configTopic, 
            configMessage.toBuffer(), 
            MqttClientOptions.DEFAULT_QOS, 
            false, 
            ar -> {
                if (ar.succeeded()) {
                    // Registrar dispositivo en la base de datos
                    vertx.eventBus().request("lowlevel.api.devices", 
                        deviceData, 
                        reply -> {
                            if (reply.succeeded()) {
                                ctx.response()
                                    .setStatusCode(201)
                                    .end(reply.result().body().toString());
                            } else {
                                ctx.fail(500, reply.cause());
                            }
                        });
                } else {
                    ctx.fail(500, ar.cause());
                }
            });
    }
    
    private void getESPDeviceStatus(RoutingContext ctx) {
        String deviceId = ctx.request().getParam("deviceId");
        
        // 1. Obtener información básica del dispositivo de la API de bajo nivel
        vertx.eventBus().request("lowlevel.api.devices." + deviceId, 
            new JsonObject(), 
            reply -> {
                if (reply.succeeded()) {
                    JsonObject deviceInfo = new JsonObject(reply.result().body().toString());
                    
                    // 2. Obtener último estado del dispositivo via MQTT
                    String statusTopic = MQTT_TOPIC_PREFIX + deviceId + "/status";
                    
                    // En una implementación real, aquí podríamos consultar un caché o base de datos
                    // que almacena los últimos estados MQTT recibidos
                    
                    JsonObject response = new JsonObject()
                        .put("deviceInfo", deviceInfo)
                        .put("lastStatus", "offline"); // Valor por defecto
                        
                    ctx.response()
                        .setStatusCode(200)
                        .end(response.encode());
                } else {
                    ctx.fail(404, reply.cause());
                }
            });
    }
    
    private void updateESPDeviceConfig(RoutingContext ctx) {
        String deviceId = ctx.request().getParam("deviceId");
        JsonObject config = ctx.getBodyAsJson();
        
        String configTopic = MQTT_TOPIC_PREFIX + deviceId + "/config";
        
        mqttClient.publish(configTopic, 
            config.toBuffer(), 
            MqttClientOptions.DEFAULT_QOS, 
            false, 
            ar -> {
                if (ar.succeeded()) {
                    ctx.response()
                        .setStatusCode(200)
                        .end(new JsonObject().put("status", "ok").encode());
                } else {
                    ctx.fail(500, ar.cause());
                }
            });
    }
    
    private void publishMqttCommand(RoutingContext ctx) {
        JsonObject command = ctx.getBodyAsJson();
        
        if (command == null || !command.containsKey("topic") || !command.containsKey("message")) {
            ctx.fail(400, new IllegalArgumentException("Datos de comando inválidos"));
            return;
        }
        
        String topic = MQTT_TOPIC_PREFIX + command.getString("topic");
        String message = command.getString("message");
        
        mqttClient.publish(topic, 
            message.getBytes(), 
            MqttClientOptions.DEFAULT_QOS, 
            false, 
            ar -> {
                if (ar.succeeded()) {
                    ctx.response()
                        .setStatusCode(200)
                        .end(new JsonObject().put("status", "published").encode());
                } else {
                    ctx.fail(500, ar.cause());
                }
            });
    }
    
    private void subscribeMqttTopic(RoutingContext ctx) {
        String topic = MQTT_TOPIC_PREFIX + ctx.request().getParam("topic");
        
        mqttClient.subscribe(topic, 1, res -> {
            if (res.succeeded()) {
                ctx.response()
                    .setStatusCode(200)
                    .end(new JsonObject().put("status", "subscribed").encode());
            } else {
                ctx.fail(500, res.cause());
            }
        });
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    private void setCommonHeaders(RoutingContext ctx) {
        ctx.response()
            .putHeader("Content-Type", "application/json")
            .putHeader("Charset", "UTF-8");
        ctx.next();
    }
}