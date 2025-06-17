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
later with docker swarm or kubernetes