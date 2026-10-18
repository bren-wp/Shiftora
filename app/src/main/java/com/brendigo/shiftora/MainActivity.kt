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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
            var tab by rememberSaveable { mutableIntStateOf(0) }
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
                            context.contentResolver.openOutputStream(uri)
                                ?.bufferedWriter(Charsets.UTF_8)?.use { it.write(vm.exportCsv()) }
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
                            context.contentResolver.openOutputStream(uri)
                                ?.bufferedWriter(Charsets.UTF_8)?.use { it.write(vm.exportBackup()) }
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
                                    require(total <= BackupCodec.MAX_BYTES) { "Datoteka je veća od 4 MB." }
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

            ShiftoraTheme(darkMode, shiftColors) {
                if (onboarding) {
                    OnboardingScreen(onComplete = vm::finishOnboarding)
                } else {
                    BackHandler(enabled = tab != 0 && editing == null && !rotationOpen && !salaryOpen && !colorsOpen && !archiveOpen && pendingRestore == null) {
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
                                editing = shifts.firstOrNull { it.date == date && it.kind.isWork }
                                    ?.copy(actual = true)
                                    ?: Shift(date = date, kind = ShiftKind.MORNING, actual = true)
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
