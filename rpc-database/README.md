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

## Info
```
Es wurden 2 Flags gesetzt, um...
-XX:+EnableDynamicAgentLoading // dynamischen Agent-Laden zu unterdrücken
-Xshare:off // die CDS-Warnung zu unterdrücken

Die Warnungen kommen wegen der Abhängigkeiten, die in Tests gesetzt wurden.
```