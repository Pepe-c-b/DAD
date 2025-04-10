package es.us.dad.mysql.entities;

import java.util.Objects;

public class Placa {

    private Integer idPlaca; // Identificador único de la placa
    private Integer idGrupo; // Identificador del grupo al que pertenece
    private String mqttCh; // Canal MQTT de la placa

    // Constructor vacío
    public Placa() {
        super();
    }

    // Constructor con parámetros
    public Placa(Integer idPlaca, Integer idGrupo, String mqttCh) {
        super();
        this.idPlaca = idPlaca;
        this.idGrupo = idGrupo;
        this.mqttCh = mqttCh;
    }

    // Métodos getter y setter
    public Integer getIdPlaca() {
        return idPlaca;
    }

    public void setIdPlaca(Integer idPlaca) {
        this.idPlaca = idPlaca;
    }

    public Integer getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(Integer idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getMqttCh() {
        return mqttCh;
    }

    public void setMqttCh(String mqttCh) {
        this.mqttCh = mqttCh;
    }

    // Método hashCode() para generar un código hash basado en los atributos
    
    public int hashCode() {
        final int prime = 10000;
        int result = 1;
        result = prime * result + ((idPlaca == null) ? 0 : idPlaca.hashCode());
        result = prime * result + ((idGrupo == null) ? 0 : idGrupo.hashCode());
        result = prime * result + ((mqttCh == null) ? 0 : mqttCh.hashCode());
        return result;
    }

    // Método equals() para comparar si dos objetos Placa son iguales
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Placa other = (Placa) obj;
        return Objects.equals(idPlaca, other.idPlaca) &&
               Objects.equals(idGrupo, other.idGrupo) &&
               Objects.equals(mqttCh, other.mqttCh);
    }

    // Método toString() para representar el objeto como una cadena de texto
    @Override
    public String toString() {
        return "Placa [idPlaca=" + idPlaca + ", idGrupo=" + idGrupo + ", mqttCh=" + mqttCh + "]";
    }
}
