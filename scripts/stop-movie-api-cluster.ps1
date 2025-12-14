param (
    [int]$START_PORT = 9001,
    [int]$END_PORT   = 9003
)

Write-Host "Stopping movie_api instances..."
Write-Host "Port range: $START_PORT -> $END_PORT"
Write-Host ""

# Find java processes running movie_api with server.port
$processes = Get-CimInstance Win32_Process |
        Where-Object {
            $_.Name -eq "java.exe" -and
                    $_.CommandLine -match "movie_api-0.0.1-SNAPSHOT.jar"
        }

if (-not $processes) {
    Write-Host "No running movie_api instances found."
    exit 0
}

foreach ($proc in $processes) {

    foreach ($port in $START_PORT..$END_PORT) {

        if ($proc.CommandLine -match "--server.port=$port") {

            Write-Host "Stopping instance on port $port (PID $($proc.ProcessId))"
            Stop-Process -Id $proc.ProcessId -Force
        }
    }
}

Write-Host ""
Write-Host "All matching movie_api instances stopped."
