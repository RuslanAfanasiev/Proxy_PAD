$ROOT = Resolve-Path "$PSScriptRoot\.."

Write-Host "Project root: $ROOT"

# ---- Build & install common ----
Write-Host "Building and installing common (Docker Maven)..."

docker run --rm `
  -v "$ROOT/common:/build" `
  -v "$HOME/.m2:/root/.m2" `
  -w /build `
  maven:3.9.9-eclipse-temurin-21 `
  mvn clean install -DskipTests


# ---- Build movie_api ----
Write-Host "Building movie_api (Docker Maven)..."

docker run --rm `
  -v "$ROOT/movie_api:/build" `
  -v "$HOME/.m2:/root/.m2" `
  -w /build `
  maven:3.9.9-eclipse-temurin-21 `
  mvn clean package -DskipTests

Write-Host "Docker build finished successfully."
