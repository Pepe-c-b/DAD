package es.us.dad.mysql;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import es.us.dad.mysql.entities.Grupo;
import es.us.dad.mysql.entities.Placa;
import es.us.dad.mysql.entities.SensorAire;
import es.us.dad.mysql.entities.Ventilador;
import es.us.dad.mysql.messages.DatabaseEntity;
import es.us.dad.mysql.messages.DatabaseMessage;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;

public class MySQLVerticleAUX extends AbstractVerticle {

    // Pool de conexiones MySQL
    protected MySQLPool mySqlClient;
    // Instancia de Gson para serialización/deserialización JSON
    protected transient Gson gson;

    @Override
    public void start(Promise<Void> startFuture) {
        // Configuración de conexión a MySQL
        MySQLConnectOptions connectOptions = new MySQLConnectOptions()
                .setPort(8080)
                .setHost("localhost")
                .setDatabase("dad")
                .setUser("root")
                .setPassword("root");

        // Opciones del pool de conexiones
        PoolOptions poolOptions = new PoolOptions()
                .setMaxSize(5);

        // Creación del cliente MySQL
        mySqlClient = MySQLPool.pool(vertx, connectOptions, poolOptions);
        gson = new Gson();

        // Configuración de consumidores para las entidades especificadas
        configureGrupoConsumer();
        configurePlacaConsumer();
        configureSensorAireConsumer();
        configureVentiladorConsumer();

        startFuture.complete();
    }

    // ========================================================================
    // Configuración de consumidores del EventBus
    // ========================================================================

    private void configureGrupoConsumer() {
        getVertx().eventBus().consumer(DatabaseEntity.Grupo.getAddress(), message -> {
            DatabaseMessage databaseMessage = gson.fromJson((String) message.body(), DatabaseMessage.class);
            switch (databaseMessage.getMethod()) {
                case Create:
                    createGrupo(databaseMessage.getRequestBodyAs(Grupo.class), databaseMessage, message);
                    break;
                case Get:
                    getGrupo(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case Edit:
                    editGrupo(databaseMessage.getRequestBodyAs(Grupo.class), databaseMessage, message);
                    break;
                case Delete:
                    deleteGrupo(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case GetPlacasFromGrupo:
                    getPlacasFromGrupo(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                default:
                    message.fail(405, "Método no permitido");
                    break;
            }
        });
    }

    private void configurePlacaConsumer() {
        getVertx().eventBus().consumer(DatabaseEntity.Placa.getAddress(), message -> {
            DatabaseMessage databaseMessage = gson.fromJson((String) message.body(), DatabaseMessage.class);
            switch (databaseMessage.getMethod()) {
                case Create:
                    createPlaca(databaseMessage.getRequestBodyAs(Placa.class), databaseMessage, message);
                    break;
                case Get:
                    getPlaca(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case Edit:
                    editPlaca(databaseMessage.getRequestBodyAs(Placa.class), databaseMessage, message);
                    break;
                case Delete:
                    deletePlaca(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case GetSensoresAireFromPlaca:
                    getSensoresAireFromPlaca(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case GetVentiladoresFromPlaca:
                    getVentiladoresFromPlaca(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                default:
                    message.fail(405, "Método no permitido");
                    break;
            }
        });
    }

    private void configureSensorAireConsumer() {
        getVertx().eventBus().consumer(DatabaseEntity.SensorAire.getAddress(), message -> {
            DatabaseMessage databaseMessage = gson.fromJson((String) message.body(), DatabaseMessage.class);
            switch (databaseMessage.getMethod()) {
                case Create:
                    createSensorAire(databaseMessage.getRequestBodyAs(SensorAire.class), databaseMessage, message);
                    break;
                case Get:
                    getSensorAire(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case Edit:
                    editSensorAire(databaseMessage.getRequestBodyAs(SensorAire.class), databaseMessage, message);
                    break;
                case Delete:
                    deleteSensorAire(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case GetLastValueFromSensor:
                    getLastValueFromSensorAire(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                default:
                    message.fail(405, "Método no permitido");
                    break;
            }
        });
    }

    private void configureVentiladorConsumer() {
        getVertx().eventBus().consumer(DatabaseEntity.Ventilador.getAddress(), message -> {
            DatabaseMessage databaseMessage = gson.fromJson((String) message.body(), DatabaseMessage.class);
            switch (databaseMessage.getMethod()) {
                case Create:
                    createVentilador(databaseMessage.getRequestBodyAs(Ventilador.class), databaseMessage, message);
                    break;
                case Get:
                    getVentilador(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case Edit:
                    editVentilador(databaseMessage.getRequestBodyAs(Ventilador.class), databaseMessage, message);
                    break;
                case Delete:
                    deleteVentilador(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                case GetLastStatusFromVentilador:
                    getLastStatusFromVentilador(Integer.parseInt(databaseMessage.getRequestBody()), databaseMessage, message);
                    break;
                default:
                    message.fail(405, "Método no permitido");
                    break;
            }
        });
    }

    // ========================================================================
    // Operaciones CRUD para Grupos
    // ========================================================================

    protected void createGrupo(Grupo grupo, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "INSERT INTO grupos (idGrupo, mqttGrupoch) VALUES (?, ?);";
        
        mySqlClient.preparedQuery(query,
                Tuple.of(grupo.getIdGrupo(), grupo.getMqttGrupoch()), 
                res -> {
                    if (res.succeeded()) {
                        databaseMessage.setResponseBody(grupo);
                        databaseMessage.setStatusCode(200);
                        message.reply(gson.toJson(databaseMessage));
                    } else {
                        handleDatabaseError(message, res.cause());
                    }
                });
    }

    protected void getGrupo(int idGrupo, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM grupos WHERE idGrupo = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idGrupo), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                Grupo grupo = null;
                
                if (resultSet.iterator().hasNext()) {
                    Row elem = resultSet.iterator().next();
                    grupo = new Grupo(
                        elem.getInteger("idGrupo"), 
                        elem.getString("mqttGrupoch"));
                }
                
                databaseMessage.setResponseBody(grupo);
                databaseMessage.setStatusCode(grupo != null ? 200 : 404);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void editGrupo(Grupo grupo, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "UPDATE grupos SET mqttGrupoch = ? WHERE idGrupo = ?;";
        
        mySqlClient.preparedQuery(query,
                Tuple.of(grupo.getMqttGrupoch(), grupo.getIdGrupo()),
                res -> {
                    if (res.succeeded()) {
                        databaseMessage.setResponseBody(grupo);
                        databaseMessage.setStatusCode(200);
                        message.reply(gson.toJson(databaseMessage));
                    } else {
                        handleDatabaseError(message, res.cause());
                    }
                });
    }

    protected void deleteGrupo(int idGrupo, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "DELETE FROM grupos WHERE idGrupo = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idGrupo), res -> {
            if (res.succeeded()) {
                databaseMessage.setResponseBody(idGrupo);
                databaseMessage.setStatusCode(200);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void getPlacasFromGrupo(int idGrupo, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM placas WHERE idGrupo = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idGrupo), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                List<Placa> placas = new ArrayList<>();
                
                for (Row elem : resultSet) {
                    placas.add(new Placa(
                        elem.getInteger("idPlaca"),
                        elem.getInteger("idGrupo"),
                        elem.getString("mqttCh")));
                }
                
                databaseMessage.setResponseBody(placas);
                databaseMessage.setStatusCode(200);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    // ========================================================================
    // Operaciones CRUD para Placas
    // ========================================================================

    protected void createPlaca(Placa placa, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "INSERT INTO placas (idPlaca, idGrupo, mqttCh) VALUES (?, ?, ?);";
        
        mySqlClient.preparedQuery(query,
                Tuple.of(placa.getIdPlaca(), placa.getIdGrupo(), placa.getMqttCh()),
                res -> {
                    if (res.succeeded()) {
                        databaseMessage.setResponseBody(placa);
                        databaseMessage.setStatusCode(200);
                        message.reply(gson.toJson(databaseMessage));
                    } else {
                        handleDatabaseError(message, res.cause());
                    }
                });
    }

    protected void getPlaca(int idPlaca, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM placas WHERE idPlaca = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idPlaca), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                Placa placa = null;
                
                if (resultSet.iterator().hasNext()) {
                    Row elem = resultSet.iterator().next();
                    placa = new Placa(
                        elem.getInteger("idPlaca"),
                        elem.getInteger("idGrupo"),
                        elem.getString("mqttCh"));
                }
                
                databaseMessage.setResponseBody(placa);
                databaseMessage.setStatusCode(placa != null ? 200 : 404);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void editPlaca(Placa placa, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "UPDATE placas SET idGrupo = ?, mqttCh = ? WHERE idPlaca = ?;";
        
        mySqlClient.preparedQuery(query,
                Tuple.of(placa.getIdGrupo(), placa.getMqttCh(), placa.getIdPlaca()),
                res -> {
                    if (res.succeeded()) {
                        databaseMessage.setResponseBody(placa);
                        databaseMessage.setStatusCode(200);
                        message.reply(gson.toJson(databaseMessage));
                    } else {
                        handleDatabaseError(message, res.cause());
                    }
                });
    }

    protected void deletePlaca(int idPlaca, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "DELETE FROM placas WHERE idPlaca = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idPlaca), res -> {
            if (res.succeeded()) {
                databaseMessage.setResponseBody(idPlaca);
                databaseMessage.setStatusCode(200);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void getSensoresAireFromPlaca(int idPlaca, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM sensores_aire WHERE idPlaca = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idPlaca), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                List<SensorAire> sensores = new ArrayList<>();
                
                for (Row elem : resultSet) {
                    sensores.add(new SensorAire(
                        elem.getInteger("idSensor"),
                        elem.getInteger("idPlaca"),
                        elem.getDouble("valor"),
                        elem.getLocalDateTime("timestamp").toInstant(ZoneOffset.UTC).toEpochMilli()));
                }
                
                databaseMessage.setResponseBody(sensores);
                databaseMessage.setStatusCode(200);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void getVentiladoresFromPlaca(int idPlaca, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM ventiladores WHERE idPlaca = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idPlaca), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                List<Ventilador> ventiladores = new ArrayList<>();
                
                for (Row elem : resultSet) {
                    ventiladores.add(new Ventilador(
                        elem.getInteger("idVentilador"),
                        elem.getInteger("idPlaca"),
                        elem.getBoolean("idEstado"),
                        elem.getLong("timestamp")));
                }
                
                databaseMessage.setResponseBody(ventiladores);
                databaseMessage.setStatusCode(200);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    // ========================================================================
    // Operaciones CRUD para Sensores de Aire
    // ========================================================================

    protected void createSensorAire(SensorAire sensor, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "INSERT INTO sensores_aire (idSensor, idPlaca, valor, timestamp) VALUES (?, ?, ?, ?);";
        
        LocalDateTime timestamp = Instant.ofEpochMilli(sensor.getTimestamp())
                                      .atZone(ZoneOffset.UTC)
                                      .toLocalDateTime();
        
        mySqlClient.preparedQuery(query,
                Tuple.of(sensor.getIdSensor(), sensor.getIdPlaca(), 
                        sensor.getValor(), timestamp),
                res -> {
                    if (res.succeeded()) {
                        databaseMessage.setResponseBody(sensor);
                        databaseMessage.setStatusCode(200);
                        message.reply(gson.toJson(databaseMessage));
                    } else {
                        handleDatabaseError(message, res.cause());
                    }
                });
    }

    protected void getSensorAire(int idSensor, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM sensores_aire WHERE idSensor = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idSensor), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                SensorAire sensor = null;
                
                if (resultSet.iterator().hasNext()) {
                    Row elem = resultSet.iterator().next();
                    sensor = new SensorAire(
                        elem.getInteger("idSensor"),
                        elem.getInteger("idPlaca"),
                        elem.getDouble("valor"),
                        elem.getLocalDateTime("timestamp").toInstant(ZoneOffset.UTC).toEpochMilli());
                }
                
                databaseMessage.setResponseBody(sensor);
                databaseMessage.setStatusCode(sensor != null ? 200 : 404);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void editSensorAire(SensorAire sensor, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "UPDATE sensores_aire SET idPlaca = ?, valor = ?, timestamp = ? WHERE idSensor = ?;";
        
        LocalDateTime timestamp = Instant.ofEpochMilli(sensor.getTimestamp())
                                      .atZone(ZoneOffset.UTC)
                                      .toLocalDateTime();
        
        mySqlClient.preparedQuery(query,
                Tuple.of(sensor.getIdPlaca(), sensor.getValor(), 
                        timestamp, sensor.getIdSensor()),
                res -> {
                    if (res.succeeded()) {
                        databaseMessage.setResponseBody(sensor);
                        databaseMessage.setStatusCode(200);
                        message.reply(gson.toJson(databaseMessage));
                    } else {
                        handleDatabaseError(message, res.cause());
                    }
                });
    }

    protected void deleteSensorAire(int idSensor, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "DELETE FROM sensores_aire WHERE idSensor = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idSensor), res -> {
            if (res.succeeded()) {
                databaseMessage.setResponseBody(idSensor);
                databaseMessage.setStatusCode(200);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void getLastValueFromSensorAire(int idSensor, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM sensores_aire WHERE idSensor = ? ORDER BY timestamp DESC LIMIT 1;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idSensor), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                SensorAire sensor = null;
                
                if (resultSet.iterator().hasNext()) {
                    Row elem = resultSet.iterator().next();
                    sensor = new SensorAire(
                        elem.getInteger("idSensor"),
                        elem.getInteger("idPlaca"),
                        elem.getDouble("valor"),
                        elem.getLocalDateTime("timestamp").toInstant(ZoneOffset.UTC).toEpochMilli());
                }
                
                databaseMessage.setResponseBody(sensor);
                databaseMessage.setStatusCode(sensor != null ? 200 : 404);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    // ========================================================================
    // Operaciones CRUD para Ventiladores
    // ========================================================================

    protected void createVentilador(Ventilador ventilador, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "INSERT INTO ventiladores (idVentilador, idPlaca, idEstado, timestamp) VALUES (?, ?, ?, ?);";
        
        mySqlClient.preparedQuery(query,
                Tuple.of(ventilador.getIdVentilador(), ventilador.getIdPlaca(), 
                        ventilador.getIdEstado(), ventilador.getTimestamp()),
                res -> {
                    if (res.succeeded()) {
                        databaseMessage.setResponseBody(ventilador);
                        databaseMessage.setStatusCode(200);
                        message.reply(gson.toJson(databaseMessage));
                    } else {
                        handleDatabaseError(message, res.cause());
                    }
                });
    }

    protected void getVentilador(int idVentilador, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM ventiladores WHERE idVentilador = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idVentilador), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                Ventilador ventilador = null;
                
                if (resultSet.iterator().hasNext()) {
                    Row elem = resultSet.iterator().next();
                    ventilador = new Ventilador(
                        elem.getInteger("idVentilador"),
                        elem.getInteger("idPlaca"),
                        elem.getBoolean("idEstado"),
                        elem.getLong("timestamp"));
                }
                
                databaseMessage.setResponseBody(ventilador);
                databaseMessage.setStatusCode(ventilador != null ? 200 : 404);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void editVentilador(Ventilador ventilador, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "UPDATE ventiladores SET idPlaca = ?, idEstado = ?, timestamp = ? WHERE idVentilador = ?;";
        
        mySqlClient.preparedQuery(query,
                Tuple.of(ventilador.getIdPlaca(), ventilador.getIdEstado(), 
                        ventilador.getTimestamp(), ventilador.getIdVentilador()),
                res -> {
                    if (res.succeeded()) {
                        databaseMessage.setResponseBody(ventilador);
                        databaseMessage.setStatusCode(200);
                        message.reply(gson.toJson(databaseMessage));
                    } else {
                        handleDatabaseError(message, res.cause());
                    }
                });
    }

    protected void deleteVentilador(int idVentilador, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "DELETE FROM ventiladores WHERE idVentilador = ?;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idVentilador), res -> {
            if (res.succeeded()) {
                databaseMessage.setResponseBody(idVentilador);
                databaseMessage.setStatusCode(200);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    protected void getLastStatusFromVentilador(int idVentilador, DatabaseMessage databaseMessage, Message<Object> message) {
        String query = "SELECT * FROM ventiladores WHERE idVentilador = ? ORDER BY timestamp DESC LIMIT 1;";
        
        mySqlClient.preparedQuery(query, Tuple.of(idVentilador), res -> {
            if (res.succeeded()) {
                RowSet<Row> resultSet = res.result();
                Ventilador ventilador = null;
                
                if (resultSet.iterator().hasNext()) {
                    Row elem = resultSet.iterator().next();
                    ventilador = new Ventilador(
                        elem.getInteger("idVentilador"),
                        elem.getInteger("idPlaca"),
                        elem.getBoolean("idEstado"),
                        elem.getLong("timestamp"));
                }
                
                databaseMessage.setResponseBody(ventilador);
                databaseMessage.setStatusCode(ventilador != null ? 200 : 404);
                message.reply(gson.toJson(databaseMessage));
            } else {
                handleDatabaseError(message, res.cause());
            }
        });
    }

    // ========================================================================
    // Métodos auxiliares
    // ========================================================================

    private void handleDatabaseError(Message<Object> message, Throwable cause) {
        System.err.println("Error de base de datos: " + cause.getMessage());
        message.fail(500, "Error interno del servidor: " + cause.getMessage());
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        if (mySqlClient != null) {
            mySqlClient.close();
        }
        super.stop(stopFuture);
    }
}