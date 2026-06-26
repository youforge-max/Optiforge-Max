package eu.youforgemax.optiforgemax

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * UI for the 5-band compressor/limiter. All state lives in [UiState]; controls
 * write to it and push to the live effect via [UiState.applyAll]-style setters.
 */
class MainActivity : ComponentActivity() {

    private val dsp = DspEngine()
    private val meter = BandMeter(dsp)
    private val micGranted = mutableStateOf(false)

    private val askMic = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        micGranted.value = granted
        if (granted) meter.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val attached = dsp.attach(DspEngine.GLOBAL_SESSION)

        val hasMic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        micGranted.value = hasMic
        if (hasMic) meter.start() else askMic.launch(Manifest.permission.RECORD_AUDIO)

        setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                Screen(dsp, meter, attached, micGranted.value)
            }
        }
    }

    override fun onDestroy() {
        meter.stop()
        dsp.release()
        super.onDestroy()
    }
}

@Composable
private fun Screen(dsp: DspEngine, meter: BandMeter, attachedInitial: Boolean, micGranted: Boolean) {
    val st = remember { UiState() }
    var attached by remember { mutableStateOf(attachedInitial) }

    // Push initial state into the effect once attached.
    LaunchedEffect(attached) { if (attached) st.applyAll(dsp, meter) }

    // Poll smoothed GR + spectrum ~20 fps.
    val gr = remember { mutableStateListOf(*Array(DspEngine.NUM_BANDS) { 0f }) }
    val spectrum = remember { mutableStateListOf(*Array(BandMeter.SPECTRUM_BARS) { 0f }) }
    LaunchedEffect(micGranted) {
        while (true) {
            for (b in 0 until DspEngine.NUM_BANDS) gr[b] = meter.grDb[b]
            for (k in 0 until BandMeter.SPECTRUM_BARS) spectrum[k] = meter.spectrum[k]
            delay(50)
        }
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("5-Band Compressor / Limiter", fontSize = 22.sp, fontWeight = FontWeight.Bold)

            if (!attached) {
                Text(
                    "Global audio session unavailable. Retry, or bind to a player session.",
                    color = MaterialTheme.colorScheme.error
                )
                Button(onClick = { attached = dsp.attach(DspEngine.GLOBAL_SESSION) }) {
                    Text("Retry attach")
                }
            }

            PresetBar(st) { st.applyAll(dsp, meter) }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = st.masterOn, onCheckedChange = {
                    st.masterOn = it; dsp.setEnabled(it)
                })
                Spacer(Modifier.width(8.dp))
                Text(if (st.masterOn) "Processing ON" else "Processing OFF")
            }

            // Spectrum + calibration
            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("Spectrum", fontWeight = FontWeight.Bold)
                    Spectrum(spectrum)
                    Spacer(Modifier.height(8.dp))
                    SliderRow("Meter calibration", st.meterCalDb, -40f, 40f, "dB") {
                        st.meterCalDb = it; meter.calibrationDb = it
                    }
                    Text(
                        "Tune so GR reads ~0 with no compression, then to taste.",
                        fontSize = 11.sp, color = Color(0xFF999999)
                    )
                }
            }

            Card {
                Column(Modifier.padding(12.dp)) {
                    Text("Input Gain", fontWeight = FontWeight.Bold)
                    SliderRow("Gain", st.inputGain, -40f, 40f, "dB") {
                        st.inputGain = it; dsp.setInputGainDb(it)
                    }
                }
            }

            if (!micGranted) {
                Text("Mic permission denied — meters/spectrum disabled.",
                    color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }

            for (b in 0 until DspEngine.NUM_BANDS) BandCard(dsp, st, b, gr[b])

            LimiterCard(dsp, st)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PresetBar(st: UiState, onLoaded: () -> Unit) {
    val ctx = LocalContext.current
    val repo = remember { PresetRepo(ctx) }
    var names by remember { mutableStateOf(repo.names()) }
    var selected by remember { mutableStateOf(names.firstOrNull() ?: "") }
    var newName by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Presets", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(if (selected.isEmpty()) "— none —" else selected)
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        names.forEach { n ->
                            DropdownMenuItem(text = { Text(n) }, onClick = {
                                selected = n; expanded = false
                            })
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                Button(enabled = selected.isNotEmpty(), onClick = {
                    if (repo.load(selected, st)) onLoaded()
                }) { Text("Load") }
                Spacer(Modifier.width(4.dp))
                OutlinedButton(enabled = selected.isNotEmpty(), onClick = {
                    repo.delete(selected); names = repo.names(); selected = names.firstOrNull() ?: ""
                }) { Text("Del") }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName, onValueChange = { newName = it },
                    label = { Text("Save as") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(enabled = newName.isNotBlank(), onClick = {
                    repo.save(newName.trim(), st); names = repo.names(); selected = newName.trim()
                    newName = ""
                }) { Text("Save") }
            }
        }
    }
}

@Composable
private fun Spectrum(bars: List<Float>) {
    Canvas(Modifier.fillMaxWidth().height(96.dp).background(Color(0xFF111111))) {
        val n = bars.size
        if (n == 0) return@Canvas
        val gap = 2.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        for (i in 0 until n) {
            val h = bars[i].coerceIn(0f, 1f) * size.height
            val x = i * (w + gap)
            val c = when {
                bars[i] > 0.8f -> Color(0xFFE53935)
                bars[i] > 0.5f -> Color(0xFFFFB300)
                else -> Color(0xFF43A047)
            }
            drawRect(color = c, topLeft = androidx.compose.ui.geometry.Offset(x, size.height - h),
                size = androidx.compose.ui.geometry.Size(w, h))
        }
    }
}

@Composable
private fun BandCard(dsp: DspEngine, st: UiState, band: Int, grDb: Float) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Band ${band + 1}  (${dsp.bandLow(band).roundToInt()}–${dsp.bandHigh(band).roundToInt()} Hz)",
                    fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
                )
                Switch(checked = st.bandOn[band], onCheckedChange = {
                    st.bandOn[band] = it; dsp.setBandEnabled(band, it)
                })
            }
            GrMeter(grDb)
            SliderRow("Threshold", st.threshold[band], -100f, 0f, "dBFS") {
                st.threshold[band] = it; dsp.setThreshold(band, it)
            }
            SliderRow("Attack", st.attack[band], 0f, 500f, "ms") {
                st.attack[band] = it; dsp.setAttack(band, it)
            }
            SliderRow("Release", st.release[band], 0f, 3000f, "ms") {
                st.release[band] = it; dsp.setRelease(band, it)
            }
            SliderRow("Ratio", st.ratio[band], 1f, 20f, ":1") {
                st.ratio[band] = it; dsp.setRatio(band, it)
            }
            SliderRow("Makeup", st.makeup[band], -12f, 24f, "dB") {
                st.makeup[band] = it; dsp.setBandPostGain(band, it)
            }
        }
    }
}

@Composable
private fun LimiterCard(dsp: DspEngine, st: UiState) {
    Card {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Output Limiter", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(checked = st.limOn, onCheckedChange = {
                    st.limOn = it; dsp.setLimiterEnabled(it)
                })
            }
            SliderRow("Threshold", st.limThr, -60f, 0f, "dBFS") {
                st.limThr = it; dsp.setLimiterThreshold(it)
            }
            SliderRow("Attack", st.limAtk, 0f, 100f, "ms") {
                st.limAtk = it; dsp.setLimiterAttack(it)
            }
            SliderRow("Release", st.limRel, 0f, 1000f, "ms") {
                st.limRel = it; dsp.setLimiterRelease(it)
            }
            SliderRow("Ratio", st.limRatio, 1f, 50f, ":1") {
                st.limRatio = it; dsp.setLimiterRatio(it)
            }
            SliderRow("Post Gain", st.limPost, -12f, 12f, "dB") {
                st.limPost = it; dsp.setLimiterPostGain(it)
            }
        }
    }
}

/** Horizontal gain-reduction bar, 0..MAX dB. */
@Composable
private fun GrMeter(grDb: Float, maxDb: Float = 24f) {
    val frac = (grDb / maxDb).coerceIn(0f, 1f)
    Column {
        Row {
            Text("GR", fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text("-${"%.1f".format(grDb)} dB", fontSize = 11.sp)
        }
        Box(Modifier.fillMaxWidth().height(8.dp).background(Color(0xFF222222))) {
            Box(Modifier.fillMaxWidth(frac).height(8.dp).background(
                when {
                    grDb > 12f -> Color(0xFFE53935)
                    grDb > 4f -> Color(0xFFFFB300)
                    else -> Color(0xFF43A047)
                }
            ))
        }
    }
}

@Composable
private fun SliderRow(
    label: String, value: Float, min: Float, max: Float, unit: String,
    onChange: (Float) -> Unit
) {
    Column {
        Row {
            Text(label, modifier = Modifier.weight(1f), fontSize = 13.sp)
            Text("${"%.1f".format(value)} $unit", fontSize = 13.sp)
        }
        Slider(value = value, onValueChange = onChange, valueRange = min..max)
    }
}
