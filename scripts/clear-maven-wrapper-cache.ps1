# Script to clear Maven wrapper cache
# This can help resolve "fail to move MAVEN_HOME" errors

$MAVEN_M2_PATH = "$env:USERPROFILE\.m2"
if ($env:MAVEN_USER_HOME) {
    $MAVEN_M2_PATH = "$env:MAVEN_USER_HOME"
}

$WRAPPER_DISTS = "$MAVEN_M2_PATH\wrapper\dists"

Write-Host "Checking Maven wrapper cache at: $WRAPPER_DISTS" -ForegroundColor Yellow

if (Test-Path -Path $WRAPPER_DISTS) {
    Write-Host "Found wrapper cache. Removing..." -ForegroundColor Yellow
    try {
        Remove-Item -Path $WRAPPER_DISTS -Recurse -Force
        Write-Host "Successfully cleared Maven wrapper cache!" -ForegroundColor Green
        Write-Host "The next time you run mvnw, it will download Maven again." -ForegroundColor Green
    } catch {
        Write-Host "Error removing cache: $_" -ForegroundColor Red
        Write-Host "You may need to close any processes using these files and try again." -ForegroundColor Yellow
        exit 1
    }
} else {
    Write-Host "Wrapper cache directory does not exist. Nothing to clear." -ForegroundColor Green
}

