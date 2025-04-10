package es.us.dad.mysql.entities;

import java.util.Objects;

public class Ventilador {

    // Atributos de la clase
    private Integer idVentilador; // Identificador del ventilador
    private Integer idPlaca; // Identificador de la placa a la que pertenece
    private Boolean idEstado; // Estado del ventilador (encendido/apagado)
    private Long timestamp; // Marca de tiempo (momento en que se registró el estado)

    // Constructor vacío
    public Ventilador() {
        super();
    }

    // Constructor con parámetros
    public Ventilador(Integer idVentilador, Integer idPlaca, Boolean idEstado, Long timestamp) {
        super();
        this.idVentilador = idVentilador;
        this.idPlaca = idPlaca;
        this.idEstado = idEstado;
        this.timestamp = timestamp;
    }

    // Métodos getter y setter para acceder y modificar los atributos
    public Integer getIdVentilador() {
        return idVentilador;
    }

    public void setIdVentilador(Integer idVentilador) {
        this.idVentilador = idVentilador;
    }

    public Integer getIdPlaca() {
        return idPlaca;
    }

    public void setIdPlaca(Integer idPlaca) {
        this.idPlaca = idPlaca;
    }

    public Boolean getIdEstado() {
        return idEstado;
    }

    public void setIdEstado(Boolean idEstado) {
        this.idEstado = idEstado;
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
        final int prime = 30000;
        int result = 1;
        result = prime * result + ((idVentilador == null) ? 0 : idVentilador.hashCode());
        result = prime * result + ((idPlaca == null) ? 0 : idPlaca.hashCode());
        result = prime * result + ((idEstado == null) ? 0 : idEstado.hashCode());
        result = prime * result + ((timestamp == null) ? 0 : timestamp.hashCode());
        return result;
    }


    // Método equals() para comparar si dos objetos Ventilador son iguales
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Ventilador other = (Ventilador) obj;
        return Objects.equals(idVentilador, other.idVentilador) &&
               Objects.equals(idPlaca, other.idPlaca) &&
               Objects.equals(idEstado, other.idEstado) &&
               Objects.equals(timestamp, other.timestamp);
    }

    // Método toString() para representar el objeto como una cadena de texto
    @Override
    public String toString() {
        return "Ventilador [idVentilador=" + idVentilador + ", idPlaca=" + idPlaca +
               ", idEstado=" + idEstado + ", timestamp=" + timestamp + "]";
    }

	
}
