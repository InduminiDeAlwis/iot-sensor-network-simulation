<#
Run the IoT client. This runs the client in the current terminal so you can see output.
Usage:
  .\run-client.ps1                 # default: localhost 9000 deviceA
  .\run-client.ps1 -Host host -Port 9000 -Device deviceA
#>
param(
  [string]$ServerHost = 'localhost',
  [int]$Port = 9000,
  [string]$Device = 'deviceA'
)

$argString = "$ServerHost $Port $Device"
Write-Host "Running IoTDevice with args: $argString"
if (Test-Path -Path "target\classes") {
  # Run the compiled class directly (avoids invoking Maven and potential mainClass conflicts)
  $cp = "target\classes"
  & java -cp $cp client.IoTDevice $ServerHost $Port $Device
} else {
  & mvn org.codehaus.mojo:exec-maven-plugin:3.1.0:java "-Dexec.mainClass=client.IoTDevice" "-Dexec.args=$argString"
}
