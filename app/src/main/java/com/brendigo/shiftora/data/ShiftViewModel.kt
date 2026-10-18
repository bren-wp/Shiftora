package com.brendigo.shiftora.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.YearMonth

class ShiftViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ShiftStore(application)
    private val preferences = Preferences(application)
    private val scheduleMutex = Mutex()
    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading
    private val _shifts = MutableStateFlow<List<Shift>>(emptyList())
    val shifts: StateFlow<List<Shift>> = _shifts
    private val _archived = MutableStateFlow<List<ArchivedShift>>(emptyList())
    val archived: StateFlow<List<ArchivedShift>> = _archived
    private val _salary = MutableStateFlow(preferences.salarySettings())
    val salary: StateFlow<SalarySettings> = _salary
    private val _onboarding = MutableStateFlow(!preferences.onboardingDone)
    val onboarding: StateFlow<Boolean> = _onboarding
    private val _darkMode = MutableStateFlow(preferences.darkMode)
    val darkMode: StateFlow<Boolean> = _darkMode
    private val _shiftColors = MutableStateFlow(preferences.shiftColors())
    val shiftColors: StateFlow<Map<ShiftKind, Int>> = _shiftColors
    private val _monthlyFunds = MutableStateFlow(preferences.monthlyFundOverrides())
    val monthlyFunds: StateFlow<Map<String, Int>> = _monthlyFunds

    fun setShiftColor(kind: ShiftKind, argb: Int?) {
        preferences.saveShiftColor(kind, argb)
        _shiftColors.value = preferences.shiftColors()
    }

    fun setMonthlyFund(month: YearMonth, minutes: Int?) {
        preferences.saveMonthlyFund(month, minutes)
        _monthlyFunds.value = preferences.monthlyFundOverrides()
        _message.value = "Fond sati je spremljen."
    }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init { refresh() }

    private fun reload() {
        _shifts.value = store.all()
        _archived.value = store.archived()
    }

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        scheduleMutex.withLock {
            runCatching { reload() }.onFailure {
                _message.value = "Raspored nije moguće učitati: ${it.message}"
            }
            _loading.value = false
        }
    }

    fun save(shift: Shift) = viewModelScope.launch(Dispatchers.IO) {
        scheduleMutex.withLock {
            runCatching { store.save(shift) }.onSuccess {
                reload()
                _message.value = "Smjena je spremljena i trajno evidentirana."
            }.onFailure { _message.value = "Spremanje nije uspjelo: ${it.message}" }
        }
    }

    /** Move into recoverable archive, never permanently remove from SQLite. */
    fun delete(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        scheduleMutex.withLock {
            runCatching { store.delete(id) }.onSuccess {
                reload()
                _message.value = "Smjena je arhivirana. Možeš je vratiti u Više → Arhiva rasporeda."
            }.onFailure { _message.value = "Arhiviranje nije uspjelo: ${it.message}" }
        }
    }

    fun restoreArchived(archiveId: Long) = viewModelScope.launch(Dispatchers.IO) {
        scheduleMutex.withLock {
            runCatching { store.restoreArchived(archiveId) }.onSuccess {
                reload()
                _message.value = "Smjena je vraćena u kalendar."
            }.onFailure {
                _message.value = "Vraćanje nije uspjelo: ${it.message}"
            }
        }
    }

    fun rotation(start: LocalDate, pattern: List<ShiftKind>, days: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            scheduleMutex.withLock {
                runCatching { store.addRotation(start, pattern, days) }.onSuccess { count ->
                    reload()
                    _message.value = "Dodano ${count} dana. Nijedna postojeća ili arhivirana smjena nije prebrisana."
                }.onFailure { _message.value = "Raspored nije izrađen: ${it.message}" }
            }
        }

    fun updateSalary(settings: SalarySettings) {
        preferences.saveSalary(settings)
        _salary.value = settings
        _message.value = "Postavke obračuna su spremljene."
    }

    fun setDarkMode(value: Boolean) {
        preferences.darkMode = value
        _darkMode.value = value
    }

    fun finishOnboarding() {
        preferences.onboardingDone = true
        _onboarding.value = false
    }

    fun consumeMessage() { _message.value = null }

    /** User-initiated local export, no server upload. */
    fun exportCsv(): String = CsvExporter.encode(shifts.value)

    fun exportBackup(): String = BackupCodec.encode(
        shifts.value, salary.value, shiftColors.value, monthlyFunds.value
    )

    /** Merge only: existing records, colors, salary settings and monthly funds stay untouched. */
    fun restoreBackup(backup: ShiftBackup) = viewModelScope.launch(Dispatchers.IO) {
        scheduleMutex.withLock {
            runCatching { store.mergeBackup(backup.shifts) }.onSuccess { summary ->
                reload()
                _message.value = "Uvezeno ${summary.added} smjena, preskočeno ${summary.skipped}. " +
                    "Postojeći rasporedi i postavke nisu mijenjani."
            }.onFailure {
                _message.value = "Uvoz nije uspio: ${it.message}. Postojeći rasporedi su sačuvani."
            }
        }
    }

    override fun onCleared() {
        store.close()
        super.onCleared()
    }
}
