## Run
```bash
./mvnw clean package
java -jar .\target\iot-gateway-1.0-SNAPSHOT-jar-with-dependencies.jar
```

## Try it out
In CMD:
```bash
curl -X POST http://localhost:8080 -H "Content-Type: application/json" -d "{\"sensorId\":42,\"temperature\":23.5}"
```

```bash
curl http://localhost:8080
```
##
```bash
mvn clean compile
mvn clean test
```