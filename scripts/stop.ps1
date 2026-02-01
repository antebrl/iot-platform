# Remove the stack
docker stack rm mo4x_teama
Write-Host "Stack mo4x_teama is being removed..."

# Wait until no services are listed
Write-Host "Waiting for all services to be removed..."
do {
    $services = docker service ls --format "{{.Name}}"
    Start-Sleep -Seconds 2
} while ($services -match "mo4x_teama")

Write-Host "All services have been removed."

# Wait until all related containers are removed
Write-Host "Waiting for all related containers to be removed..."
do {
    $containerCheck = docker ps -a --format "{{.Names}}" | Where-Object { $_ -like "mo4x_teama*" }
    Start-Sleep -Seconds 2
} while ($containerCheck.Count -gt 0)

Write-Host "All related containers have been removed."

# Optional: List networks/volumes if you want to check them manually
# docker network ls | Where-Object { $_ -like "*mo4x_teama*" }
# docker volume ls | Where-Object { $_ -like "*mo4x_teama*" }

Write-Host "Stack mo4x_teama has been completely removed!"