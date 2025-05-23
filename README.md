## Run
```bash
docker compose up --build
docker compose down
docker compose ps --all
```
## Scaling
```bash
docker compose up --build --scale iot-gateway=3
```
later with docker swarm