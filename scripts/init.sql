CREATE OR REPLACE MAPPING sensorData (
__key VARCHAR,
this JSON
)
TYPE IMap
OPTIONS (
'keyFormat' = 'varchar',
'valueFormat' = 'json'
);