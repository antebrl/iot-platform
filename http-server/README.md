## HTTP Server

A simple socket-based HTTP server for handling sensor data using JSON.

### Run standalone

```bash
./mvnw clean package
java -jar .\target\http-server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

The server application will start on port `8080` by default.

### Try it out

In CMD:

`POST REQUEST` to add a new sensor data point:
```bash
curl -X POST -H "Content-Type: application/json" -d "{\"sensorId\": 1, \"temperature\": 25.5}" http://localhost:8080/
```

`GET REQUEST` to retrieve all sensor data points:
```bash
curl http://localhost:8080
```

`UPDATE` to update a specific sensor data entry:
```bash
curl -X PUT -H "Content-Type: application/json" -d "{\"sensorId\": 1, \"temperature\": 26.0}" http://localhost:8080/<id>
```

`DELETE REQUEST` to delete a sensor data entry:
```bash
curl -X DELETE http://localhost:8080/<id>
```

## Testing

### Running Tests

```bash
mvn clean test
```
