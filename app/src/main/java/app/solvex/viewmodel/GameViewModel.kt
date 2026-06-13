package app.solvex.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.solvex.data.LevelRepository
import app.solvex.logic.GameValidator
import app.solvex.logic.PuzzleGenerator
import app.solvex.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

const val TOTAL_LEVELS = 820

data class UserStats(
    val currentStreak: Int = 0,
    val maxStreak: Int = 0,
    val totalCompleted: Int = 0,
    val totalLevels: Int = 0,
    val dailyCount: Int = 0,
    val dailyTime: Int = 0,
    val totalDays: Int = 0
) {
    val winPercent: Int get() = if (totalDays > 0) dailyCount * 100 / totalDays else 0
}

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = app.getSharedPreferences("elementflow", Context.MODE_PRIVATE)

    private val _levelCount = MutableStateFlow(TOTAL_LEVELS)
    val levelCount: StateFlow<Int> = _levelCount.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _screen = MutableStateFlow(AppScreen.HOME)
    val screen: StateFlow<AppScreen> = _screen.asStateFlow()

    private val _gameState = MutableStateFlow<GameState?>(null)
    val gameState: StateFlow<GameState?> = _gameState.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound", true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("haptics", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _stats = MutableStateFlow(UserStats())
    val stats: StateFlow<UserStats> = _stats.asStateFlow()

    private var currentLevelIndex: Int = 0
    private var isDaily: Boolean = false
    private var timerJob: Job? = null
    private var errorJob: Job? = null

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = LevelRepository.load(app)
            if (loaded.isNotEmpty()) _levelCount.value = loaded.size
            _isLoading.value = false
            _stats.value = buildStats()
        }
    }

    fun navigate(screen: AppScreen) { _screen.value = screen }

    // Play → level 1 on June 12 2026, level 2 on June 13, etc.
    @SuppressLint("NewApi")
    fun resumePlay() {
        isDaily = true
        val allLevels = LevelRepository.getAll()
        val today = LocalDate.now().toEpochDay()
        val dayOffset = (today - 20616L).coerceAtLeast(0L)
        if (allLevels.isNotEmpty()) {
            val index = (dayOffset % allLevels.size).toInt()
            val rawLevel = allLevels[index]
            val level = rawLevel.copy(isDaily = true, levelNumber = index + 1)
            val todayAlreadyDone = prefs.getLong("streak_last_date", -1L) == today
            if (todayAlreadyDone) {
                val completedBoard = loadCompletedBoard(rawLevel.id, rawLevel.size)
                val time = prefs.getInt("daily_last_time", 0)
                val hints = prefs.getInt("daily_last_hints", 0)
                _gameState.value = GameState(
                    level = level,
                    board = completedBoard ?: rawLevel.clues.map { it.toList() },
                    isComplete = true,
                    wasAlreadyComplete = true,
                    elapsedSeconds = time,
                    hintsUsed = hints
                )
                _screen.value = AppScreen.VICTORY
            } else {
                loadLevel(level)
            }
        } else {
            loadLevel(
                PuzzleGenerator.daily(today).copy(
                    isDaily = true,
                    levelNumber = (dayOffset % 820 + 1).toInt()
                )
            )
        }
    }

    fun startLevel(levelIndex: Int) {
        currentLevelIndex = levelIndex
        isDaily = false
        val allLevels = LevelRepository.getAll()
        val level = if (allLevels.isNotEmpty() && levelIndex < allLevels.size) {
            allLevels[levelIndex]
        } else {
            PuzzleGenerator.generate(levelIndex, 9001L + levelIndex * 7919L)
        }
        loadLevel(level)
    }

    private fun loadLevel(level: GameLevel) {
        val saved = loadBoardState(level.id, level.size)
        _gameState.value = GameState(
            level = level,
            board = saved?.first ?: level.clues.map { it.toList() },
            elapsedSeconds = saved?.second ?: 0
        )
        startTimer()
        _screen.value = AppScreen.GAME
    }

    fun tapCell(row: Int, col: Int) {
        val state = _gameState.value ?: return
        if (state.isComplete) return  // board is locked when complete
        val level = state.level
        if (level.clues[row][col] != CellElement.EMPTY) return
        val next = GameValidator.nextElement(state.board[row][col])
        val newBoard = state.board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, el -> if (r == row && c == col) next else el }
        }
        val complete = GameValidator.isComplete(newBoard, level)
        // Clear any pending error highlight and update board without errors immediately
        errorJob?.cancel()
        _gameState.value = state.copy(
            board = newBoard,
            history = state.history + listOf(state.board),
            errorCells = emptySet(),
            isComplete = complete
        )
        if (!complete) {
            saveBoardState(level.id, newBoard, state.elapsedSeconds)
            // Show errors after 2-second delay so user can self-correct
            errorJob = viewModelScope.launch {
                delay(2000L)
                val cur = _gameState.value ?: return@launch
                if (!cur.isComplete) {
                    _gameState.value = cur.copy(
                        errorCells = GameValidator.getErrors(cur.board, cur.level)
                    )
                }
            }
        }
        if (_hapticsEnabled.value) vibrate(if (complete) 60L else 20L)
        if (complete) {
            timerJob?.cancel()
            errorJob?.cancel()
            saveCompletedBoard(level.id, newBoard)
            clearBoardState(level.id)
            saveCompleted(level.id, state.elapsedSeconds, state.hintsUsed)
            viewModelScope.launch {
                delay(400)
                _screen.value = AppScreen.VICTORY
            }
        }
    }

    fun undo() {
        val state = _gameState.value ?: return
        val prev = state.history.lastOrNull() ?: return
        _gameState.value = state.copy(
            board = prev,
            history = state.history.dropLast(1),
            errorCells = GameValidator.getErrors(prev, state.level)
        )
    }

    fun restart() {
        val state = _gameState.value ?: return
        errorJob?.cancel()
        clearBoardState(state.level.id)
        _gameState.value = state.copy(
            board = state.level.clues.map { it.toList() },
            history = emptyList(),
            errorCells = emptySet(),
            isComplete = false,
            wasAlreadyComplete = false,
            elapsedSeconds = 0
        )
        startTimer()
    }

    fun useHint() {
        val state = _gameState.value ?: return
        val empties = (0 until state.level.size).flatMap { r ->
            (0 until state.level.size).map { c -> r to c }
        }.filter { (r, c) -> state.board[r][c] == CellElement.EMPTY }
        if (empties.isEmpty()) return
        val (hr, hc) = empties.random()
        val hintBoard = state.board.mapIndexed { r, row ->
            row.mapIndexed { c, el ->
                if (r == hr && c == hc) state.level.solution[r][c] else el
            }
        }
        _gameState.value = state.copy(
            board = hintBoard,
            hintsUsed = state.hintsUsed + 1,
            errorCells = GameValidator.getErrors(hintBoard, state.level)
        )
    }

    fun isLevelCompleted(levelIndex: Int): Boolean {
        val allLevels = LevelRepository.getAll()
        val key = if (allLevels.isNotEmpty() && levelIndex < allLevels.size)
            allLevels[levelIndex].id.toString()
        else
            (9001L + levelIndex * 7919L).toString()
        return prefs.getStringSet("completed", emptySet())?.contains(key) == true
    }

    fun toggleDarkMode() {
        _darkMode.value = !_darkMode.value
        prefs.edit().putBoolean("dark_mode", _darkMode.value).apply()
    }
    fun toggleSound() {
        _soundEnabled.value = !_soundEnabled.value
        prefs.edit().putBoolean("sound", _soundEnabled.value).apply()
    }
    fun toggleHaptics() {
        _hapticsEnabled.value = !_hapticsEnabled.value
        prefs.edit().putBoolean("haptics", _hapticsEnabled.value).apply()
    }

    override fun onCleared() {
        super.onCleared()
        errorJob?.cancel()
        val state = _gameState.value ?: return
        if (!state.isComplete) saveBoardState(state.level.id, state.board, state.elapsedSeconds)
    }

    private fun saveBoardState(levelId: Int, board: List<List<CellElement>>, elapsedSeconds: Int) {
        val flat = board.flatten().joinToString(",") { it.name }
        prefs.edit()
            .putString("board_$levelId", flat)
            .putInt("time_$levelId", elapsedSeconds)
            .apply()
    }

    private fun loadBoardState(levelId: Int, size: Int): Pair<List<List<CellElement>>, Int>? {
        val flat = prefs.getString("board_$levelId", null) ?: return null
        val elements = flat.split(",").map {
            try { CellElement.valueOf(it) } catch (_: Exception) { CellElement.EMPTY }
        }
        if (elements.size != size * size) return null
        return elements.chunked(size) to prefs.getInt("time_$levelId", 0)
    }

    private fun clearBoardState(levelId: Int) {
        prefs.edit().remove("board_$levelId").remove("time_$levelId").apply()
    }

    private fun saveCompletedBoard(levelId: Int, board: List<List<CellElement>>) {
        val flat = board.flatten().joinToString(",") { it.name }
        prefs.edit().putString("done_board_$levelId", flat).apply()
    }

    private fun loadCompletedBoard(levelId: Int, size: Int): List<List<CellElement>>? {
        val flat = prefs.getString("done_board_$levelId", null) ?: return null
        val elements = flat.split(",").map {
            try { CellElement.valueOf(it) } catch (_: Exception) { CellElement.EMPTY }
        }
        if (elements.size != size * size) return null
        return elements.chunked(size)
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val s = _gameState.value ?: break
                if (s.isComplete) break
                _gameState.value = s.copy(elapsedSeconds = s.elapsedSeconds + 1)
            }
        }
    }

    private fun saveCompleted(levelId: Int, elapsedSeconds: Int = 0, hintsUsed: Int = 0) {
        val set = prefs.getStringSet("completed", emptySet())!!.toMutableSet()
        set.add(levelId.toString())
        prefs.edit().putStringSet("completed", set).apply()
        if (isDaily) {
            updateDailyStreak()
            prefs.edit()
                .putInt("daily_last_time", elapsedSeconds)
                .putInt("daily_last_hints", hintsUsed)
                .apply()
        }
        _stats.value = buildStats()
    }

    @SuppressLint("NewApi")
    private fun updateDailyStreak() {
        val today = LocalDate.now().toEpochDay()
        val lastDay = prefs.getLong("streak_last_date", -1L)
        val current = prefs.getInt("streak_current", 0)
        if (lastDay == today) return // already counted today
        val newStreak = if (lastDay == today - 1) current + 1 else 1
        val newMax = maxOf(prefs.getInt("streak_max", 0), newStreak)
        prefs.edit()
            .putLong("streak_last_date", today)
            .putInt("streak_current", newStreak)
            .putInt("streak_max", newMax)
            .putInt("daily_count", prefs.getInt("daily_count", 0) + 1)
            .apply()
    }

    @SuppressLint("NewApi")
    private fun buildStats(): UserStats {
        val completed = prefs.getStringSet("completed", emptySet())!!.size
        val today = LocalDate.now().toEpochDay()
        val totalDays = (today - 20616L).coerceAtLeast(0L).toInt() + 1
        return UserStats(
            currentStreak = prefs.getInt("streak_current", 0),
            maxStreak = prefs.getInt("streak_max", 0),
            totalCompleted = completed,
            totalLevels = _levelCount.value,
            dailyCount = prefs.getInt("daily_count", 0),
            dailyTime = prefs.getInt("daily_last_time", 0),
            totalDays = totalDays
        )
    }

    @SuppressLint("NewApi")
    private fun vibrate(ms: Long) {
        try {
            val ctx = getApplication<Application>()
            val v = if (android.os.Build.VERSION.SDK_INT >= 31)
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
            else @Suppress("DEPRECATION") ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }
}
