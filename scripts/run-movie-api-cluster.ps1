param (
    [int]$START_PORT = 9001,
    [int]$END_PORT   = 9003
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

Write-Host "Starting movie_api instances..."
Write-Host "Port range: $START_PORT -> $END_PORT"
Write-Host ""

for ($port = $START_PORT; $port -le $END_PORT; $port++) {

    $outLog = "$LOGS\movie_api_$port.out.log"
    $errLog = "$LOGS\movie_api_$port.err.log"

    Write-Host "Starting instance on port $port"

    Start-Process `
        -FilePath $JAVA `
        -ArgumentList "-jar `"$JAR`" --server.port=$port" `
        -WorkingDirectory $ROOT `
        -RedirectStandardOutput $outLog `
        -RedirectStandardError  $errLog
}

Write-Host ""
Write-Host "All instances started successfully."
Write-Host "Logs directory: $LOGS"
