package es.us.dad.controllers;

import es.us.dad.mysql.entities.Placa;
import es.us.dad.mysql.entities.Ventilador;
import es.us.dad.mysql.messages.DatabaseEntity;
import es.us.dad.mysql.messages.DatabaseMessage;
import es.us.dad.mysql.messages.DatabaseMessageType;
import es.us.dad.mysql.messages.DatabaseMethod;
import es.us.dad.mysql.rest.RestEntityMessage;
import io.vertx.core.Future;
import io.vertx.core.Promise;

/**
 * Controller associated with the ActuatorState entity. It will perform all
 * operations related to this entity at the request of the Verticle deploying
 * the Rest API. This class extends the basic functionality implemented by the
 * AbstractController class.
 * 
 */
public class ActuatorStatesController extends AbstractController {

    public ActuatorStatesController() {
        super(DatabaseEntity.Ventilador);
    }

    @Override
    public void start(Promise<Void> startFuture) {

        getVertx().eventBus().consumer(RestEntityMessage.ActuatorStatus.getAddress(), message -> {
            DatabaseMessage databaseMessage = gson.fromJson((String) message.body(), DatabaseMessage.class);

            if (databaseMessage.getMethod() == null) {
                message.fail(400, "No method specified");
                return;
            }

            switch (databaseMessage.getMethod()) {
                case CreateActuatorStatus:
                    // Procesamos el mensaje del Actuator (Ventilador)
                    Ventilador actuator = databaseMessage.getRequestBodyAs(Ventilador.class);

                    // Lógica 1: Guardamos el estado del ventilador
                    launchDatabaseOperation(message);

                    // Lógica 2: Actualizamos la Placa correspondiente
                    Placa placa = new Placa(
                        actuator.getIdVentilador(),
                        actuator.getIdPlaca(),
                        null
                    );

                    DatabaseMessage updatePlacaMessage = new DatabaseMessage(
                        DatabaseMessageType.UPDATE,
                        DatabaseEntity.Placa,
                        DatabaseMethod.EditDevice,
                        placa // ← usamos el objeto directamente si está permitido
                    );

                    launchDatabaseOperation(DatabaseEntity.Placa, updatePlacaMessage);
                    break;

                case DeleteActuatorStatus:
                case GetLastActuatorStatusFromActuatorId:
                case GetLatestActuatorStatesFromActuatorId:
                    launchDatabaseOperation(message);
                    break;

                default:
                    message.fail(401, "Method not allowed");
            }
        });

        startFuture.complete();
    }


}
