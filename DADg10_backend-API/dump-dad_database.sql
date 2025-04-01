-- MySQL Workblench 
--
-- Host: localhost    Database: dad_database
-- ------------------------------------------------------
-- Server version	8.0.28


-- Elimina la tabla `Grupo` si ya existe y la vuelve a crear
DROP TABLE IF EXISTS `Grupo`;
CREATE TABLE Grupo (
    IdGrupo INT AUTO_INCREMENT PRIMARY KEY,  -- ID único del grupo
    mqtt_grupoch VARCHAR(255) NOT NULL  -- Canal MQTT del grupo
) ENGINE=InnoDB 
  AUTO_INCREMENT=1  -- Inicia en 1 para evitar colisiones con otras tablas
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_0900_ai_ci;

-- Elimina la tabla `Placa` si ya existe y la vuelve a crear
DROP TABLE IF EXISTS `Placa`;
CREATE TABLE Placa (
    IdPlaca INT AUTO_INCREMENT PRIMARY KEY,  -- ID único de la placa
    IdGrupo INT NOT NULL,  -- Relación con `Grupo`
    mqtt_ch VARCHAR(255) NOT NULL,  -- Canal MQTT de la placa
    FOREIGN KEY (IdGrupo) REFERENCES Grupo(IdGrupo) ON DELETE CASCADE  -- Borra placas si se elimina su grupo
) ENGINE=InnoDB 
  AUTO_INCREMENT=10000  -- Inicia en 10000 para evitar colisiones con otras tablas
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_0900_ai_ci;

-- Elimina la tabla `Sensor_Aire` si ya existe y la vuelve a crear
DROP TABLE IF EXISTS `Sensor_Aire`;
CREATE TABLE Sensor_Aire (
    IdSensor INT AUTO_INCREMENT PRIMARY KEY,  -- ID único del sensor
    IdPlaca INT NOT NULL,  -- Relación con `Placa`
    Valor DOUBLE NOT NULL,  -- Valor de la medición
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Fecha y hora de la medición
    FOREIGN KEY (IdPlaca) REFERENCES Placa(IdPlaca) ON DELETE CASCADE  -- Borra sensores si se elimina la placa
) ENGINE=InnoDB 
  AUTO_INCREMENT=20000  -- Inicia en 20000 para evitar colisiones con otras tablas
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_0900_ai_ci;

-- Elimina la tabla `Ventilador` si ya existe y la vuelve a crear
DROP TABLE IF EXISTS `Ventilador`;
CREATE TABLE Ventilador (
    IdVentilador INT AUTO_INCREMENT PRIMARY KEY,  -- ID único del ventilador
    IdPlaca INT NOT NULL,  -- Relación con `Placa`
    Estado BOOLEAN NOT NULL,  -- Estado del ventilador (1 = encendido, 0 = apagado)
    timestamp DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL,  -- Fecha y hora del cambio de estado
    FOREIGN KEY (IdPlaca) REFERENCES Placa(IdPlaca) ON DELETE CASCADE  -- Borra ventiladores si se elimina la placa
) ENGINE=InnoDB 
  AUTO_INCREMENT=30000  -- Inicia en 30000 para evitar colisiones con otras tablas
  DEFAULT CHARSET=utf8mb4 
  COLLATE=utf8mb4_0900_ai_ci;
