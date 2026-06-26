# .github/scripts/deploy-team.ps1
# 将壶言经济 AI 智能体团队配置复制到目标项目
# 用法：.\deploy-team.ps1 -Target "D:\path\to\project"

param(
    [Parameter(Mandatory=$true)]
    [string]$Target
)

$ErrorActionPreference = "Stop"

$sourceDir = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$targetGithub = Join-Path $Target ".github"
$targetAgents = Join-Path $targetGithub "agents"
$targetHooks = Join-Path $targetGithub "hooks"
$targetScripts = Join-Path $targetGithub "scripts"
$targetInstructions = Join-Path $targetGithub "instructions"
$targetPrompts = Join-Path $targetGithub "prompts"

# 创建目录结构
$dirs = @($targetAgents, $targetHooks, $targetScripts, $targetInstructions, $targetPrompts)
foreach ($dir in $dirs) {
    if (!(Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
    }
}

# 复制智能体文件
$agentFiles = Get-ChildItem -Path (Join-Path $sourceDir ".github\agents") -Filter "*.agent.md"
foreach ($file in $agentFiles) {
    Copy-Item $file.FullName -Destination (Join-Path $targetAgents $file.Name) -Force
    Write-Host "  [Agent] $($file.Name)" -ForegroundColor Green
}

# 复制钩子文件
$hookFiles = Get-ChildItem -Path (Join-Path $sourceDir ".github\hooks") -Filter "*.json"
foreach ($file in $hookFiles) {
    Copy-Item $file.FullName -Destination (Join-Path $targetHooks $file.Name) -Force
    Write-Host "  [Hook] $($file.Name)" -ForegroundColor Cyan
}

# 复制脚本文件
$scriptFiles = Get-ChildItem -Path (Join-Path $sourceDir ".github\scripts") -Filter "*.ps1"
foreach ($file in $scriptFiles) {
    Copy-Item $file.FullName -Destination (Join-Path $targetScripts $file.Name) -Force
    Write-Host "  [Script] $($file.Name)" -ForegroundColor Yellow
}

# 复制指令文件
$instructionFiles = Get-ChildItem -Path (Join-Path $sourceDir ".github\instructions") -Filter "*.md"
foreach ($file in $instructionFiles) {
    Copy-Item $file.FullName -Destination (Join-Path $targetInstructions $file.Name) -Force
    Write-Host "  [Instruction] $($file.Name)" -ForegroundColor Magenta
}

# 复制提示文件
$promptFiles = Get-ChildItem -Path (Join-Path $sourceDir ".github\prompts") -Filter "*.md"
foreach ($file in $promptFiles) {
    Copy-Item $file.FullName -Destination (Join-Path $targetPrompts $file.Name) -Force
    Write-Host "  [Prompt] $($file.Name)" -ForegroundColor Blue
}

Write-Host "`n部署完成！" -ForegroundColor Green
Write-Host "已复制到: $targetGithub" -ForegroundColor White
