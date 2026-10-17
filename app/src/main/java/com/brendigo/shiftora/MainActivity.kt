package com.brendigo.shiftora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.brendigo.shiftora.data.Shift
import com.brendigo.shiftora.data.ShiftBackup
import com.brendigo.shiftora.data.BackupCodec
import java.io.ByteArrayOutputStream
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.data.ShiftViewModel
import com.brendigo.shiftora.ui.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (BuildConfig.DEBUG && intent.getBooleanExtra("shiftora_qa_seed", false)) {
            // Screenshot fixtures are available in debug builds only, never in Play bundles.
            com.brendigo.shiftora.data.ShiftStore(this).use { store ->
                val month = java.time.YearMonth.now()
                store.addRotation(
                    month.atDay(1),
                    listOf(
                        ShiftKind.MORNING, ShiftKind.AFTERNOON, ShiftKind.NIGHT, ShiftKind.OFF,
                        ShiftKind.MORNING, ShiftKind.AFTERNOON, ShiftKind.NIGHT, ShiftKind.OFF
                    ),
                    month.lengthOfMonth()
                )
                // Demonstration only: show planned and actually worked time in real screenshots.
                store.all().filter { it.date.isBefore(java.time.LocalDate.now()) &&
                    it.kind != ShiftKind.OFF
                }.forEach { sample ->
                    store.save(sample.copy(actual = true, overtime = sample.date.dayOfMonth % 11 == 0))
                }
            }
        }
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
            val salary by vm.salary.collectAsState()
            val onboarding by vm.onboarding.collectAsState()
            val darkMode by vm.darkMode.collectAsState()
            val message by vm.message.collectAsState()
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            val snackbars = remember { SnackbarHostState() }
            var tab by rememberSaveable { mutableIntStateOf(0) }
            var editing by remember { mutableStateOf<Shift?>(null) }
            var rotationOpen by remember { mutableStateOf(false) }
            var salaryOpen by remember { mutableStateOf(false) }
            val exportLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("text/csv")
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(vm.exportCsv())
                        } ?: error("Nije moguće otvoriti datoteku.")
                    }.onSuccess {
                        scope.launch { snackbars.showSnackbar("CSV je uspješno izvezen.") }
                    }.onFailure {
                        scope.launch { snackbars.showSnackbar("Izvoz nije uspio: ${it.message}") }
                    }
                }
            }
            var pendingRestore by remember { mutableStateOf<ShiftBackup?>(null) }
            val backupLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.CreateDocument("application/json")
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use {
                            it.write(vm.exportBackup())
                        } ?: error("Datoteka se nije mogla otvoriti.")
                    }.onSuccess {
                        scope.launch { snackbars.showSnackbar("Sigurnosna kopija je spremljena.") }
                    }.onFailure {
                        scope.launch { snackbars.showSnackbar("Neuspješno spremanje: ${it.message}") }
                    }
                }
            }
            val restoreLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) {
                    runCatching {
                        val content = context.contentResolver.openInputStream(uri) ?: error("Datoteka nije dostupna.")
                        val bytes = content.use { input ->
                            val result = ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var count = 0
                            while (true) {
                                val size = input.read(buffer)
                                if (size < 0) break
                                count += size
                                require(count <= BackupCodec.MAX_BYTES) { "Datoteka je veća od 4 MB." }
                                result.write(buffer, 0, size)
                            }
                            result.toByteArray()
                        }
                        BackupCodec.decode(bytes.toString(Charsets.UTF_8))
                    }.onSuccess {
                        pendingRestore = it
                    }.onFailure {
                        scope.launch { snackbars.showSnackbar("Neispravna kopija: ${it.message}") }
                    }
                }
            }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = false
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

            ShiftoraTheme(darkMode) {
                if (onboarding) {
                    OnboardingScreen(onComplete = vm::finishOnboarding)
                } else {
                    BackHandler(enabled = tab != 0 && editing == null && !rotationOpen && !salaryOpen && pendingRestore == null) {
                        tab = 0
                    }
                    val sections = listOf(
                        "Početna" to Icons.Default.Home,
                        "Kalendar" to Icons.Default.CalendarMonth,
                        "Statistika" to Icons.Default.BarChart,
                        "Plaća" to Icons.Default.AccountBalanceWallet,
                        "Više" to Icons.Default.MoreHoriz
                    )
                    Scaffold(
                        snackbarHost = { SnackbarHost(snackbars) },
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = androidx.compose.ui.unit.Dp.Unspecified
                            ) {
                                sections.forEachIndexed { index, option ->
                                    NavigationBarItem(
                                        selected = tab == index,
                                        onClick = { tab = index },
                                        icon = { Icon(option.second, contentDescription = option.first) },
                                        label = { Text(option.first, maxLines = 1) },
                                        alwaysShowLabel = true,
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = Palette.Blue,
                                            selectedTextColor = Palette.Blue,
                                            indicatorColor = Palette.BlueLight
                                        )
                                    )
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
                                editing = shifts.firstOrNull { it.date == date && it.kind != ShiftKind.OFF }
                                    ?.copy(actual = true)
                                    ?: Shift(date = date, kind = ShiftKind.MORNING, actual = true)
                            }
                            val goSettings: () -> Unit = { tab = 4 }
                            when (tab) {
                                0 -> HomeScreen(shifts, salary, add, edit, track, { tab = it }, goSettings)
                                1 -> CalendarScreen(shifts, add, edit, goSettings)
                                2 -> StatsScreen(shifts, salary, goSettings)
                                3 -> SalaryScreen(shifts, salary, { salaryOpen = true }, goSettings)
                                else -> MoreScreen(
                                    darkMode = darkMode, onDark = vm::setDarkMode,
                                    onRotation = { rotationOpen = true },
                                    onCsv = { exportLauncher.launch("Shiftora-smjene-${LocalDate.now()}.csv") },
                                    onBackup = { backupLauncher.launch("Shiftora-kopija-${LocalDate.now()}.json") },
                                    onRestore = { restoreLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                                    onEditSalary = { salaryOpen = true }
                                )
                            }
                        }
                    }
                    pendingRestore?.let { backup ->
                        AlertDialog(
                            onDismissRequest = { pendingRestore = null },
                            title = { Text("Vratiti sigurnosnu kopiju?") },
                            text = {
                                Text("Kopija sadrži ${backup.shifts.size} smjena. Postojeće smjene i postavke satnice bit će ZAMIJENJENE. Ovo se ne može poništiti. Želiš li nastaviti?")
                            },
                            confirmButton = {
                                Button(onClick = {
                                    vm.restoreBackup(backup)
                                    pendingRestore = null
                                }) { Text("Zamijeni podatke") }
                            },
                            dismissButton = {
                                TextButton(onClick = { pendingRestore = null }) { Text("Odustani") }
                            }
                        )
                    }
                    editing?.let { item ->
                        ShiftEditorDialog(
                            entry = item,
                            onDismiss = { editing = null },
                            onSave = { vm.save(it) },
                            onDelete = { vm.delete(it) }
                        )
                    }
                    if (rotationOpen) RotationDialog(
                        onDismiss = { rotationOpen = false },
                        onGenerate = { start, pattern, days -> vm.rotation(start, pattern, days) }
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
