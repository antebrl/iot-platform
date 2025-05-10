## HTTP Server

A simple HTTP server for handling sensor data using JSON.

### Run

```bash
./mvnw clean package
java -jar .\target\http-server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The server application will start on port 8080 by default.

### Try it out

In CMD:

```bash
curl -X POST http://localhost:8080 -H "Content-Type: application/json" -d "{\"sensorId\":42,\"temperature\":23.5}"
```

```bash
curl http://localhost:8080
```

## Testing

### Running Tests

```bash
mvn clean compile
mvn clean test
```
