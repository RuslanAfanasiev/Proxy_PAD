param (
    [int]$START_PORT = 9001,
    [int]$END_PORT   = 9003,
    [string]$DB_URL = "jdbc:postgresql://localhost:5432/proxydb",
    [string]$DB_USER = "admin",
    [string]$DB_PASS = "adminpass",
    [string]$SYNC_NODE_URL = "http://localhost:9099"
)

$ROOT = Resolve-Path "$PSScriptRoot\.."
$JAR  = "$ROOT\movie_api\target\movie_api-0.0.1-SNAPSHOT.jar"
$LOGS = "$ROOT\logs"

if (!(Test-Path $JAR)) {
    Write-Error "JAR not found at $JAR. Build the project first."
    exit 1
}

$JAVA_CMD = Get-Command java -ErrorAction SilentlyContinue
if (-not $JAVA_CMD) {
    Write-Error "Java not found on PATH. Install JDK or fix PATH."
    exit 1
}

$JAVA = $JAVA_CMD.Source

New-Item -ItemType Directory -Force -Path $LOGS | Out-Null

function Get-PostgresHostPortFromJdbcUrl {
    param([string]$JdbcUrl)

    # expected: jdbc:postgresql://host:port/db
    if ($JdbcUrl -match '^jdbc:postgresql://([^/:]+)(?::([0-9]+))?/') {
        $dbHost = $Matches[1]
        $port = if ($Matches[2]) { [int]$Matches[2] } else { 5432 }
        return [pscustomobject]@{ Host = $dbHost; Port = $port }
    }

    return $null
}

Write-Host "Checking database connectivity..."
$dbEndpoint = Get-PostgresHostPortFromJdbcUrl -JdbcUrl $DB_URL
if ($dbEndpoint) {
    $tcp = Test-NetConnection -ComputerName $dbEndpoint.Host -Port $dbEndpoint.Port -WarningAction SilentlyContinue
    if (-not $tcp.TcpTestSucceeded) {
        Write-Error "Cannot reach PostgreSQL at $($dbEndpoint.Host):$($dbEndpoint.Port). Start DB first (e.g. run scripts\\init-db.ps1) or fix -DB_URL."
        exit 1
    }
} else {
    Write-Warning "Could not parse -DB_URL '$DB_URL' to test connectivity; continuing."
}

Write-Host "Starting movie_api instances..."
Write-Host "Port range: $START_PORT -> $END_PORT"
Write-Host "DB URL: $DB_URL"
Write-Host "DB User: $DB_USER"
Write-Host "Sync node: $SYNC_NODE_URL"
Write-Host ""


$started = @()
for ($port = $START_PORT; $port -le $END_PORT; $port++) {

    $outLog = "$LOGS\movie_api_$port.out.log"
    $errLog = "$LOGS\movie_api_$port.err.log"

    Write-Host "Starting instance on port $port"

    $args = @(
        "-jar `"$JAR`"",
        "--server.port=$port",
        "--spring.datasource.url=`"$DB_URL`"",
        "--spring.datasource.username=`"$DB_USER`"",
        "--spring.datasource.password=`"$DB_PASS`"",
        "--sync.node.url=`"$SYNC_NODE_URL`""
    ) -join ' '

    $proc = Start-Process `
        -FilePath $JAVA `
        -ArgumentList $args `
        -WorkingDirectory $ROOT `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError  $errLog `
        -PassThru

    $started += [pscustomobject]@{
        Port   = $port
        Pid    = $proc.Id
        OutLog = $outLog
        ErrLog = $errLog
    }
}

Start-Sleep -Seconds 5
$exited = @()
foreach ($p in $started) {
    $proc = Get-Process -Id $p.Pid -ErrorAction SilentlyContinue
    if (-not $proc) {
        $exited += $p
    }
}

if ($exited.Count -gt 0) {
    Write-Host ""
    Write-Error "One or more instances exited right after startup. Check logs in $LOGS (most likely DB credentials or DB not running)."
    foreach ($p in $exited) {
        Write-Host ""
        Write-Host "---- Port $($p.Port) exited (PID $($p.Pid)) ----"
        if (Test-Path $p.ErrLog) {
            Write-Host "# tail $($p.ErrLog)"
            Get-Content -Tail 50 $p.ErrLog
        }
        if (Test-Path $p.OutLog) {
            Write-Host "# tail $($p.OutLog)"
            Get-Content -Tail 50 $p.OutLog
        }
    }
    exit 1
}

Write-Host ""
Write-Host "All instances started successfully."
Write-Host "Logs directory: $LOGS"
