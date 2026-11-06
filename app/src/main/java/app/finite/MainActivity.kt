package app.finite

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.finite.accessibility.FiniteAccessibilityService
import app.finite.data.SessionState
import app.finite.data.SessionStore

class MainActivity : ComponentActivity() {
    private lateinit var store: SessionStore
    private var sessionState by mutableStateOf(
        SessionState(5, 0, false, false, "Waiting for a session"),
    )
    private var accessibilityEnabled by mutableStateOf(false)

    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = refreshState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = SessionStore(this)
        refreshState()
        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter(SessionStore.ACTION_STATE_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            FiniteTheme {
                FiniteScreen(
                    state = sessionState,
                    accessibilityEnabled = accessibilityEnabled,
                    onOpenAccessibility = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    onStart = ::startSession,
                    onEnd = { store.end() },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::store.isInitialized) refreshState()
    }

    override fun onDestroy() {
        unregisterReceiver(stateReceiver)
        super.onDestroy()
    }

    private fun refreshState() {
        sessionState = store.snapshot()
        accessibilityEnabled = isFiniteAccessibilityEnabled()
    }

    private fun startSession(limit: Int) {
        if (!accessibilityEnabled) {
            Toast.makeText(this, "Enable FINITE accessibility access first", Toast.LENGTH_LONG)
                .show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            return
        }

        val instagramIntent = packageManager.getLaunchIntentForPackage(INSTAGRAM_PACKAGE)
        if (instagramIntent == null) {
            store.end("Instagram is not installed")
            Toast.makeText(this, "Instagram is not installed", Toast.LENGTH_LONG).show()
            return
        }

        store.start(limit)
        instagramIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(instagramIntent)
    }

    private fun isFiniteAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0,
        ) == 1
        if (!enabled) return false

        val expected = ComponentName(this, FiniteAccessibilityService::class.java)
        val services = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return services.split(':')
            .mapNotNull(ComponentName::unflattenFromString)
            .any { it == expected }
    }

    companion object {
        private const val INSTAGRAM_PACKAGE = "com.instagram.android"
    }
}

private val Ink = Color(0xFF090A0B)
private val Panel = Color(0xFF141517)
private val PanelRaised = Color(0xFF1B1C1F)
private val Acid = Color(0xFFD9FF57)
private val Muted = Color(0xFFA5A6A9)
private val Hairline = Color(0xFF34363A)
private val Danger = Color(0xFFFF8A80)

@Composable
private fun FiniteTheme(content: @Composable () -> Unit) {
    val colors = darkColorScheme(
        primary = Acid,
        onPrimary = Ink,
        background = Ink,
        onBackground = Color.White,
        surface = Panel,
        onSurface = Color.White,
        surfaceVariant = PanelRaised,
        onSurfaceVariant = Muted,
        outline = Hairline,
        error = Danger,
    )
    MaterialTheme(colorScheme = colors, typography = MaterialTheme.typography, content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiniteScreen(
    state: SessionState,
    accessibilityEnabled: Boolean,
    onOpenAccessibility: () -> Unit,
    onStart: (Int) -> Unit,
    onEnd: () -> Unit,
) {
    var limitText by rememberSaveable(state.limit, state.active) {
        mutableStateOf(state.limit.toString())
    }
    val limit = limitText.toIntOrNull()
    val validLimit = limit != null && limit in SessionStore.MIN_LIMIT..SessionStore.MAX_LIMIT
    val focusManager = LocalFocusManager.current

    Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0x182C3911), Color.Transparent),
                        center = Offset(800f, 80f),
                        radius = 900f,
                    ),
                ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp)
                    .padding(top = 58.dp, bottom = 32.dp),
            ) {
                Brand()
                Spacer(Modifier.height(42.dp))
                Text(
                    text = if (state.active) "Your session is live." else "Scroll with an ending.",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    lineHeight = 39.sp,
                    letterSpacing = (-0.8).sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (state.active) {
                        "FINITE is watching for distinct Reel transitions on this device."
                    } else {
                        "Choose the number of Reels you actually want to watch. We’ll remind you when you arrive."
                    },
                    color = Muted,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                )
                Spacer(Modifier.height(30.dp))

                if (state.active) {
                    ActiveSessionCard(state = state, onEnd = onEnd)
                } else {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Panel),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
                    ) {
                        Column(Modifier.padding(22.dp)) {
                            Text(
                                text = "REEL LIMIT",
                                color = Acid,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                letterSpacing = 1.5.sp,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = limitText,
                                onValueChange = { newValue ->
                                    if (newValue.length <= 3 && newValue.all(Char::isDigit)) {
                                        limitText = newValue
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                suffix = { Text("Reels", color = Muted) },
                                isError = limitText.isNotEmpty() && !validLimit,
                                supportingText = if (!validLimit) {
                                    { Text("Enter a number from 1 to 100") }
                                } else null,
                                textStyle = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = {
                                    focusManager.clearFocus()
                                    limit?.let(onStart)
                                },
                                enabled = validLimit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Acid,
                                    contentColor = Ink,
                                    disabledContainerColor = Hairline,
                                    disabledContentColor = Muted,
                                ),
                            ) {
                                Text("Start Session", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                AccessibilityCard(
                    enabled = accessibilityEnabled,
                    onOpenAccessibility = onOpenAccessibility,
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "DETECTOR STATUS",
                    color = Muted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = state.detectorStatus,
                    color = Color(0xFFD0D1D3),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Counting is best-effort because Instagram exposes no official per-Reel API. FINITE processes accessibility metadata on-device and stores no browsing content.",
                    color = Color(0xFF77797E),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}

@Composable
private fun Brand() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .background(Acid, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.size(20.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.15f, size.height * 0.1f)
                    lineTo(size.width * 0.9f, size.height * 0.1f)
                    lineTo(size.width * 0.9f, size.height * 0.35f)
                    lineTo(size.width * 0.43f, size.height * 0.35f)
                    lineTo(size.width * 0.43f, size.height * 0.53f)
                    lineTo(size.width * 0.82f, size.height * 0.53f)
                    lineTo(size.width * 0.82f, size.height * 0.76f)
                    lineTo(size.width * 0.43f, size.height * 0.76f)
                    lineTo(size.width * 0.43f, size.height * 0.95f)
                    lineTo(size.width * 0.15f, size.height * 0.95f)
                    close()
                }
                drawPath(path, Ink)
            }
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = "FINITE",
            color = Color.White,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 3.4.sp,
        )
    }
}

@Composable
private fun ActiveSessionCard(state: SessionState, onEnd: () -> Unit) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Panel),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Hairline, RoundedCornerShape(24.dp)),
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "REELS WATCHED",
                color = Acid,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "${state.count}/${state.limit}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 64.sp,
                letterSpacing = (-2).sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = when {
                    state.count == 0 -> "Open Reels and start scrolling"
                    state.count < state.limit -> "${state.limit - state.count} left in your plan"
                    state.limitAcknowledged -> "Limit acknowledged · still counting"
                    else -> "You reached your plan"
                },
                color = Muted,
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(20.dp))
            TextButton(
                onClick = onEnd,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = Danger),
            ) {
                Text("End Session", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun AccessibilityCard(enabled: Boolean, onOpenAccessibility: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = PanelRaised),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (enabled) Acid else Danger, CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (enabled) "Accessibility ready" else "Setup required",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (enabled) {
                        "On-device Reel detection is enabled"
                    } else {
                        "Allow FINITE to identify Reel transitions"
                    },
                    color = Muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )
            }
            TextButton(onClick = onOpenAccessibility) {
                Text(if (enabled) "Settings" else "Enable", fontWeight = FontWeight.Bold)
            }
        }
    }
}

