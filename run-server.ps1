<#
Starts the IoT server as a background PowerShell job.
Usage:
  .\run-server.ps1            # starts server on default port (9000)
  .\run-server.ps1 -Port 9001 # starts server on port 9001
#>
param(
    [int]$Port = 9000
)

Write-Host "Starting IoTServer on port $Port as a background job..."

# Start a background job that invokes Maven exec plugin. The job will keep running until the server is stopped.
# Capture the current working folder so the background job runs from the project root
$cwd = (Get-Location).Path
Start-Job -ScriptBlock {
  param($p, $cwd)
  Write-Host "[job] Running mvn exec for IoTServer on port $p from $cwd"
  Set-Location $cwd
  & mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java "-Dexec.mainClass=server.IoTServer" "-Dexec.args=$p"
} -ArgumentList $Port, $cwd | Out-Null

Write-Host "Server job started. Use 'Get-Job' to list jobs, 'Receive-Job -Keep -Id <id>' to view output, and 'Stop-Job -Id <id>' to stop it."