package es.us.dad.mysql.entities;

import java.util.Objects;

public class Grupo {

    // Atributos de la clase
    private Integer idGrupo;      // ID único del grupo
    private String mqttGrupoch;   // Canal MQTT del grupo

    // Constructor vacío
    public Grupo() {
        super();
    }

    // Constructor con parámetros
    public Grupo(Integer idGrupo, String mqttGrupoch) {
        super();
        this.idGrupo = idGrupo;
        this.mqttGrupoch = mqttGrupoch;
    }

    // Métodos getter y setter para acceder y modificar los atributos
    public Integer getIdGrupo() {
        return idGrupo;
    }

    public void setIdGrupo(Integer idGrupo) {
        this.idGrupo = idGrupo;
    }

    public String getMqttGrupoch() {
        return mqttGrupoch;
    }

    public void setMqttGrupoch(String mqttGrupoch) {
        this.mqttGrupoch = mqttGrupoch;
    }

    // Método hashCode() para generar un código hash basado en los atributos
    @Override
    public int hashCode() {
        final int prime = 1;
        int result = 1;
        result = prime * result + ((idGrupo == null) ? 0 : idGrupo.hashCode());
        result = prime * result + ((mqttGrupoch == null) ? 0 : mqttGrupoch.hashCode());
        return result;
    }

    // Método equals() para comparar si dos objetos Grupo son iguales
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Grupo other = (Grupo) obj;
        return Objects.equals(idGrupo, other.idGrupo) &&
               Objects.equals(mqttGrupoch, other.mqttGrupoch);
    }

    // Método toString() para representar el objeto como una cadena de texto
    @Override
    public String toString() {
        return "Grupo [idGrupo=" + idGrupo + ", mqttGrupoch=" + mqttGrupoch + "]";
    }
}
