package es.us.dad.mysql.rest;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import es.us.dad.mysql.entities.Actuator;
import es.us.dad.mysql.entities.ActuatorStatus;
import es.us.dad.mysql.entities.ActuatorType;
import es.us.dad.mysql.entities.Device;
import es.us.dad.mysql.entities.Group;
import es.us.dad.mysql.entities.Sensor;
import es.us.dad.mysql.entities.SensorType;
import es.us.dad.mysql.entities.SensorValue;
import es.us.dad.mysql.messages.DatabaseEntity;
import es.us.dad.mysql.messages.DatabaseMessage;
import es.us.dad.mysql.messages.DatabaseMessageIdAndActuatorType;
import es.us.dad.mysql.messages.DatabaseMessageIdAndSensorType;
import es.us.dad.mysql.messages.DatabaseMessageLatestValues;
import es.us.dad.mysql.messages.DatabaseMessageType;
import es.us.dad.mysql.messages.DatabaseMethod;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class RestAPIVerticle extends AbstractVerticle {
    
    // Constantes para los headers HTTP
    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final String CHARSET_UTF8 = "charset=utf-8";
    
    // Mapa para almacenar grupos en memoria
    //private Map<Integer, Group> groups = new HashMap<Integer, Group>();
    
    // Objeto Gson para serialización/deserialización JSON
    private Gson gson;

    @Override
    public void start(Promise<Void> startFuture) {
    	
        // Crear datos de prueba
        //createSomeData(25);
        
        // Inicializar Gson
        gson = new GsonBuilder().create();

        // Crear el enrutador
        Router router = Router.router(vertx);

        // Configurar el servidor HTTP
        vertx.createHttpServer().requestHandler(router::handle).listen(8080, result -> {
            if (result.succeeded()) {
                System.out.println("API Rest escuchando en puerto 8080");
                startFuture.complete();
            } else {
                startFuture.fail(result.cause());
            }
        });

        // ========== HANDLERS GLOBALES ==========
        
        // Handler para procesar el cuerpo de las peticiones
        router.route("/api*").handler(BodyHandler.create());
        
        // Handler para establecer headers comunes
        router.route("/api*").handler(this::setCommonHeaders);
        
        // Handler para manejo centralizado de errores
        router.route("/api*").failureHandler(this::handleFailure);

        // ========== ENDPOINTS PARA GRUPOS ==========
        
        // Obtener todos los grupos
        router.get("/api/groups").handler(this::getAllGroups);
        
        // Obtener un grupo por ID
        router.get("/api/groups/:groupid").handler(this::getGroupById);
        
        // Crear nuevo grupo
        router.post("/api/groups").handler(this::addGroup);
        
        // Eliminar grupo
        router.delete("/api/groups/:groupid").handler(this::deleteGroup);
        
        // Actualizar grupo
        router.put("/api/groups/:groupid").handler(this::putGroup);
        
        // Obtener dispositivos de un grupo
        router.get("/api/groups/:groupid/devices").handler(this::getDevicesFromGroup);
        
        // Añadir dispositivo a grupo
        router.put("/api/groups/:groupid/devices/:deviceid").handler(this::addDeviceToGroup);

        // ========== ENDPOINTS PARA DISPOSITIVOS ==========
        
        // Obtener todos los dispositivos
        router.get("/api/devices").handler(this::getAllDevices);
        
        // Obtener dispositivo por ID
        router.get("/api/devices/:deviceid").handler(this::getDeviceById);
        
        // Crear nuevo dispositivo
        router.post("/api/devices").handler(this::addDevice);
        
        // Crear múltiples dispositivos (batch)
        router.post("/api/devices/batch").handler(this::addDevicesBatch);
        
        // Eliminar dispositivo
        router.delete("/api/devices/:deviceid").handler(this::deleteDevice);
        
        // Actualizar dispositivo
        router.put("/api/devices/:deviceid").handler(this::putDevice);
        
        // Obtener sensores de un dispositivo
        router.get("/api/devices/:deviceid/sensors").handler(this::getSensorsFromDevice);
        
        // Obtener actuadores de un dispositivo
        router.get("/api/devices/:deviceid/actuators").handler(this::getActuatorsFromDevice);
        
        // Obtener sensores de un dispositivo por tipo
        router.get("/api/devices/:deviceid/sensors/:type").handler(this::getSensorsFromDeviceAndType);
        
        // Obtener actuadores de un dispositivo por tipo
        router.get("/api/devices/:deviceid/actuators/:type").handler(this::getActuatorsFromDeviceAndType);

        // ========== ENDPOINTS PARA SENSORES ==========
        
        // Obtener todos los sensores
        router.get("/api/sensors").handler(this::getAllSensors);
        
        // Obtener sensor por ID
        router.get("/api/sensors/:sensorid").handler(this::getSensorById);
        
        // Crear nuevo sensor
        router.post("/api/sensors").handler(this::addSensor);
        
        // Crear múltiples sensores (batch)
        router.post("/api/sensors/batch").handler(this::addSensorsBatch);
        
        // Eliminar sensor
        router.delete("/api/sensors/:sensorid").handler(this::deleteSensor);
        
        // Actualizar sensor
        router.put("/api/sensors/:sensorid").handler(this::putSensor);

        // ========== ENDPOINTS PARA ACTUADORES ==========
        
        // Obtener todos los actuadores
        router.get("/api/actuators").handler(this::getAllActuators);
        
        // Obtener actuador por ID
        router.get("/api/actuators/:actuatorid").handler(this::getActuatorById);
        
        // Crear nuevo actuador
        router.post("/api/actuators").handler(this::addActuator);
        
        // Crear múltiples actuadores (batch)
        router.post("/api/actuators/batch").handler(this::addActuatorsBatch);
        
        // Eliminar actuador
        router.delete("/api/actuators/:actuatorid").handler(this::deleteActuator);
        
        // Actualizar actuador
        router.put("/api/actuators/:actuatorid").handler(this::putActuator);

        // ========== ENDPOINTS PARA VALORES DE SENSORES ==========
        
        // Obtener todos los valores de sensores
        router.get("/api/sensorValues").handler(this::getAllSensorValues);
        
        // Registrar nuevo valor de sensor
        router.post("/api/sensorValues").handler(this::addSensorValue);
        
        // Registrar múltiples valores (batch)
        router.post("/api/sensorValues/batch").handler(this::addSensorValuesBatch);
        
        // Eliminar valor de sensor
        router.delete("/api/sensorValues/:sensorvalueid").handler(this::deleteSensorValue);
        
        // Obtener último valor de un sensor
        router.get("/api/sensorValues/:sensorid/latest").handler(this::getLastSensorValue);
        
        // Obtener últimos N valores de un sensor
        router.get("/api/sensorValues/:sensorid/latest/:limit").handler(this::getLatestSensorValue);
        
        // Obtener último valor de cada sensor del grupo
        router.get("/api/group/:groupid/sensorValues/latest").handler(this::getLastSensorValueGroup);

        // ========== ENDPOINTS PARA ESTADOS DE ACTUADORES ==========
        
        // Obtener todos los estados de actuadores
        router.get("/api/actuator_states").handler(this::getAllActuatorStates);
        
        // Registrar nuevo estado de actuador
        router.post("/api/actuator_states").handler(this::addActuatorStatus);
        
        // Registrar múltiples estados (batch)
        router.post("/api/actuator_states/batch").handler(this::addActuatorStatesBatch);
        
        // Eliminar estado de actuador
        router.delete("/api/actuator_states/:actuatorstatusid").handler(this::deleteActuatorStatus);
        
        // Obtener último estado de un actuador
        router.get("/api/actuator_states/:actuatorid/latest").handler(this::getLastActuatorStatus);
        
        // Obtener últimos N estados de un actuador
        router.get("/api/actuator_states/:actuatorid/latest/:limit").handler(this::getLatestActuatorStatus);
        
        // Obtener último estado de cada actuador del grupo
        router.get("/api/group/:groupid/actuatorStates/latest").handler(this::getLastActuatorStatusGroup);
    }

    // ========== MANEJADORES COMUNES ==========

    /**
     * Establece headers comunes para todas las respuestas
     */
    private void setCommonHeaders(RoutingContext ctx) {
        ctx.response()
           .putHeader("Content-Type", CONTENT_TYPE_JSON)
           .putHeader("Charset", CHARSET_UTF8);
        ctx.next();
    }

    /**
     * Maneja errores de forma centralizada
     */
    private void handleFailure(RoutingContext ctx) {
        ctx.response()
           .setStatusCode(ctx.statusCode())
           .end(new JsonObject()
               .put("error", ctx.failure().getMessage())
               .encode());
    }

    /**
     * Deserializa un mensaje de la base de datos
     */
    private DatabaseMessage deserializeDatabaseMessageFromMessageHandler(AsyncResult<Message<Object>> handler) {
        return gson.fromJson(handler.result().body().toString(), DatabaseMessage.class);
    }

    // ========== MÉTODOS UTILITARIOS ==========

    /**
     * Envía un mensaje a la base de datos y maneja la respuesta
     */
    private <T> void sendDatabaseMessage(RoutingContext ctx, DatabaseMessage message, String address, 
                                       Class<T> responseType, int successStatusCode) {
        vertx.eventBus().request(address, gson.toJson(message), handler -> {
            if (handler.succeeded()) {
                DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
                ctx.response()
                   .setStatusCode(successStatusCode)
                   .end(gson.toJson(responseMessage.getResponseBodyAs(responseType)));
            } else {
                ctx.fail(500, handler.cause());
            }
        });
    }

    /**
     * Versión para listas del método sendDatabaseMessage
     */
    private <T> void sendDatabaseMessageForList(RoutingContext ctx, DatabaseMessage message, String address, 
                                             Class<T[]> responseType, int successStatusCode) {
        vertx.eventBus().request(address, gson.toJson(message), handler -> {
            if (handler.succeeded()) {
                DatabaseMessage responseMessage = deserializeDatabaseMessageFromMessageHandler(handler);
                ctx.response()
                   .setStatusCode(successStatusCode)
                   .end(gson.toJson(responseMessage.getResponseBodyAs(responseType)));
            } else {
                ctx.fail(500, handler.cause());
            }
        });
    }

    // ========== MANEJADORES PARA GRUPOS ==========

    /**
     * Obtiene todos los grupos
     */
    private void getAllGroups(RoutingContext ctx) {
        DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Group, 
                                                    DatabaseMethod.GetGroup, "");
        sendDatabaseMessageForList(ctx, message, RestEntityMessage.Group.getAddress(), Group[].class, 200);
    }

    /**
     * Obtiene un grupo por su ID
     */
    private void getGroupById(RoutingContext ctx) {
        try {
            int groupId = Integer.parseInt(ctx.request().getParam("groupid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Group,
                    DatabaseMethod.GetGroup, groupId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Group.getAddress(), Group.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Crea un nuevo grupo
     */
    private void addGroup(RoutingContext ctx) {
        try {
            Group group = gson.fromJson(ctx.getBodyAsString(), Group.class);
            if (group == null || group.getMqttChannel() == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de grupo inválidos"));
                return;
            }
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Group,
                    DatabaseMethod.CreateGroup, group);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Group.getAddress(), Group.class, 201);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Elimina un grupo
     */
    private void deleteGroup(RoutingContext ctx) {
        try {
            int groupId = Integer.parseInt(ctx.request().getParam("groupid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.Group,
                    DatabaseMethod.DeleteGroup, groupId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Group.getAddress(), Group.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Actualiza un grupo existente
     */
    private void putGroup(RoutingContext ctx) {
        try {
            int groupId = Integer.parseInt(ctx.request().getParam("groupid"));
            Group group = gson.fromJson(ctx.getBodyAsString(), Group.class);
            if (group == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de grupo inválidos"));
                return;
            }
            group.setIdGroup(groupId);
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Group,
                    DatabaseMethod.EditGroup, group);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Group.getAddress(), Group.class, 200);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene los dispositivos de un grupo
     */
    private void getDevicesFromGroup(RoutingContext ctx) {
        try {
            int groupId = Integer.parseInt(ctx.request().getParam("groupid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Group,
                    DatabaseMethod.GetDevicesFromGroupId, groupId);
            sendDatabaseMessageForList(ctx, message, RestEntityMessage.Group.getAddress(), Device[].class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Añade un dispositivo a un grupo
     */
    private void addDeviceToGroup(RoutingContext ctx) {
        try {
            int groupId = Integer.parseInt(ctx.request().getParam("groupid"));
            int deviceId = Integer.parseInt(ctx.request().getParam("deviceid"));
            
            Device device = new Device();
            device.setIdGroup(groupId);
            device.setIdDevice(deviceId);
            
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Group,
                    DatabaseMethod.AddDeviceToGroup, device);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Group.getAddress(), Device.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    // ========== MANEJADORES PARA DISPOSITIVOS ==========

    /**
     * Obtiene todos los dispositivos
     */
    private void getAllDevices(RoutingContext ctx) {
        DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Device, 
                                                    DatabaseMethod.GetDevice, "");
        sendDatabaseMessageForList(ctx, message, RestEntityMessage.Device.getAddress(), Device[].class, 200);
    }

    /**
     * Obtiene un dispositivo por su ID
     */
    private void getDeviceById(RoutingContext ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.request().getParam("deviceid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Device,
                    DatabaseMethod.GetDevice, deviceId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Device.getAddress(), Device.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Crea un nuevo dispositivo
     */
    private void addDevice(RoutingContext ctx) {
        try {
            Device device = gson.fromJson(ctx.getBodyAsString(), Device.class);
            if (device == null || device.getMqttChannel() == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de dispositivo inválidos"));
                return;
            }
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Device,
                    DatabaseMethod.CreateDevice, device);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Device.getAddress(), Device.class, 201);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Crea múltiples dispositivos (batch)
     */
    private void addDevicesBatch(RoutingContext ctx) {
        try {
            Device[] devices = gson.fromJson(ctx.getBodyAsString(), Device[].class);
            List<DatabaseMessage> messages = java.util.Arrays.stream(devices)
                .map(device -> new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Device, 
                                                 DatabaseMethod.CreateDevice, device))
                .collect(Collectors.toList());
            
            processBatchMessages(ctx, messages, RestEntityMessage.Device.getAddress(), Device[].class);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Elimina un dispositivo
     */
    private void deleteDevice(RoutingContext ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.request().getParam("deviceid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.Device,
                    DatabaseMethod.DeleteDevice, deviceId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Device.getAddress(), Device.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Actualiza un dispositivo existente
     */
    private void putDevice(RoutingContext ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.request().getParam("deviceid"));
            Device device = gson.fromJson(ctx.getBodyAsString(), Device.class);
            if (device == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de dispositivo inválidos"));
                return;
            }
            device.setIdDevice(deviceId);
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Device,
                    DatabaseMethod.EditDevice, device);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Device.getAddress(), Device.class, 200);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene los sensores de un dispositivo
     */
    private void getSensorsFromDevice(RoutingContext ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.request().getParam("deviceid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Device,
                    DatabaseMethod.GetSensorsFromDeviceId, deviceId);
            sendDatabaseMessageForList(ctx, message, RestEntityMessage.Device.getAddress(), Sensor[].class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene los actuadores de un dispositivo
     */
    private void getActuatorsFromDevice(RoutingContext ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.request().getParam("deviceid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Device,
                    DatabaseMethod.GetActuatorsFromDeviceId, deviceId);
            sendDatabaseMessageForList(ctx, message, RestEntityMessage.Device.getAddress(), Actuator[].class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene sensores de un dispositivo filtrados por tipo
     */
    private void getSensorsFromDeviceAndType(RoutingContext ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.request().getParam("deviceid"));
            SensorType type = SensorType.valueOf(ctx.request().getParam("type"));
            
            DatabaseMessageIdAndSensorType query = new DatabaseMessageIdAndSensorType(deviceId, type);
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Device,
                    DatabaseMethod.GetSensorsFromDeviceIdAndSensorType, query);
            sendDatabaseMessageForList(ctx, message, RestEntityMessage.Device.getAddress(), Sensor[].class, 200);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene actuadores de un dispositivo filtrados por tipo
     */
    private void getActuatorsFromDeviceAndType(RoutingContext ctx) {
        try {
            int deviceId = Integer.parseInt(ctx.request().getParam("deviceid"));
            ActuatorType type = ActuatorType.valueOf(ctx.request().getParam("type"));
            
            DatabaseMessageIdAndActuatorType query = new DatabaseMessageIdAndActuatorType(deviceId, type);
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Device,
                    DatabaseMethod.GetActuatorsFromDeviceIdAndActuatorType, query);
            sendDatabaseMessageForList(ctx, message, RestEntityMessage.Device.getAddress(), Actuator[].class, 200);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    // ========== MANEJADORES PARA SENSORES ==========

    /**
     * Obtiene todos los sensores
     */
    private void getAllSensors(RoutingContext ctx) {
        DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Sensor, 
                                                    DatabaseMethod.GetSensor, "");
        sendDatabaseMessageForList(ctx, message, RestEntityMessage.Sensor.getAddress(), Sensor[].class, 200);
    }

    /**
     * Obtiene un sensor por su ID
     */
    private void getSensorById(RoutingContext ctx) {
        try {
            int sensorId = Integer.parseInt(ctx.request().getParam("sensorid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Sensor,
                    DatabaseMethod.GetSensor, sensorId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Sensor.getAddress(), Sensor.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Crea un nuevo sensor
     */
    private void addSensor(RoutingContext ctx) {
        try {
            Sensor sensor = gson.fromJson(ctx.getBodyAsString(), Sensor.class);
            if (sensor == null || sensor.getIdDevice() == null || sensor.getSensorType() == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de sensor inválidos"));
                return;
            }
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Sensor,
                    DatabaseMethod.CreateSensor, sensor);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Sensor.getAddress(), Sensor.class, 201);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Crea múltiples sensores (batch)
     */
    private void addSensorsBatch(RoutingContext ctx) {
        try {
            Sensor[] sensors = gson.fromJson(ctx.getBodyAsString(), Sensor[].class);
            List<DatabaseMessage> messages = java.util.Arrays.stream(sensors)
                .map(sensor -> new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Sensor, 
                                                 DatabaseMethod.CreateSensor, sensor))
                .collect(Collectors.toList());
            
            processBatchMessages(ctx, messages, RestEntityMessage.Sensor.getAddress(), Sensor[].class);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Elimina un sensor
     */
    private void deleteSensor(RoutingContext ctx) {
        try {
            int sensorId = Integer.parseInt(ctx.request().getParam("sensorid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.Sensor,
                    DatabaseMethod.DeleteSensor, sensorId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Sensor.getAddress(), Sensor.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Actualiza un sensor existente
     */
    private void putSensor(RoutingContext ctx) {
        try {
            int sensorId = Integer.parseInt(ctx.request().getParam("sensorid"));
            Sensor sensor = gson.fromJson(ctx.getBodyAsString(), Sensor.class);
            if (sensor == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de sensor inválidos"));
                return;
            }
            sensor.setIdSensor(sensorId);
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Sensor,
                    DatabaseMethod.EditSensor, sensor);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Sensor.getAddress(), Sensor.class, 200);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    // ========== MANEJADORES PARA ACTUADORES ==========

    /**
     * Obtiene todos los actuadores
     */
    private void getAllActuators(RoutingContext ctx) {
        DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Actuator, 
                                                    DatabaseMethod.GetActuator, "");
        sendDatabaseMessageForList(ctx, message, RestEntityMessage.Actuator.getAddress(), Actuator[].class, 200);
    }

    /**
     * Obtiene un actuador por su ID
     */
    private void getActuatorById(RoutingContext ctx) {
        try {
            int actuatorId = Integer.parseInt(ctx.request().getParam("actuatorid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Actuator,
                    DatabaseMethod.GetActuator, actuatorId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Actuator.getAddress(), Actuator.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Crea un nuevo actuador
     */
    private void addActuator(RoutingContext ctx) {
        try {
            Actuator actuator = gson.fromJson(ctx.getBodyAsString(), Actuator.class);
            if (actuator == null || actuator.getIdDevice() == null || actuator.getActuatorType() == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de actuador inválidos"));
                return;
            }
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Actuator,
                    DatabaseMethod.CreateActuator, actuator);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Actuator.getAddress(), Actuator.class, 201);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Crea múltiples actuadores (batch)
     */
    private void addActuatorsBatch(RoutingContext ctx) {
        try {
            Actuator[] actuators = gson.fromJson(ctx.getBodyAsString(), Actuator[].class);
            List<DatabaseMessage> messages = java.util.Arrays.stream(actuators)
                .map(actuator -> new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.Actuator, 
                                                   DatabaseMethod.CreateActuator, actuator))
                .collect(Collectors.toList());
            
            processBatchMessages(ctx, messages, RestEntityMessage.Actuator.getAddress(), Actuator[].class);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Elimina un actuador
     */
    private void deleteActuator(RoutingContext ctx) {
        try {
            int actuatorId = Integer.parseInt(ctx.request().getParam("actuatorid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.Actuator,
                    DatabaseMethod.DeleteActuator, actuatorId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Actuator.getAddress(), Actuator.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Actualiza un actuador existente
     */
    private void putActuator(RoutingContext ctx) {
        try {
            int actuatorId = Integer.parseInt(ctx.request().getParam("actuatorid"));
            Actuator actuator = gson.fromJson(ctx.getBodyAsString(), Actuator.class);
            if (actuator == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de actuador inválidos"));
                return;
            }
            actuator.setIdActuator(actuatorId);
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Actuator,
                    DatabaseMethod.EditActuator, actuator);
            sendDatabaseMessage(ctx, message, RestEntityMessage.Actuator.getAddress(), Actuator.class, 200);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    // ========== MANEJADORES PARA VALORES DE SENSORES ==========

    /**
     * Obtiene todos los valores de sensores
     */
    private void getAllSensorValues(RoutingContext ctx) {
        DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.SensorValue, 
                                                    DatabaseMethod.GetSensor, "");
        sendDatabaseMessageForList(ctx, message, RestEntityMessage.SensorValue.getAddress(), SensorValue[].class, 200);
    }

    /**
     * Registra un nuevo valor de sensor
     */
    private void addSensorValue(RoutingContext ctx) {
        try {
            SensorValue sensorValue = gson.fromJson(ctx.getBodyAsString(), SensorValue.class);
            if (sensorValue == null || sensorValue.getIdSensor() == null || sensorValue.getValue() == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de valor de sensor inválidos"));
                return;
            }
            if (sensorValue.getTimestamp() == null) {
                sensorValue.setTimestamp(Calendar.getInstance().getTimeInMillis());
            }
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.SensorValue,
                    DatabaseMethod.CreateSensorValue, sensorValue);
            sendDatabaseMessage(ctx, message, RestEntityMessage.SensorValue.getAddress(), SensorValue.class, 201);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Registra múltiples valores de sensores (batch)
     */
    private void addSensorValuesBatch(RoutingContext ctx) {
        try {
            SensorValue[] sensorValues = gson.fromJson(ctx.getBodyAsString(), SensorValue[].class);
            List<DatabaseMessage> messages = java.util.Arrays.stream(sensorValues)
                .map(sensorValue -> new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.SensorValue, 
                                                      DatabaseMethod.CreateSensorValue, sensorValue))
                .collect(Collectors.toList());
            
            processBatchMessages(ctx, messages, RestEntityMessage.SensorValue.getAddress(), SensorValue[].class);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Elimina un valor de sensor
     */
    private void deleteSensorValue(RoutingContext ctx) {
        try {
            int sensorValueId = Integer.parseInt(ctx.request().getParam("sensorvalueid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.SensorValue,
                    DatabaseMethod.DeleteSensorValue, sensorValueId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.SensorValue.getAddress(), SensorValue.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene el último valor de un sensor
     */
    private void getLastSensorValue(RoutingContext ctx) {
        try {
            int sensorId = Integer.parseInt(ctx.request().getParam("sensorid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.SensorValue,
                    DatabaseMethod.GetLastSensorValueFromSensorId, sensorId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.SensorValue.getAddress(), SensorValue.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene los últimos N valores de un sensor
     */
    private void getLatestSensorValue(RoutingContext ctx) {
        try {
            int sensorId = Integer.parseInt(ctx.request().getParam("sensorid"));
            int limit = Integer.parseInt(ctx.request().getParam("limit"));
            
            DatabaseMessageLatestValues query = new DatabaseMessageLatestValues(sensorId, limit);
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.SensorValue,
                    DatabaseMethod.GetLatestSensorValuesFromSensorId, query);
            sendDatabaseMessageForList(ctx, message, RestEntityMessage.SensorValue.getAddress(), SensorValue[].class, 200);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }
    
    /**
     * Obtiene el último valor de cada sensor en un grupo
     */
    private void getLastSensorValueGroup(RoutingContext ctx) {
        try {
            int groupId = Integer.parseInt(ctx.request().getParam("groupid"));
            DatabaseMessage message = new DatabaseMessage(
                    DatabaseMessageType.SELECT,
                    DatabaseEntity.SensorValue,
                    DatabaseMethod.GetLastSensorValuesFromGroupId,
                    groupId
            );
            sendDatabaseMessage(ctx, message, RestEntityMessage.SensorValue.getAddress(), List.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    // ========== MANEJADORES PARA ESTADOS DE ACTUADORES ==========

    /**
     * Obtiene todos los estados de actuadores
     */
    private void getAllActuatorStates(RoutingContext ctx) {
        DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.ActuatorStatus, 
                                                    DatabaseMethod.GetActuator, "");
        sendDatabaseMessageForList(ctx, message, RestEntityMessage.ActuatorStatus.getAddress(), ActuatorStatus[].class, 200);
    }

    /**
     * Registra un nuevo estado de actuador
     */
    private void addActuatorStatus(RoutingContext ctx) {
        try {
            ActuatorStatus actuatorStatus = gson.fromJson(ctx.getBodyAsString(), ActuatorStatus.class);
            if (actuatorStatus == null || actuatorStatus.getIdActuator() == null || actuatorStatus.getStatus() == null) {
                ctx.fail(400, new IllegalArgumentException("Datos de estado de actuador inválidos"));
                return;
            }
            if (actuatorStatus.getTimestamp() == null) {
                actuatorStatus.setTimestamp(Calendar.getInstance().getTimeInMillis());
            }
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.ActuatorStatus,
                    DatabaseMethod.CreateActuatorStatus, actuatorStatus);
            sendDatabaseMessage(ctx, message, RestEntityMessage.ActuatorStatus.getAddress(), ActuatorStatus.class, 201);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Registra múltiples estados de actuadores (batch)
     */
    private void addActuatorStatesBatch(RoutingContext ctx) {
        try {
            ActuatorStatus[] actuatorStates = gson.fromJson(ctx.getBodyAsString(), ActuatorStatus[].class);
            List<DatabaseMessage> messages = java.util.Arrays.stream(actuatorStates)
                .map(actuatorStatus -> new DatabaseMessage(DatabaseMessageType.INSERT, DatabaseEntity.ActuatorStatus, 
                                                         DatabaseMethod.CreateActuatorStatus, actuatorStatus))
                .collect(Collectors.toList());
            
            processBatchMessages(ctx, messages, RestEntityMessage.ActuatorStatus.getAddress(), ActuatorStatus[].class);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Elimina un estado de actuador
     */
    private void deleteActuatorStatus(RoutingContext ctx) {
        try {
            int actuatorStatusId = Integer.parseInt(ctx.request().getParam("actuatorstatusid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.DELETE, DatabaseEntity.ActuatorStatus,
                    DatabaseMethod.DeleteActuatorStatus, actuatorStatusId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.ActuatorStatus.getAddress(), ActuatorStatus.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene el último estado de un actuador
     */
    private void getLastActuatorStatus(RoutingContext ctx) {
        try {
            int actuatorId = Integer.parseInt(ctx.request().getParam("actuatorid"));
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.ActuatorStatus,
                    DatabaseMethod.GetLastActuatorStatusFromActuatorId, actuatorId);
            sendDatabaseMessage(ctx, message, RestEntityMessage.ActuatorStatus.getAddress(), ActuatorStatus.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    /**
     * Obtiene los últimos N estados de un actuador
     */
    private void getLatestActuatorStatus(RoutingContext ctx) {
        try {
            int actuatorId = Integer.parseInt(ctx.request().getParam("actuatorid"));
            int limit = Integer.parseInt(ctx.request().getParam("limit"));
            
            DatabaseMessageLatestValues query = new DatabaseMessageLatestValues(actuatorId, limit);
            DatabaseMessage message = new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.ActuatorStatus,
                    DatabaseMethod.GetLatestActuatorStatesFromActuatorId, query);
            sendDatabaseMessageForList(ctx, message, RestEntityMessage.ActuatorStatus.getAddress(), ActuatorStatus[].class, 200);
        } catch (Exception e) {
            ctx.fail(400, e);
        }
    }
    
    /**
     * Obtiene el último estado de cada actuador en un grupo
     */
    private void getLastActuatorStatusGroup(RoutingContext ctx) {
        try {
            int groupId = Integer.parseInt(ctx.request().getParam("groupid"));
            DatabaseMessage message = new DatabaseMessage(
                    DatabaseMessageType.SELECT,
                    DatabaseEntity.ActuatorStatus,
                    DatabaseMethod.GetLastActuatorStatesFromGroupId,
                    groupId
            );
            sendDatabaseMessage(ctx, message, RestEntityMessage.ActuatorStatus.getAddress(), List.class, 200);
        } catch (NumberFormatException e) {
            ctx.fail(400, e);
        }
    }

    // ========== PROCESAMIENTO BATCH ==========

    /**
     * Procesa múltiples mensajes en lote
     */
    private <T> void processBatchMessages(RoutingContext ctx, List<DatabaseMessage> messages, 
                                        String address, Class<T[]> responseType) {
        List<Future> futures = messages.stream()
            .map(message -> {
                Promise<T> promise = Promise.promise();
                vertx.eventBus().request(address, gson.toJson(message), reply -> {
                    if (reply.succeeded()) {
                        DatabaseMessage response = deserializeDatabaseMessageFromMessageHandler(reply);
                        promise.complete((T) response.getResponseBodyAs(responseType.getComponentType()));
                    } else {
                        promise.fail(reply.cause());
                    }
                });
                return promise.future();
            })
            .collect(Collectors.toList());

        CompositeFuture.all(futures).setHandler(ar -> {
            if (ar.succeeded()) {
                List<T> results = ar.result().list();
                ctx.response()
                   .setStatusCode(201)
                   .end(gson.toJson(results));
            } else {
                ctx.fail(500, ar.cause());
            }
        });
    }

    // ========== GENERACIÓN DE DATOS ==========

    /**
     * Crea datos de prueba
     */
//    private void createSomeData(int number) {
//        Random rnd = new Random();
//        IntStream.range(0, number).forEach(elem -> {
//            int id = rnd.nextInt(); 
//            groups.put(id, new Group());
//        });
//    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        // Limpiar datos al detener el verticle
        groups.clear();
        super.stop(stopFuture);
    }
}