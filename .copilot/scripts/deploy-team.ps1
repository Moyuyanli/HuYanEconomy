# .copilot/scripts/deploy-team.ps1
# 配置当前项目的 VS Code 设置以识别 .copilot 目录
# 用法：在目标项目根目录运行 .\.copilot\scripts\deploy-team.ps1

$ErrorActionPreference = "Stop"

$projectRoot = Get-Location
$targetVscode = Join-Path $projectRoot ".vscode"

# 创建 .vscode 目录（如果不存在）
if (!(Test-Path $targetVscode)) {
    New-Item -ItemType Directory -Path $targetVscode -Force | Out-Null
}

# 创建或更新 VS Code settings.json
$settingsPath = Join-Path $targetVscode "settings.json"
if (Test-Path $settingsPath) {
    $settings = Get-Content $settingsPath -Raw | ConvertFrom-Json
} else {
    $settings = @{}
}

# 添加智能体路径配置
$settings | Add-Member -NotePropertyName "chat.agentFilesLocations" -NotePropertyValue @{
    ".copilot/agents" = $true
    ".github/agents" = $false
} -Force

$settings | Add-Member -NotePropertyName "chat.instructionsFilesLocations" -NotePropertyValue @{
    ".copilot/instructions" = $true
    ".github/instructions" = $false
} -Force

$settings | ConvertTo-Json -Depth 10 | Set-Content -Path $settingsPath -Encoding UTF8

Write-Host "配置完成！" -ForegroundColor Green
Write-Host "VS Code 设置已更新: $settingsPath" -ForegroundColor White
Write-Host "`n请重启 VS Code 使配置生效。" -ForegroundColor Yellow
}

Write-Host "`n部署完成！" -ForegroundColor Green
Write-Host "已复制到: $targetGithub" -ForegroundColor White
