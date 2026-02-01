#!/bin/bash

HZ_HOST="localhost"
HZ_PORT=5701

echo "Warte, bis Hazelcast auf $HZ_HOST:$HZ_PORT erreichbar ist..."
until nc -z $HZ_HOST $HZ_PORT; do
  sleep 1
done
echo "Hazelcast ist erreichbar, führe SQL-Mapping aus..."

# SQL-Mapping Befehl als JSON an REST API senden
curl -X POST \
  -H "Content-Type: application/json" \
  -d '{"statement":"CREATE OR REPLACE MAPPING sensorData (__key VARCHAR, this JSON) TYPE IMap OPTIONS (''keyFormat'' = ''varchar'', ''valueFormat'' = ''json'');"}' \
  http://$HZ_HOST:$HZ_PORT/hazelcast/sql/execute

echo "SQL-Mapping abgeschlossen."
