
### MQTT Explorer mit HiveMQ CE (host: hivemq Port: 1883)
http://localhost:4000

### Node RED
http://localhost:1880

### Node-RED Flow JSON zum Importieren
```
[
{
"id": "mqtt_in_1",
"type": "mqtt in",
"z": "flow1",
"name": "Sensor MQTT Input",
"topic": "sensors/#",
"qos": "1",
"datatype": "auto",
"broker": "mqtt_broker",
"x": 150,
"y": 100,
"wires": [["json_parse"]]
},
{
"id": "json_parse",
"type": "json",
"z": "flow1",
"name": "JSON Parser",
"property": "payload",
"action": "",
"pretty": false,
"x": 350,
"y": 100,
"wires": [["debug_out"]]
},
{
"id": "debug_out",
"type": "debug",
"z": "flow1",
"name": "Debug Output",
"active": true,
"tosidebar": true,
"console": false,
"tostatus": false,
"complete": "payload",
"targetType": "msg",
"x": 550,
"y": 100,
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
"protocolVersion": "3.1",
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
}
]
```