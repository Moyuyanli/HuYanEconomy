# .github/scripts/agent-router.ps1
# PostToolUse 钩子脚本：根据上下文中的状态标记自动路由到对应智能体
#
# 输入：JSON（来自 stdin），包含工具调用的输出内容
# 输出：JSON（路由指令或空）
# 退出码：0 = 继续，2 = 阻断

$ErrorActionPreference = "SilentlyContinue"

try {
    $inputJson = [Console]::In.ReadToEnd()
    if ([string]::IsNullOrWhiteSpace($inputJson)) {
        exit 0
    }

    $data = $inputJson | ConvertFrom-Json
    $content = ""

    # 提取工具输出内容
    if ($data.tool_output) {
        $content = $data.tool_output | Out-String
    }

    # 规则1：Developer 完成，触发 Inspector
    if ($content -match '\[Status:\s*Pending Inspection\]') {
        $result = @{
            hookSpecificOutput = @{
                hookEventName = "PostToolUse"
                systemMessage = "[Auto-Router] 检测到 `[Status: Pending Inspection]`，请使用 `@Inspector` 智能体对当前代码进行需求对齐校验。"
            }
        }
        $result | ConvertTo-Json -Depth 10
        exit 0
    }

    # 规则2：Inspector 通过，触发 Tester
    if ($content -match '\[Action:\s*Approved.*Move to @Tester\]') {
        $result = @{
            hookSpecificOutput = @{
                hookEventName = "PostToolUse"
                systemMessage = "[Auto-Router] 检测到 `[Action: Approved - Move to @Tester]`，请使用 `@Tester` 智能体运行构建验证并生成交付报告。"
            }
        }
        $result | ConvertTo-Json -Depth 10
        exit 0
    }

    # 规则3：Inspector 退回，路由回 Developer
    if ($content -match '\[Action:\s*Refuse.*Return to @Developer\]') {
        $result = @{
            hookSpecificOutput = @{
                hookEventName = "PostToolUse"
                systemMessage = "[Auto-Router] 检测到 `[Action: Refuse - Return to @Developer]`，请使用 `@Developer` 智能体根据问题清单修改代码。"
            }
        }
        $result | ConvertTo-Json -Depth 10
        exit 0
    }

    # 无匹配，继续正常流程
    exit 0
} catch {
    # 出错时不影响正常流程
    exit 0
}
