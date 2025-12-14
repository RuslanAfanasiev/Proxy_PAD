$DB_NAME = "proxydb"
$DB_USER = "admin"
$DB_PASS = "adminpass"
$CONTAINER = "postgres-primary"
$MAINT_DB = "postgres"

Write-Host "Starting database cluster..."
docker compose up -d postgres-primary postgres-replica

Write-Host "Waiting for PostgreSQL to be ready..."
do {
    Start-Sleep -Seconds 2
    docker exec $CONTAINER pg_isready `
        -U $DB_USER `
        -d $MAINT_DB | Out-Null
} while ($LASTEXITCODE -ne 0)

Write-Host "PostgreSQL is ready"

Write-Host "Checking if database '$DB_NAME' exists..."

$dbExists = docker exec $CONTAINER psql `
    -U $DB_USER `
    -d $MAINT_DB `
    -tAc "SELECT 1 FROM pg_database WHERE datname='$DB_NAME';"

if ($dbExists -ne "1") {
    Write-Host "Database not found. Creating..."
    docker exec $CONTAINER psql `
        -U $DB_USER `
        -d $MAINT_DB `
        -c "CREATE DATABASE $DB_NAME;"
    Write-Host "Database '$DB_NAME' created"
} else {
    Write-Host "Database '$DB_NAME' already exists"
}

Write-Host "Database initialization complete"
