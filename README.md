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
## 
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
docker stack deploy -c docker-compose.yml Mo-4X-TeamA
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
docker stack rm Mo-4X-TeamA
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
