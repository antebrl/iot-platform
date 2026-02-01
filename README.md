## Run
```bash
docker compose up --build
docker compose down
docker compose ps --all
```
## Scaling
```bash
docker compose up --build --scale sensor=3
```

## Hazelcast
### (http://localhost:8081/login.html?redirectUrl=/)
#### login: admin, PW=Geheim42!
## Map für Hazelcast
```bash
CREATE OR REPLACE MAPPING sensorData (
__key VARCHAR,
this JSON
)
TYPE IMap
OPTIONS (
'keyFormat' = 'varchar',
'valueFormat' = 'json'
);
```
## SQL Abfragen (Beispiele)
### Alle Werte:
```bash
SELECT JSON_VALUE(this, '$.sensorId') AS sensorId,
       JSON_VALUE(this, '$.temperature') AS temperature
FROM sensorData;
```
### Durchschnittstemperatur:
```bash
SELECT JSON_VALUE(this, '$.sensorId') AS sensorId,
       AVG(CAST(JSON_VALUE(this, '$.temperature') AS DOUBLE)) AS avg_temp
FROM sensorData
GROUP BY sensorId;
```
### Nur hohe Temperaturen:
```bash
SELECT JSON_VALUE(this, '$.sensorId') AS sensorId,
       JSON_VALUE(this, '$.temperature') AS temperature
FROM sensorData
WHERE CAST(JSON_VALUE(this, '$.temperature') AS DOUBLE) > 25;
```
### Messwerte eines bestimmten Sensors abfragen (z.B. 450)
```bash
SELECT
CAST(JSON_VALUE (this, '$.sensorId') AS INT) AS sensorId,
CAST(JSON_VALUE (this, '$.temperature') AS DOUBLE) AS temperature
FROM
sensorData
WHERE
CAST(JSON_VALUE (this, '$.sensorId') AS INT) = 450;
```
### Maximale Temperatur pro Sensor
```bash
SELECT
  CAST(JSON_VALUE(this, '$.sensorId') AS INT) AS sensorId,
  MAX(CAST(JSON_VALUE(this, '$.temperature') AS DOUBLE)) AS maxTemperature
FROM sensorData
GROUP BY CAST(JSON_VALUE(this, '$.sensorId') AS INT);
```
### Anzahl der Messwerte pro Sensor
```bash
SELECT
  CAST(JSON_VALUE(this, '$.sensorId') AS INT) AS sensorId,
  COUNT(*) AS measurementsCount
FROM sensorData
GROUP BY CAST(JSON_VALUE(this, '$.sensorId') AS INT);
```
### Alle Messungen mit Temperatur und einem Label „hoch“ oder „niedrig“
```bash
SELECT
  sensorId,
  temperature,
  CASE
    WHEN temperature > 25 THEN 'hoch'
    ELSE 'niedrig'
  END AS tempLevel
FROM (
  SELECT
    CAST(JSON_VALUE(this, '$.sensorId') AS INT) AS sensorId,
    CAST(JSON_VALUE(this, '$.temperature') AS DOUBLE) AS temperature
  FROM sensorData
);
```
### Zusammenfassung von Werten mit mehreren Aggregationen
```bash
SELECT
  sensorId,
  COUNT(*) AS countMeasurements,
  AVG(temperature) AS avgTemperature,
  MIN(temperature) AS minTemperature,
  MAX(temperature) AS maxTemperature
FROM (
  SELECT
    CAST(JSON_VALUE(this, '$.sensorId') AS INT) AS sensorId,
    CAST(JSON_VALUE(this, '$.temperature') AS DOUBLE) AS temperature
  FROM sensorData
)
GROUP BY sensorId;
```
### 
```bash
```

### 
```bash
```

### Swarm-Befehle:
#### Swarm initialisieren (einmalig)
```bash
docker swarm init
```

# In deinem Projektverzeichnis:

docker build -t mo4x_teama_http-server ./http-server
docker build -t mo4x_teama_rpc-database ./rpc-database
docker build -t mo4x_teama_iot-gateway ./iot-gateway
docker build -t mo4x_teama_frontend ./frontend
docker build -t mo4x_teama_sensor ./mqtt


#### Stack deployen
```bash
docker stack deploy -c docker-compose.yml mo4x_teama
```
#### Status prüfen
```bash
docker service ls
```
#### Replikate live anpassen
```bash
docker service scale Mo-4X-TeamA_hazelcast-node=5
```
#### Stack entfernen
```bash
docker stack rm mo4x_teama
```

#### Schaltet Docker Swarm aus 
```bash
docker swarm leave --force
```
### 
```bash
.\scripts\start.ps1

```
###
```bash
.\scripts\stop.ps1
```
