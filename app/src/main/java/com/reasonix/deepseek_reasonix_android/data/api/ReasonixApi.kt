package com.reasonix.deepseek_reasonix_android.data.api

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import com.reasonix.deepseek_reasonix_android.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Reasonix REST API — 对应 index.html 中 fetch() 调用的所有后端接口。
 */
class ReasonixApi(
    private val baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
) {
    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // ── 发送消息 ──
    suspend fun submit(input: String): Boolean = withContext(Dispatchers.IO) {
        post("/submit", mapOf("input" to input))
        true
    }

    // ── 取消当前操作 ──
    suspend fun cancel() = withContext(Dispatchers.IO) {
        // 新版 serve 的 /cancel 不接受 JSON body，只接受空 body
        val request = Request.Builder()
            .url("$baseUrl/cancel")
            .post("".toRequestBody(null))
            .build()
        try {
            client.newCall(request).execute().close()
        } catch (_: IOException) {
            // 忽略取消请求失败
        }
    }

    // ── 获取历史消息 ──
    suspend fun getHistory(): List<HistoryMessage> = withContext(Dispatchers.IO) {
        val json = get("/history")
        if (json.isNullOrBlank()) return@withContext emptyList()
        try {
            gson.fromJson(json, object : TypeToken<List<HistoryMessage>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── 获取服务器状态 ──
    suspend fun getStatus(): StatusInfo? = withContext(Dispatchers.IO) {
        val json = get("/status")
        if (json.isNullOrBlank()) return@withContext null
        try {
            gson.fromJson(json, StatusInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // ── 会话列表 ──
    suspend fun getSessions(): List<SessionInfo> = withContext(Dispatchers.IO) {
        val json = get("/sessions")
        if (json.isNullOrBlank()) return@withContext emptyList()
        try {
            gson.fromJson(json, object : TypeToken<List<SessionInfo>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── 新建会话 ──
    suspend fun newSession() = withContext(Dispatchers.IO) {
        post("/new")
    }

    // ── 恢复会话 ──
    suspend fun resumeSession(path: String) = withContext(Dispatchers.IO) {
        post("/resume", mapOf("path" to path))
    }

    // ── 删除会话 ──
    suspend fun deleteSession(name: String) = withContext(Dispatchers.IO) {
        post("/delete-session", mapOf("name" to name))
    }

    // ── 压缩对话 ──
    suspend fun compact() = withContext(Dispatchers.IO) {
        post("/compact")
    }

    // ── 获取检查点 ──
    suspend fun getCheckpoints(): List<CheckpointInfo> = withContext(Dispatchers.IO) {
        val json = get("/checkpoints")
        if (json.isNullOrBlank()) return@withContext emptyList()
        try {
            gson.fromJson(json, object : TypeToken<List<CheckpointInfo>>() {}.type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── 回退 ──
    suspend fun rewind(turn: Int, scope: String = "both") = withContext(Dispatchers.IO) {
        post("/rewind", mapOf("turn" to turn, "scope" to scope))
    }

    // ── 分叉 ──
    suspend fun fork(turn: Int, name: String = "") = withContext(Dispatchers.IO) {
        post("/fork", mapOf("turn" to turn, "name" to name))
    }

    // ── 总结 ──
    suspend fun summarize(turn: Int, mode: String) = withContext(Dispatchers.IO) {
        post("/summarize", mapOf("turn" to turn, "mode" to mode))
    }

    // ── 批准工具 ──
    suspend fun approve(
        id: String,
        allow: Boolean,
        session: Boolean = false,
        persist: Boolean = false,
        scope: String = ""
    ) = withContext(Dispatchers.IO) {
        post("/approve", mapOf(
            "id" to id,
            "allow" to allow,
            "session" to session,
            "persist" to persist,
            "scope" to scope
        ))
    }

    // ── 回答提问卡片 ──
    suspend fun answer(id: String, answers: List<Map<String, Any>>) = withContext(Dispatchers.IO) {
        post("/answer", mapOf("id" to id, "answers" to answers))
    }

    // ── 计划模式 ──
    suspend fun setPlan(on: Boolean) = withContext(Dispatchers.IO) {
        post("/plan", mapOf("on" to on))
    }

    // ── 工具审批模式 ──
    suspend fun setToolApprovalMode(mode: String) = withContext(Dispatchers.IO) {
        post("/tool-approval-mode", mapOf("mode" to mode))
    }

    // ═══════════════════════════════════════════════
    // Provider 配置管理（Termux 配置服务 :8790）
    // ═══════════════════════════════════════════════

    private fun cfgBaseUrl(): String = baseUrl.replace(Regex(":\\d+$"), ":8790")

    /** 列出所有已配置的 provider（读 Termux config.toml） */
    suspend fun listProviders(): List<ProviderInfo> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(cfgBaseUrl() + "/api/providers")
                .get()
                .build()
            client.newCall(request).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val obj = gson.fromJson(body, JsonObject::class.java)
                val arr = obj.getAsJsonArray("providers") ?: return@withContext emptyList()
                val list = mutableListOf<ProviderInfo>()
                arr.forEach { el ->
                    val o = el.asJsonObject
                    val models = mutableListOf<String>()
                    o.getAsJsonArray("models")?.forEach { models.add(it.asString) }
                    list.add(
                        ProviderInfo(
                            name = o.get("name")?.asString ?: "",
                            kind = o.get("kind")?.asString ?: "openai",
                            baseUrl = o.get("base_url")?.asString ?: "",
                            models = models,
                            default = o.get("default")?.asString ?: "",
                            apiKeyEnv = o.get("api_key_env")?.asString ?: ""
                        )
                    )
                }
                list
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 新增或更新 provider。返回 (是否成功, 消息) */
    suspend fun upsertProvider(p: ProviderInfo): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val payload = mapOf(
                "name" to p.name,
                "kind" to p.kind,
                "base_url" to p.baseUrl,
                "models" to p.models,
                "default" to p.default,
                "api_key_env" to p.apiKeyEnv
            )
            val request = Request.Builder()
                .url(cfgBaseUrl() + "/api/providers")
                .post(gson.toJson(payload).toRequestBody(jsonMediaType))
                .build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: "{}"
                val obj = gson.fromJson(body, JsonObject::class.java)
                val ok = obj.get("ok")?.asBoolean ?: false
                val msg = obj.get("message")?.asString ?: obj.get("error")?.asString ?: resp.message
                Pair(ok, msg)
            }
        } catch (e: Exception) {
            Pair(false, "连接配置服务失败: ${e.message}")
        }
    }

    /** 删除 provider。返回 (是否成功, 消息) */
    suspend fun deleteProvider(name: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(cfgBaseUrl() + "/api/providers/" + name)
                .delete()
                .build()
            client.newCall(request).execute().use { resp ->
                val body = resp.body?.string() ?: "{}"
                val obj = gson.fromJson(body, JsonObject::class.java)
                val ok = obj.get("ok")?.asBoolean ?: false
                val msg = obj.get("message")?.asString ?: obj.get("error")?.asString ?: resp.message
                Pair(ok, msg)
            }
        } catch (e: Exception) {
            Pair(false, "连接配置服务失败: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════
    // 内部 HTTP 辅助
    // ═══════════════════════════════════════════════

    private suspend fun get(path: String): String? {
        val request = Request.Builder()
            .url("$baseUrl$path")
            .get()
            .build()
        return execute(request)
    }

    private suspend fun post(path: String, body: Any? = null) {
        val requestBody = if (body != null) {
            gson.toJson(body).toRequestBody(jsonMediaType)
        } else {
            "{}".toRequestBody(jsonMediaType)
        }
        val request = Request.Builder()
            .url("$baseUrl$path")
            .post(requestBody)
            .build()
        execute(request)
    }

    private suspend fun execute(request: Request): String? {
        return try {
            val response = client.newCall(request).execute()
            response.body?.string().also { response.close() }
        } catch (e: IOException) {
            null
        }
    }
}
