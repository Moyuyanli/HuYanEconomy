[Console]::InputEncoding = [System.Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [System.Text.UTF8Encoding]::new($false)
$OutputEncoding = [Console]::OutputEncoding
chcp.com 65001 | Out-Null

& "$PSScriptRoot\gradlew.bat" ":economy-main:runConsole" "--refresh-dependencies" "--console=plain"
exit $LASTEXITCODE
