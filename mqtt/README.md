## Functional Tests
```bash
mvn clean test
```

### MQTT Explorer mit HiveMQ CE (host: hivemq Port: 1883)
http://localhost:4000

### Node RED
http://localhost:1880

### Node-RED Flow JSON zum Importieren
```
[
  {
    "id": "mqtt_input",
    "type": "mqtt in",
    "z": "mqtt_flow",
    "name": "Temperature Data",
    "topic": "sensor/temperature/#",
    "qos": "0",
    "datatype": "auto",
    "broker": "mqtt_broker",
    "nl": false,
    "rap": true,
    "rh": 0,
    "x": 140,
    "y": 120,
    "wires": [["json_parse", "debug_node"]]
  },
  {
    "id": "json_parse",
    "type": "json",
    "z": "mqtt_flow",
    "name": "Parse JSON",
    "property": "payload",
    "action": "",
    "pretty": false,
    "x": 330,
    "y": 120,
    "wires": [["ui_chart"]]
  },
  {
    "id": "debug_node",
    "type": "debug",
    "z": "mqtt_flow",
    "name": "MQTT Payload",
    "active": true,
    "tosidebar": true,
    "console": false,
    "tostatus": false,
    "complete": "payload",
    "targetType": "msg",
    "statusVal": "",
    "statusType": "auto",
    "x": 330,
    "y": 180,
    "wires": []
  },
  {
    "id": "ui_chart",
    "type": "ui_chart",
    "z": "mqtt_flow",
    "name": "Temperature Chart",
    "group": "ui_group",
    "order": 1,
    "width": 0,
    "height": 0,
    "label": "Temperaturverlauf",
    "chartType": "line",
    "legend": "true",
    "xformat": "HH:mm:ss",
    "interpolate": "linear",
    "nodata": "",
    "dot": false,
    "ymin": "15",
    "ymax": "35",
    "removeOlder": 1,
    "removeOlderPoints": "",
    "removeOlderUnit": "3600",
    "cutout": 0,
    "useOneColor": false,
    "colors": ["#1f77b4", "#ff7f0e", "#2ca02c"],
    "outputs": 1,
    "x": 560,
    "y": 120,
    "wires": []
  },
  {
    "id": "mqtt_broker",
    "type": "mqtt-broker",
    "name": "HiveMQ Broker",
    "broker": "hivemq",
    "port": "1883",
    "clientid": "",
    "usetls": false,
    "protocolVersion": "4",
    "keepalive": "60",
    "cleansession": true,
    "birthTopic": "",
    "birthQos": "0",
    "birthPayload": "",
    "closeTopic": "",
    "closePayload": "",
    "willTopic": "",
    "willQos": "0",
    "willPayload": ""
  },
  {
    "id": "ui_group",
    "type": "ui_group",
    "name": "Temperatur",
    "tab": "ui_tab",
    "order": 1,
    "disp": true,
    "width": "6",
    "collapse": false
  },
  {
    "id": "ui_tab",
    "type": "ui_tab",
    "name": "Sensor Dashboard",
    "icon": "dashboard",
    "order": 1
  }
]
```