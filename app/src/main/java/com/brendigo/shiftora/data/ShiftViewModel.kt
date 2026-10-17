package com.brendigo.shiftora.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class ShiftViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ShiftStore(application)
    private val preferences = Preferences(application)
    private val _shifts = MutableStateFlow<List<Shift>>(emptyList())
    val shifts: StateFlow<List<Shift>> = _shifts
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

    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        _shifts.value = store.all()
    }

    fun save(shift: Shift) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { store.save(shift) }.onSuccess {
            _shifts.value = store.all()
            _message.value = "Smjena je spremljena."
        }.onFailure { _message.value = "Spremanje nije uspjelo: ${it.message}" }
    }

    fun delete(id: Long) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { store.delete(id) }.onSuccess {
            _shifts.value = store.all()
            _message.value = "Smjena je obrisana."
        }.onFailure { _message.value = "Brisanje nije uspjelo: ${it.message}" }
    }

    fun rotation(start: LocalDate, pattern: List<ShiftKind>, days: Int) =
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { store.addRotation(start, pattern, days) }.onSuccess { count ->
                _shifts.value = store.all()
                _message.value = "Dodano $count dana. Postojeći unosi nisu promijenjeni."
            }.onFailure { _message.value = "Raspored nije izrađen: ${it.message}" }
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

    fun exportBackup(): String = BackupCodec.encode(shifts.value, salary.value)

    /** Import only after explicit preview and confirmation by the user. */
    fun restoreBackup(backup: ShiftBackup) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            store.replaceAll(backup.shifts)
            preferences.saveSalary(backup.salary)
            _shifts.value = store.all()
            _salary.value = backup.salary
        }.onSuccess {
            _message.value = "Sigurnosna kopija vraćena: ${backup.shifts.size} smjena."
        }.onFailure {
            _message.value = "Vraćanje nije uspjelo: ${it.message}"
        }
    }

    override fun onCleared() {
        store.close()
        super.onCleared()
    }
}
