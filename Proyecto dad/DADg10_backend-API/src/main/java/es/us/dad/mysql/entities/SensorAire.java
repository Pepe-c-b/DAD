package es.us.dad.mysql.entities;

import java.util.Objects;

public class SensorAire {

    // Atributos de la clase
    private Integer idSensor;   // ID único del sensor
    private Integer idPlaca;    // Relación con la placa
    private Double valor;       // Valor de la medición
    private Long timestamp;  // Fecha y hora de la medición

    // Constructor vacío
    public SensorAire() {
        super();
    }

    // Constructor con parámetros
    public SensorAire(Integer idSensor, Integer idPlaca, Double valor, Long timestamp) {
        super();
        this.idSensor = idSensor;
        this.idPlaca = idPlaca;
        this.valor = valor;
        this.timestamp = timestamp;
    }

    // Métodos getter y setter para acceder y modificar los atributos
    public Integer getIdSensor() {
        return idSensor;
    }

    public void setIdSensor(Integer idSensor) {
        this.idSensor = idSensor;
    }

    public Integer getIdPlaca() {
        return idPlaca;
    }

    public void setIdPlaca(Integer idPlaca) {
        this.idPlaca = idPlaca;
    }

    public Double getValor() {
        return valor;
    }

    public void setValor(Double valor) {
        this.valor = valor;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    // Método hashCode() para generar un código hash basado en los atributos
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((idSensor == null) ? 0 : idSensor.hashCode());
        result = prime * result + ((idPlaca == null) ? 0 : idPlaca.hashCode());
        result = prime * result + ((valor == null) ? 0 : valor.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
        return result;
    }

    // Método equals() para comparar si dos objetos SensorAire son iguales
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        SensorAire other = (SensorAire) obj;
        return Objects.equals(idSensor, other.idSensor) &&
               Objects.equals(idPlaca, other.idPlaca) &&
               Objects.equals(valor, other.valor) &&
               Objects.equals(timestamp, other.timestamp);
    }

    // Método toString() para representar el objeto como una cadena de texto
    @Override
    public String toString() {
        return "SensorAire [idSensor=" + idSensor + ", idPlaca=" + idPlaca + 
               ", valor=" + valor + ", timestamp=" + timestamp + "]";
    }
}
