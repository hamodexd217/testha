package com.bigmoe.onedge.ui.chat

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.bigmoe.onedge.core.EngineState
import com.bigmoe.onedge.core.InferencePhase
import com.bigmoe.onedge.core.InferenceTelemetry
import com.bigmoe.onedge.core.RecentTokenRate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

private data class MonitorMemory(val pssBytes: Long?, val availableBytes: Long?, val lowMemory: Boolean)

/** Memory reads are deliberately off-main, at most once per 2 seconds, never per token. */
private fun readMonitorMemory(context: Context): MonitorMemory {
    val pss = runCatching { Debug.MemoryInfo().also { Debug.getMemoryInfo(it) }.totalPss.toLong() * 1024L }
        .getOrNull()?.takeIf { it > 0 }
    val system = runCatching {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        info
    }.getOrNull()
    return MonitorMemory(pss, system?.availMem, system?.lowMemory ?: false)
}

/** Local state isolates 2 Hz telemetry recompositions from the chat message list. */
@Composable
internal fun LivePerformanceMonitor(
    engineState: EngineState,
    sessionId: String?,
    generating: Boolean,
    readSnapshot: () -> InferenceTelemetry?
) {
    val context = LocalContext.current.applicationContext
    val owner = LocalView.current.findViewTreeLifecycleOwner()
    val model = (engineState as? EngineState.Ready)?.info
    var expanded by remember { mutableStateOf(false) }
    var snapshot by remember(model?.path, sessionId) { mutableStateOf<InferenceTelemetry?>(null) }
    var recent by remember(model?.path, sessionId) { mutableStateOf<Double?>(null) }
    var memory by remember { mutableStateOf<MonitorMemory?>(null) }
    val active by rememberUpdatedState(generating)
    val details by rememberUpdatedState(expanded)
    val read by rememberUpdatedState(readSnapshot)
    LaunchedEffect(model, sessionId, owner) {
        snapshot = null
        recent = null
        if (model == null || owner == null) return@LaunchedEffect
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val rates = RecentTokenRate()
            var baselineRequest: Long? = null
            var trackedRequest: Long? = null
            var memoryAt = Long.MIN_VALUE
            while (true) {
                val next = withContext(Dispatchers.IO) { read() }
                if (next != null) {
                    if (baselineRequest == null) baselineRequest = next.requestId
                    // Do not show another conversation's completed or API-server response on entering a chat.
                    if (active && (next.phase == InferencePhase.PREFILL || next.phase == InferencePhase.GENERATION ||
                                next.requestId != baselineRequest)) trackedRequest = next.requestId
                    if (trackedRequest == next.requestId) {
                        recent = rates.sample(next) ?: if (next.phase == InferencePhase.GENERATION) recent else null
                        snapshot = next
                    }
                }
                val now = SystemClock.elapsedRealtime()
                if (details && (memoryAt == Long.MIN_VALUE || now - memoryAt >= 2_000)) {
                    memory = withContext(Dispatchers.IO) { readMonitorMemory(context) }
                    memoryAt = now
                }
                delay(if (active || snapshot?.phase == InferencePhase.PREFILL || snapshot?.phase == InferencePhase.GENERATION) 500L else 2_000L)
            }
        }
    }
    val phase = when (engineState) {
        is EngineState.Loading -> "Loading model"
        is EngineState.Unloaded -> "No model loaded"
        is EngineState.Failed -> "Model unavailable"
        is EngineState.Ready -> snapshot?.phase?.label ?: if (generating) "Starting / telemetry unavailable" else "Idle"
    }
    Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$phase · ${speed(snapshot?.averageTokensPerSecond)}", modifier = Modifier.weight(1f).padding(top = 12.dp),
                    style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide stats" else "Live stats") }
            }
            if (expanded) {
                Text("Prompt: ${speed(snapshot?.promptTokensPerSecond)} · evaluated ${snapshot?.promptTokens ?: "—"} tokens",
                    style = MaterialTheme.typography.labelSmall)
                Text("Generation: ${speed(recent)} recent · ${speed(snapshot?.averageTokensPerSecond)} average",
                    style = MaterialTheme.typography.labelSmall)
                Text("App PSS: ${mib(memory?.pssBytes)} · System available: ${mib(memory?.availableBytes)}" +
                    if (memory?.lowMemory == true) " · Low memory" else "", style = MaterialTheme.typography.labelSmall)
                val used = snapshot?.contextUsed
                val cap = snapshot?.contextCapacity ?: model?.nCtx?.toLong()
                Text("Context: ${used ?: "—"} / ${cap ?: "—"}" +
                    (snapshot?.contextPercent?.let { " (${String.format(Locale.ROOT, "%.1f", it)}%)" } ?: "") +
                    (snapshot?.remainingContext?.let { " · $it remaining" } ?: " · occupancy unavailable"),
                    style = MaterialTheme.typography.labelSmall)
                snapshot?.draftAcceptancePercent?.let {
                    Text("Draft acceptance: ${String.format(Locale.ROOT, "%.1f", it)}% (${snapshot?.draftAccepted}/${snapshot?.draftProposed})",
                        style = MaterialTheme.typography.labelSmall)
                }
                Text(if (model?.npuPrefillReady == true) "NPU prefill initialized; execution unverified. Decode uses existing backend."
                    else "NPU prefill not active. Decode uses existing backend.", style = MaterialTheme.typography.labelSmall)
                Text("PSS is not model-only RAM. Dedicated GPU/NPU memory unavailable. — = unavailable / insufficient sample.",
                    style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

private fun speed(value: Double?): String = value?.let { String.format(Locale.ROOT, "%.1f tok/s", it) } ?: "—"
private fun mib(value: Long?): String = value?.let { String.format(Locale.ROOT, "%.0f MiB", it / 1048576.0) } ?: "—"
