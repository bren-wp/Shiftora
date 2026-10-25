package com.brendigo.shiftora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftBackup
import com.brendigo.shiftora.data.BackupCodec
import java.io.ByteArrayOutputStream
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.data.ShiftViewModel
import com.brendigo.shiftora.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The system splash uses the actual Shiftora clock mark, then hands
        // directly to the Calendar after onboarding has been completed.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(11, 31, 59)),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.rgb(248, 250, 252), android.graphics.Color.rgb(248, 250, 252))
        )
        // A navy decor/status backdrop keeps Android's white status icons readable
        // even when edge-to-edge insets are transparent on older device images.
        window.decorView.setBackgroundColor(android.graphics.Color.rgb(11, 31, 59))
        window.statusBarColor = android.graphics.Color.rgb(11, 31, 59)
        setContent {
            val vm: ShiftViewModel = viewModel()
            val shifts by vm.shifts.collectAsState()
            val archived by vm.archived.collectAsState()
            val loading by vm.loading.collectAsState()
            val salary by vm.salary.collectAsState()
            val onboarding by vm.onboarding.collectAsState()
            val darkMode by vm.darkMode.collectAsState()
            val shiftColors by vm.shiftColors.collectAsState()
            val monthlyFunds by vm.monthlyFunds.collectAsState()
            val message by vm.message.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val snackbars = remember { SnackbarHostState() }
            // Open directly into the functional calendar after the splash and
            // completed onboarding. Home remains accessible as the first tab.
            var tab by rememberSaveable { mutableIntStateOf(1) }
            var onboardingStep by rememberSaveable { mutableIntStateOf(0) }
            val screenStateHolder = rememberSaveableStateHolder()
            var editing by remember { mutableStateOf<Shift?>(null) }
            var rotationOpen by remember { mutableStateOf(false) }
            var salaryOpen by remember { mutableStateOf(false) }
            var colorsOpen by remember { mutableStateOf(false) }
            var archiveOpen by remember { mutableStateOf(false) }
            // SAF file operations and JSON validation must not block Compose rendering.
            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("text/csv")
            ) { uri ->
                if (uri != null) scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            // Prepare the entire export before touching the destination.
                            // A serialization failure must not truncate an existing document.
                            val csv = vm.exportCsv()
                            context.contentResolver.openOutputStream(uri)
                                ?.bufferedWriter(Charsets.UTF_8)?.use { it.write(csv) }
                                ?: error("Datoteku nije moguće otvoriti.")
                        }
                    }
                    snackbars.showSnackbar(result.fold(
                        onSuccess = { "CSV je uspješno izvezen." },
                        onFailure = { "Izvoz nije uspio: ${it.message}" }
                    ))
                }
            }
            var pendingRestore by remember { mutableStateOf<ShiftBackup?>(null) }
            val backupLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            val backup = vm.exportBackup()
                            context.contentResolver.openOutputStream(uri)
                                ?.bufferedWriter(Charsets.UTF_8)?.use { it.write(backup) }
                                ?: error("Datoteku nije moguće otvoriti.")
                        }
                    }
                    snackbars.showSnackbar(result.fold(
                        onSuccess = { "Sigurnosna kopija je spremljena." },
                        onFailure = { "Neuspješno spremanje: ${it.message}" }
                    ))
                }
            }
            val restoreLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) scope.launch {
                    val result = runCatching {
                        withContext(Dispatchers.IO) {
                            val input = context.contentResolver.openInputStream(uri)
                                ?: error("Datoteka nije dostupna.")
                            val bytes = input.use { stream ->
                                val buffer = ByteArray(8192)
                                val output = ByteArrayOutputStream()
                                var total = 0L
                                while (true) {
                                    val count = stream.read(buffer)
                                    if (count < 0) break
                                    total += count
                                    require(total <= BackupCodec.MAX_BYTES) { "Datoteka je veća od 16 MB." }
                                    output.write(buffer, 0, count)
                                }
                                output.toByteArray()
                            }
                            BackupCodec.decode(bytes.toString(Charsets.UTF_8))
                        }
                    }
                    result.onSuccess { pendingRestore = it }
                        .onFailure {
                            snackbars.showSnackbar("Neispravna kopija: ${it.message}")
                        }
                }
            }
            SideEffect {
                // Only the home screen has a light header. Calendar, statistics,
                // salary and More use a navy header even when light theme is active.
                // System clock and battery glyphs must follow the visible surface.
                val lightStatusSurface = !darkMode &&
                    (if (onboarding) onboardingStep != 0 else tab == 0)
                window.statusBarColor = if (lightStatusSurface)
                    android.graphics.Color.WHITE
                else android.graphics.Color.rgb(11, 31, 59)
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = lightStatusSurface
                    isAppearanceLightNavigationBars = !darkMode
                }
            }
            LaunchedEffect(message) {
                if (message != null) {
                    val current = message!!
                    vm.consumeMessage()
                    snackbars.showSnackbar(current)
                }
            }

            ShiftoraTheme(darkMode, shiftColors) {
                if (onboarding) {
                    OnboardingScreen(onComplete = vm::finishOnboarding,
                        onStepChange = { onboardingStep = it })
                } else {
                    BackHandler(enabled = tab != 1 && editing == null && !rotationOpen && !salaryOpen && !colorsOpen && !archiveOpen && pendingRestore == null) {
                        tab = 1
                    }
                    val sections = listOf(
                        "Početna" to Icons.Default.Home,
                        "Kalendar" to Icons.Default.CalendarMonth,
                        "Statistika" to Icons.Default.BarChart,
                        "Plaća" to Icons.Default.AccountBalanceWallet,
                        "Više" to Icons.Default.MoreHoriz
                    )
                    Scaffold(
                        // Android 15/16 enforce edge-to-edge, so statusBarColor
                        // alone cannot paint behind system icons. The Scaffold
                        // background must match the visible tab header.
                        containerColor = if (darkMode || tab != 0)
                            Palette.Navy else MaterialTheme.colorScheme.background,
                        snackbarHost = { SnackbarHost(snackbars) },
                        bottomBar = {
                            Row(
                                Modifier.fillMaxWidth().background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
                                ).navigationBarsPadding()
                                    .padding(start = 9.dp, end = 9.dp, top = 7.dp, bottom = 5.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                sections.forEachIndexed { index, option ->
                                    val active = tab == index
                                    val ink = if (active) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant
                                    Column(
                                        Modifier.weight(1f).heightIn(min = 65.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(if (active)
                                                MaterialTheme.colorScheme.primaryContainer
                                                else Color.Transparent)
                                            .semantics {
                                                contentDescription = option.first
                                                selected = active
                                            }
                                            .clickable(role = Role.Tab) { tab = index }
                                            .padding(vertical = 7.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(option.second, contentDescription = null,
                                            tint = ink, modifier = Modifier.size(24.dp))
                                        Spacer(Modifier.height(4.dp))
                                        Text(option.first, maxLines = 1, color = ink,
                                            fontSize = if (LocalDensity.current.fontScale > 1.3f) 9.sp else 11.sp,
                                            fontWeight = if (active)
                                                FontWeight.Bold else FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    ) { padding ->
                        Box(Modifier.padding(padding)) {
                            val add: (LocalDate) -> Unit = { date ->
                                editing = Shift(date = date, kind = ShiftKind.MORNING)
                            }
                            val edit: (Shift) -> Unit = { entry -> editing = entry }
                            val track: (LocalDate) -> Unit = { date ->
                                val existingWork = shifts.firstOrNull { it.date == date && it.kind.isWork }
                                if (existingWork != null) {
                                    editing = existingWork.copy(actual = true)
                                } else if (shifts.any { it.date == date && it.kind.blocksDay }) {
                                    scope.launch {
                                        snackbars.showSnackbar(
                                            "Na ovom datumu je GO, BO, PD ili SD. " +
                                                "Za evidenciju rada prvo uredi postojeći status."
                                        )
                                    }
                                } else {
                                    editing = Shift(date = date, kind = ShiftKind.MORNING, actual = true)
                                }
                            }
                            val goSettings: () -> Unit = { tab = 4 }
                            if (loading) {
                                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            } else screenStateHolder.SaveableStateProvider(tab) {
                                when (tab) {
                                0 -> HomeScreen(shifts, salary, add, edit, track, { tab = it }, goSettings)
                                1 -> CalendarScreen(shifts, salary, monthlyFunds, add, edit,
                                    vm::setMonthlyFund, goSettings)
                                2 -> StatsScreen(shifts, salary, goSettings)
                                3 -> SalaryScreen(shifts, salary, { salaryOpen = true }, goSettings)
                                else -> MoreScreen(
                                    darkMode = darkMode, onDark = vm::setDarkMode,
                                    savedShifts = shifts.size, archivedShifts = archived.size,
                                    onRotation = { rotationOpen = true },
                                    onCsv = { exportLauncher.launch("Shiftora-smjene-${LocalDate.now()}.csv") },
                                    onBackup = { backupLauncher.launch("Shiftora-kopija-${LocalDate.now()}.json") },
                                    onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                                    onEditSalary = { salaryOpen = true },
                                    onShiftColors = { colorsOpen = true },
                                    onArchive = { archiveOpen = true }
                                )
                                }
                            }
                        }
                    }
                    pendingRestore?.let { backup ->
                        AlertDialog(
                            onDismissRequest = { pendingRestore = null },
                            title = { Text("Dodati smjene iz sigurnosne kopije?") },
                            text = {
                                Text("Kopija sadrži ${backup.shifts.size} aktivnih i ${backup.archivedShifts.size} arhiviranih smjena. Dodaju se samo unosi koji ne postoje i ne preklapaju se s tvojim rasporedom. Prošle ni buduće smjene neće se obrisati ili zamijeniti. Trenutačne boje, fond sati i postavke plaće ostaju nepromijenjeni.")
                            },
                            confirmButton = {
                                Button(onClick = {
                                    vm.restoreBackup(backup)
                                    pendingRestore = null
                                }) { Text("Dodaj bez brisanja") }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingRestore = null }) { Text("Odustani") }
                            }
                        )
                    }
                    if (archiveOpen) ShiftArchiveDialog(
                        entries = archived,
                        onRestore = vm::restoreArchived,
                        onDismiss = { archiveOpen = false }
                    )
                    editing?.let { item ->
                        ShiftEditorDialog(
                            entry = item,
                            existing = shifts,
                            onDismiss = { editing = null },
                            onSave = { vm.save(it) },
                            onDelete = { vm.delete(it) }
                        )
                    }
                    if (rotationOpen) RotationDialog(
                        onDismiss = { rotationOpen = false },
                        onGenerate = { start, pattern, days -> vm.rotation(start, pattern, days) }
                    )
                    if (colorsOpen) ShiftColorSettingsDialog(
                        colors = shiftColors,
                        onChange = vm::setShiftColor,
                        onDismiss = { colorsOpen = false }
                    )
                    if (salaryOpen) SalarySettingsDialog(
                        current = salary,
                        onDismiss = { salaryOpen = false },
                        onSave = vm::updateSalary
                    )
                }
            }
        }
    }
}
