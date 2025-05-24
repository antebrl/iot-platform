## Run standalone
```bash
mvn clean compile
```
## Run standalone
```bash
cd rpc-database
mvn clean package        # generate Protobuf- & gRPC-Classes
java -jar target/rpc-database-1.0-SNAPSHOT.jar 
```

## Functional Tests
```bash
mvn clean test
```