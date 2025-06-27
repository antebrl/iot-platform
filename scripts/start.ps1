# Build the images
docker build -t mo4x_teama_http-server ./http-server
docker build -t mo4x_teama_rpc-database ./rpc-database
docker build -t mo4x_teama_iot-gateway ./iot-gateway
docker build -t mo4x_teama_frontend ./frontend
docker build -t mo4x_teama_sensor ./mqtt

# Deploy the stack
docker stack deploy -c docker-compose.yml mo4x_teama

Write-Host "Stack mo4x_teama has been deployed!"

# Wait until all services (except hazelcast-node) are started
do {
    $diensteStatus = docker service ls --format "{{.Name}}: {{.Replicas}}"
    $alleLaufen = $true

    foreach ($status in $diensteStatus) {
        if ($status -match "^mo4x_teama_hazelcast-node:") {
            continue  # Skip hazelcast-node
        }

        if ($status -match ": (\d+)/(\d+)") {
            $ist = [int]$matches[1]
            $soll = [int]$matches[2]
            if ($ist -ne $soll) {
                $alleLaufen = $false
                Write-Host "Not all services are running yet: $status"
                break
            }
        } else {
            $alleLaufen = $false
            Write-Host "Could not read replica status: $status"
            break
        }
    }

    if (-not $alleLaufen) {
        Start-Sleep -Seconds 5
    }
} while (-not $alleLaufen)

# Final overview
docker service ls
