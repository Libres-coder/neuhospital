package com.example.neusoft_hospital.feature.preconsult.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.neusoft_hospital.navigation.Routes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreConsultHomeScreen(navController: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("AI 智能预问诊") }) }
    ) { pad ->
        Column(modifier = Modifier.fillMaxSize().padding(pad).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.SymptomInput.create("text")) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TextSnippet, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Text("文字症状采集", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("通过多轮问答，快速收集症状信息，AI 智能推荐就诊科室与医生。", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.SymptomInput.create("image")) },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.width(12.dp))
                        Text("图文症状采集", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("上传症状图片（如皮疹、患处照片等），AI 视觉识别 + 文字补充，更精准。", style = MaterialTheme.typography.bodySmall)
                }
            }
            Card(
                modifier = Modifier.fillMaxWidth().clickable { navController.navigate(Routes.SmartRecommend.create()) }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(Modifier.width(12.dp))
                        Text("快速智能推荐", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("直接输入关键词（如\"高血压\"\"胃痛\"），立即推荐匹配医生。", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}