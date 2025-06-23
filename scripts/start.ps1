docker build -t mo4x_teama_http-server ./http-server
docker build -t mo4x_teama_rpc-database ./rpc-database
docker build -t mo4x_teama_iot-gateway ./iot-gateway
docker build -t mo4x_teama_frontend ./frontend
docker build -t mo4x_teama_sensor ./mqtt

docker stack deploy -c docker-compose.yml mo4x_teama

Write-Host "Stack mo4x_teama deployed!"

docker service ls