package com.brendigo.shiftora

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import com.brendigo.shiftora.data.ShiftKind
import com.brendigo.shiftora.data.ShiftViewModel
import com.brendigo.shiftora.ui.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
            var tab by remember { mutableIntStateOf(0) }
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
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !darkMode && tab == 0 && !onboarding
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
                    val sections = listOf(
                        Triple("Početna", Icons.Default.Home, Icons.Default.Home),
                        Triple("Kalendar", Icons.Default.CalendarMonth, Icons.Default.CalendarMonth),
                        Triple("Statistika", Icons.Default.BarChart, Icons.Default.BarChart),
                        Triple("Plaća", Icons.Default.AccountBalanceWallet, Icons.Default.AccountBalanceWallet),
                        Triple("Više", Icons.Default.MoreHoriz, Icons.Default.MoreHoriz)
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
                            val goSettings: () -> Unit = { tab = 4 }
                            when (tab) {
                                0 -> HomeScreen(shifts, salary, add, edit, { tab = it }, goSettings)
                                1 -> CalendarScreen(shifts, add, edit, { rotationOpen = true }, goSettings)
                                2 -> StatsScreen(shifts, salary, goSettings)
                                3 -> SalaryScreen(shifts, salary, { salaryOpen = true }, goSettings)
                                else -> MoreScreen(
                                    darkMode = darkMode, onDark = vm::setDarkMode,
                                    onRotation = { rotationOpen = true },
                                    onCsv = { exportLauncher.launch("Shiftora-smjene-${LocalDate.now()}.csv") },
                                    onEditSalary = { salaryOpen = true }, onSettings = goSettings
                                )
                            }
                        }
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
