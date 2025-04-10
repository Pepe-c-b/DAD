package es.us.dad.mysql.rest;

/**
 * Enumeración que representa las entidades asociadas con los mensajes que el API REST
 * intercambia con el controlador. Estas entidades difieren de las usadas entre el
 * controlador y la base de datos para evitar conflictos entre los mensajes intercambiados.
 * 
 * Cada valor enum tiene:
 * - value: Nombre de la entidad para uso interno
 * - address: Dirección del event bus para comunicación
 * 
 * @author luismi
 */
public enum RestEntityMessage {
    Sensor("SensorRest"), 
    Actuator("ActuatorRest"), 
    Group("GroupRest"), 
    Device("DeviceRest"),
    SensorValue("SensorValueRest"), 
    ActuatorStatus("ActuatorStatusRest");

    // Nombre/valor de la entidad
    private final String value;
    
    // Dirección para el event bus (se mantiene igual que value)
    private final String address;

    /**
     * Constructor privado para los valores enum
     * @param value Nombre de la entidad y dirección del event bus
     */
    private RestEntityMessage(String value) {
        this.value = value;
        this.address = value; // Se mantiene igual que value
    }

    /**
     * Obtiene el nombre de la entidad REST
     * @return Nombre de la entidad
     */
    public String getRestEntityMessage() {
        return value;
    }

    /**
     * Obtiene la dirección del event bus para esta entidad
     * @return Dirección para comunicación vía event bus
     */
    public String getAddress() {
        return address;
    }
}