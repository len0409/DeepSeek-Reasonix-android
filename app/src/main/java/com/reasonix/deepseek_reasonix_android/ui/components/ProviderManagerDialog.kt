package com.reasonix.deepseek_reasonix_android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reasonix.deepseek_reasonix_android.data.model.ProviderInfo

// ═══════════════════════════════════════════════
// 调色板 — Reasonix dark
// ═══════════════════════════════════════════════

private val Panel = Color(0xFF2A2729)
private val Bg2 = Color(0xFF222022)
private val Card = Color(0xFF282528)
private val Border = Color(0xFF3D3938)
private val BorderStr = Color(0xFF5A5452)
private val Accent = Color(0xFFEA8800)
private val Fg = Color(0xFFF5F2F0)
private val Muted = Color(0xFF9E9896)
private val Muted2 = Color(0xFF7A7270)
private val Success = Color(0xFF40A060)
private val Danger = Color(0xFFE04636)
private val Warning = Color(0xFFE5B830)

/**
 * 模型/服务商管理对话框。
 * 列出当前已配置的 provider，支持新增与删除。
 * 改动写入 Termux config.toml，重启 reasonix serve 后生效。
 */
@Composable
fun ProviderManagerDialog(
    providers: List<ProviderInfo>,
    message: String?,
    onAdd: (ProviderInfo) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddForm by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var baseUrl by remember { mutableStateOf("") }
    var apiKeyEnv by remember { mutableStateOf("") }
    var modelsText by remember { mutableStateOf("") }
    var defaultModel by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(16.dp)),
            color = Panel
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ── 标题 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "模型 / 服务商管理",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Fg
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) {
                        Text("关闭", color = Muted)
                    }
                }

                Text(
                    "配置写入 Termux 的 config.toml，重启 reasonix serve 后生效",
                    fontSize = 12.sp,
                    color = Muted2
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── 操作提示消息 ──
                message?.let { msg ->
                    Text(
                        msg,
                        fontSize = 13.sp,
                        color = if (msg.startsWith("新增") || msg.startsWith("删除成功") || msg.startsWith("更新")) Success else Warning,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // ── 新增按钮 / 表单 ──
                if (!showAddForm) {
                    Button(
                        onClick = { showAddForm = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Accent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("＋ 新增服务商", color = Color.White, fontWeight = FontWeight.Medium)
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Card, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text("新增服务商（OpenAI 兼容中转）", fontSize = 14.sp, color = Fg, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        Field("名称（英文，如 myproxy）", name, { name = it })
                        Field("Base URL（如 https://xxx/v1）", baseUrl, { baseUrl = it })
                        Field("API Key 环境变量名（如 MYPROXY_API_KEY）", apiKeyEnv, { apiKeyEnv = it })
                        Field("模型列表（逗号分隔，如 m1,m2,m3）", modelsText, { modelsText = it })
                        Field("默认模型（可选）", defaultModel, { defaultModel = it })

                        Spacer(modifier = Modifier.height(8.dp))

                        Row {
                            Button(
                                onClick = {
                                    val models = modelsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                                    if (name.isNotBlank() && baseUrl.isNotBlank() && models.isNotEmpty()) {
                                        onAdd(
                                            ProviderInfo(
                                                name = name.trim(),
                                                kind = "openai",
                                                baseUrl = baseUrl.trim(),
                                                models = models,
                                                default = defaultModel.trim().ifBlank { models.first() },
                                                apiKeyEnv = apiKeyEnv.trim()
                                            )
                                        )
                                        // 清空表单
                                        name = ""; baseUrl = ""; apiKeyEnv = ""; modelsText = ""; defaultModel = ""
                                        showAddForm = false
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Accent)
                            ) {
                                Text("保存", color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { showAddForm = false }) {
                                Text("取消", color = Muted)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Provider 列表 ──
                Text("已配置服务商 (${providers.size})", fontSize = 13.sp, color = Muted)
                Spacer(modifier = Modifier.height(4.dp))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (providers.isEmpty()) {
                        Text("暂无服务商", color = Muted2, modifier = Modifier.padding(8.dp))
                    }
                    providers.forEach { p ->
                        ProviderCard(
                            provider = p,
                            onDelete = { onDelete(p.name) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, color = Muted2, fontSize = 12.sp) },
        singleLine = true,
        textStyle = TextStyle(color = Fg, fontSize = 14.sp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

@Composable
private fun ProviderCard(provider: ProviderInfo, onDelete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Card)
            .border(1.dp, Border, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                provider.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Accent,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                "删除",
                fontSize = 12.sp,
                color = Danger,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onDelete() }
                    .padding(6.dp)
            )
        }
        Text(provider.baseUrl, fontSize = 11.sp, color = Muted2, fontFamily = FontFamily.Monospace)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            provider.models.joinToString(", "),
            fontSize = 11.sp,
            color = Muted,
            maxLines = 3
        )
        if (provider.default.isNotBlank()) {
            Text("默认: ${provider.default}", fontSize = 11.sp, color = Success)
        }
        if (provider.apiKeyEnv.isNotBlank()) {
            Text("Key: \${${provider.apiKeyEnv}}", fontSize = 10.sp, color = Muted2, fontFamily = FontFamily.Monospace)
        }
    }
}
