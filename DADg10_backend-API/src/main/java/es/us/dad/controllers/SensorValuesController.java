package es.us.dad.controllers;

import java.util.Calendar;

import es.us.dad.mqtt.MqttClientUtil;
import es.us.dad.mysql.entities.Grupo;
import es.us.dad.mysql.entities.Placa;
import es.us.dad.mysql.entities.SensorAire;
import es.us.dad.mysql.messages.DatabaseEntity;
import es.us.dad.mysql.messages.DatabaseMessage;
import es.us.dad.mysql.messages.DatabaseMessageType;
import es.us.dad.mysql.messages.DatabaseMethod;
import es.us.dad.mysql.rest.RestEntityMessage;
import io.vertx.core.Future;
import io.vertx.core.Promise;

/**
 * Controller associated with the SensorValue entity.
 */
public class SensorValuesController extends AbstractController {

    private static final double TEMPERATURE_THRESHOLD = 30.0;

    public SensorValuesController() {
        super(DatabaseEntity.SensorAire);
    }

    public void start(Promise<Void> startFuture) {
        MqttClientUtil mqttClientUtil = MqttClientUtil.getInstance(vertx);

        getVertx().eventBus().consumer(RestEntityMessage.SensorValue.getAddress(), message -> {
            DatabaseMessage databaseMessage = gson.fromJson((String) message.body(), DatabaseMessage.class);

            switch (databaseMessage.getMethod()) {
                case CreateSensorValue:
                    launchDatabaseOperation(message);
                    SensorAire sensorValue = databaseMessage.getRequestBodyAs(SensorAire.class);

                    // Obtener la entidad Sensor
                    launchDatabaseOperation(DatabaseEntity.SensorAire,
                            new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.SensorAire,
                                    DatabaseMethod.GetSensor, sensorValue.getIdSensor()))
                            .future().onComplete(res -> {
                                if (res.succeeded() && res.result() != null) {
                                    SensorAire sensor = res.result().getResponseBodyAs(SensorAire.class);

                                    if (sensor != null) {
                                        // Obtener la entidad Placa
                                        launchDatabaseOperation(DatabaseEntity.Placa,
                                                new DatabaseMessage(DatabaseMessageType.SELECT, DatabaseEntity.Placa,
                                                        DatabaseMethod.GetDevice, sensor.getIdPlaca()))
                                                .future().onComplete(resDevice -> {
                                                    if (resDevice.succeeded() && resDevice.result() != null) {
                                                        Placa device = resDevice.result().getResponseBodyAs(Placa.class);

                                                        if (device != null) {
                                                            // Publicar en canal del dispositivo
                                                            mqttClientUtil.publishMqttMessage(device.getMqttCh(),
                                                                    gson.toJson(sensorValue), handler -> {
                                                                        System.out.println(handler.result());
                                                                    });

                                                            // Obtener el grupo
                                                            launchDatabaseOperation(DatabaseEntity.Grupo,
                                                                    new DatabaseMessage(DatabaseMessageType.SELECT,
                                                                            DatabaseEntity.Grupo,
                                                                            DatabaseMethod.GetGroup,
                                                                            device.getIdGrupo()))
                                                                    .future().onComplete(resGroup -> {
                                                                        if (resGroup.succeeded() && resGroup.result() != null) {
                                                                            Grupo group = resGroup.result().getResponseBodyAs(Grupo.class);

                                                                            if (group != null) {
                                                                                // Publicar en canal del grupo
                                                                                String valueToSend = (sensorValue.getValor() > TEMPERATURE_THRESHOLD) ? "1" : "0";
                                                                                mqttClientUtil.publishMqttMessage(group.getMqttGrupoch(),
                                                                                        valueToSend, handler -> {
                                                                                            System.out.println(handler.result());
                                                                                        });
                                                                            }
                                                                        }
                                                                    });

                                                            // Actualizar la Placa con fecha actual
                                                            Placa updatedDevice = new Placa();
                                                            updatedDevice.setIdPlaca(device.getIdPlaca());
                                                            updatedDevice.setIdGrupo(device.getIdGrupo());

                                                            launchDatabaseOperation(DatabaseEntity.Placa,
                                                                    new DatabaseMessage(DatabaseMessageType.UPDATE, DatabaseEntity.Placa,
                                                                            DatabaseMethod.EditDevice, updatedDevice));
                                                        }
                                                    }
                                                });
                                    }
                                }
                            });
                    break;

                case DeleteSensorValue:
                    launchDatabaseOperation(message);
                    break;

                case GetLastSensorValueFromSensorId:
                    launchDatabaseOperation(message);
                    break;

                case GetLatestSensorValuesFromSensorId:
                    launchDatabaseOperation(message);
                    break;

                default:
                    message.fail(401, "Method not allowed");
            }
        });

        startFuture.complete();
    }

    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }
}
