# DAD_G10
Proyecto DAD grupo 10



Claves de la base de datos:

Grupo

IdGrupo → INT (PRIMARY KEY, AUTO_INCREMENT)
mqtt_grupoch → VARCHAR(255)

Tabla Placa

IdPlaca → INT (PRIMARY KEY, AUTO_INCREMENT)
IdGrupo → INT (FOREIGN KEY → Grupo(IdGrupo))
mqtt_ch → VARCHAR(255)

Tabla Sensor_Aire

IdSensor → INT (PRIMARY KEY, AUTO_INCREMENT)
IdPlaca → INT (FOREIGN KEY → Placa(IdPlaca))
Valor → DOUBLE
timestamp → DATETIME (DEFAULT CURRENT_TIMESTAMP)

Tabla Ventilador

IdVentilador → INT (PRIMARY KEY, AUTO_INCREMENT)
IdPlaca → INT (FOREIGN KEY → Placa(IdPlaca))
Estado → BOOLEAN
timestamp → DATETIME (DEFAULT CURRENT_TIMESTAMP)


(Si es necesario cambiar el tipo del valor decimelo y lo cambio)

pd: tengo que conectarlo con el vertex tengo que investigarlo
