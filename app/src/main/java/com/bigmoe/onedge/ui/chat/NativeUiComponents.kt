package com.bigmoe.onedge.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

/** Safe, declarative UI schema rendered with Compose. It never evaluates model-provided code. */
data class NativeUiMetric(val label: String, val value: String, val detail: String)
data class NativeUiCard(val title: String, val subtitle: String, val body: String)
data class NativeUiBar(val label: String, val value: Double, val valueLabel: String)
data class NativeUiAction(val label: String, val prompt: String)
data class NativeUiSpec(
    val title: String,
    val description: String,
    val metrics: List<NativeUiMetric>,
    val cards: List<NativeUiCard>,
    val bars: List<NativeUiBar>,
    val actions: List<NativeUiAction>
)

object NativeUiParser {
    private const val MAX_JSON_CHARS = 16_000
    private fun clean(o: JSONObject, key: String, max: Int = 240): String = o.optString(key, "").trim().take(max)

    fun parse(source: String): NativeUiSpec? = runCatching {
        if (source.length !in 2..MAX_JSON_CHARS) return null
        val root = JSONObject(source)
        if (root.optString("type", "").lowercase() !in setOf("dashboard", "cards", "comparison", "chart", "choices")) return null
        fun array(name: String): JSONArray = root.optJSONArray(name) ?: JSONArray()
        val metrics = (0 until array("metrics").length()).take(8).mapNotNull { i ->
            val o = array("metrics").optJSONObject(i) ?: return@mapNotNull null
            NativeUiMetric(clean(o, "label", 48), clean(o, "value", 80), clean(o, "detail", 120)).takeIf { it.label.isNotBlank() && it.value.isNotBlank() }
        }
        val cards = (0 until array("cards").length()).take(8).mapNotNull { i ->
            val o = array("cards").optJSONObject(i) ?: return@mapNotNull null
            NativeUiCard(clean(o, "title", 80), clean(o, "subtitle", 100), clean(o, "body", 320)).takeIf { it.title.isNotBlank() }
        }
        val bars = (0 until array("bars").length()).take(10).mapNotNull { i ->
            val o = array("bars").optJSONObject(i) ?: return@mapNotNull null
            val value = o.optDouble("value", Double.NaN)
            if (clean(o, "label", 56).isBlank() || !value.isFinite()) null
            else NativeUiBar(clean(o, "label", 56), value.coerceIn(0.0, 100.0), clean(o, "valueLabel", 48).ifBlank { "${value.coerceIn(0.0, 100.0).toInt()}%" })
        }
        val actions = (0 until array("actions").length()).take(6).mapNotNull { i ->
            val o = array("actions").optJSONObject(i) ?: return@mapNotNull null
            NativeUiAction(clean(o, "label", 64), clean(o, "prompt", 600)).takeIf { it.label.isNotBlank() && it.prompt.isNotBlank() }
        }
        NativeUiSpec(clean(root, "title", 100), clean(root, "description", 240), metrics, cards, bars, actions)
            .takeIf { it.title.isNotBlank() && (it.metrics.isNotEmpty() || it.cards.isNotEmpty() || it.bars.isNotEmpty() || it.actions.isNotEmpty()) }
    }.getOrNull()
}

@Composable
fun NativeUiComponents(spec: NativeUiSpec, onAction: (String) -> Unit, modifier: Modifier = Modifier) {
    var selectedCard by remember(spec.title) { mutableStateOf(0) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(spec.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (spec.description.isNotBlank()) Text(spec.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (spec.metrics.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    spec.metrics.forEach { metric ->
                        Surface(
                            modifier = Modifier.widthIn(min = 100.dp, max = 220.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(metric.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(metric.value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (metric.detail.isNotBlank()) Text(metric.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            if (spec.cards.isNotEmpty()) {
                spec.cards.forEachIndexed { index, card ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = if (selectedCard == index) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            if (selectedCard == index) 2.dp else 1.dp,
                            if (selectedCard == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        ),
                        onClick = { selectedCard = index }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(card.title, fontWeight = FontWeight.SemiBold)
                                if (card.subtitle.isNotBlank()) Text(card.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            }
                            if (selectedCard == index) Text("Selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                val selected = spec.cards[selectedCard.coerceIn(spec.cards.indices)]
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("${selected.title} · Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        if (selected.subtitle.isNotBlank()) Text(selected.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        if (selected.body.isNotBlank()) Text(selected.body, style = MaterialTheme.typography.bodyMedium)
                        else Text("No additional details provided.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (spec.bars.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    spec.bars.forEach { item ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text(item.label, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                Text(item.valueLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                            }
                            Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
                                Box(Modifier.fillMaxWidth((item.value / 100.0).toFloat()).height(8.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.primary))
                            }
                        }
                    }
                    Text("Bars are model-provided values; verify claims against real measurements.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (spec.actions.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    spec.actions.forEachIndexed { index, action ->
                        if (index == 0) Button(onClick = { onAction(action.prompt) }) { Text(action.label) }
                        else OutlinedButton(onClick = { onAction(action.prompt) }) { Text(action.label) }
                    }
                }
            }
        }
    }
}
