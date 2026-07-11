package com.example.neusoft_hospital.feature.ai.data

import com.example.neusoft_hospital.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QwenClient @Inject constructor(private val api: QwenApiService) {

    private val systemPrompt = """
        你是东软医院智能医疗助手。请遵守以下规则：
        1. 提供专业的医疗健康知识、就医建议、检查项目说明。
        2. 不替代医生诊断；明确告知用户"以上内容仅供参考，不能替代专业医疗建议"。
        3. 紧急情况（胸痛、卒中、严重外伤等）建议立即拨打 120。
        4. 用通俗易懂的语言回答，避免过度专业的术语。
    """.trimIndent()

    private fun authHeader(): String {
        val key = BuildConfig.QWEN_API_KEY
        return if (key.isBlank()) "" else "Bearer $key"
    }

    suspend fun chatSimple(userText: String): Result<String> {
        return runCatching {
            if (BuildConfig.QWEN_API_KEY.isBlank()) {
                // 本地兜底，避免无 Key 时崩溃
                return@runCatching localFallback(userText)
            }
            val resp = api.chatCompletions(
                authHeader(),
                QwenRequest(
                    messages = listOf(
                        QwenMessage("system", systemPrompt),
                        QwenMessage("user", userText)
                    )
                )
            )
            resp.choices.firstOrNull()?.message?.content ?: "抱歉，我没有理解您的问题。"
        }.recoverCatching {
            localFallback(userText)
        }
    }

    private fun localFallback(userText: String): String {
        val s = userText.lowercase()
        return when {
            s.contains("胸痛") || s.contains("心绞痛") -> "胸痛可能与心血管疾病相关，建议立即到心血管内科就诊；如伴有大汗、放射至左肩，请立即拨打 120。"
            s.contains("高血压") -> "高血压需长期管理，建议到心血管内科就诊；日常注意低盐饮食、规律服药、监测血压。"
            s.contains("胃痛") || s.contains("胃") -> "胃痛常见原因有胃炎、胃溃疡，建议到消化内科就诊；近期避免辛辣刺激饮食。"
            s.contains("糖尿病") || s.contains("血糖") -> "糖尿病需要内分泌科长期随访管理，注意饮食控制和血糖监测。"
            s.contains("失眠") || s.contains("睡不着") -> "失眠可能与压力、焦虑相关，建议保持规律作息；如长期存在，可到神经内科或中医科就诊。"
            s.contains("皮疹") || s.contains("过敏") -> "皮疹/过敏常见原因有过敏性皮炎、湿疹等，建议到皮肤科就诊，避免搔抓。"
            else -> "我已收到您的描述，建议您到对应科室就诊；如症状严重请立即前往医院或拨打 120。"
        }
    }
}