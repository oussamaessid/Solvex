package app.solvex.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.media.AudioManager
import android.media.ToneGenerator
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
    val totalDays: Int = 0,
    val currentLevel: Int = 1
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

    private val _showBrokenHeart = MutableStateFlow(false)
    val showBrokenHeart: StateFlow<Boolean> = _showBrokenHeart.asStateFlow()

    private val _darkMode = MutableStateFlow(prefs.getBoolean("dark_mode", false))
    val darkMode: StateFlow<Boolean> = _darkMode.asStateFlow()

    private val _soundEnabled = MutableStateFlow(prefs.getBoolean("sound", true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()

    private val _showTutorial = MutableStateFlow(!prefs.getBoolean("tutorial_seen", false))
    val showTutorial: StateFlow<Boolean> = _showTutorial.asStateFlow()

    private val toneGenerator = runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 45) }.getOrNull()

    private val _hapticsEnabled = MutableStateFlow(prefs.getBoolean("haptics", true))
    val hapticsEnabled: StateFlow<Boolean> = _hapticsEnabled.asStateFlow()

    private val _stats = MutableStateFlow(UserStats())
    val stats: StateFlow<UserStats> = _stats.asStateFlow()

    private val _coins = MutableStateFlow(prefs.getInt("coins", STARTING_COINS))
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _hints = MutableStateFlow(prefs.getInt("hints", DEFAULT_HINTS))
    val hints: StateFlow<Int> = _hints.asStateFlow()

    private var currentLevelIndex: Int = 0
    private var isDaily: Boolean = false
    private var timerJob: Job? = null
    private var errorJob: Job? = null
    private var shopReturnScreen: AppScreen = AppScreen.HOME

    init {
        // One-time migration from the former five-hint/shared-lives economy.
        if (prefs.getInt("economy_version", 0) < 1) {
            prefs.edit()
                .putInt("economy_version", 1)
                .putInt("coins", STARTING_COINS)
                .putInt("hints", DEFAULT_HINTS)
                .apply()
            _coins.value = STARTING_COINS
            _hints.value = DEFAULT_HINTS
        }
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = LevelRepository.load(app)
            if (loaded.isNotEmpty()) _levelCount.value = loaded.size
            _isLoading.value = false
            _stats.value = buildStats()
        }
    }

    fun navigate(screen: AppScreen) { _screen.value = screen }

    fun openShop(returnTo: AppScreen = _screen.value) {
        shopReturnScreen = if (returnTo == AppScreen.SHOP) AppScreen.HOME else returnTo
        if (shopReturnScreen == AppScreen.GAME) timerJob?.cancel()
        _screen.value = AppScreen.SHOP
    }

    fun closeShop() {
        _screen.value = shopReturnScreen
        if (shopReturnScreen == AppScreen.GAME) startTimer()
    }

    // Play → always resumes from the furthest level reached; finishing a level
    // immediately unlocks the next one (no daily/one-per-day gating).
    fun resumePlay() {
        isDaily = true
        val allLevels = LevelRepository.getAll()
        val progressIndex = prefs.getInt("progress_index", 0)
        if (allLevels.isNotEmpty()) {
            val index = progressIndex.coerceIn(0, allLevels.size - 1)
            currentLevelIndex = index
            val level = allLevels[index].copy(isDaily = true, levelNumber = index + 1)
            loadLevel(level)
        } else {
            val index = progressIndex.coerceIn(0, TOTAL_LEVELS - 1)
            currentLevelIndex = index
            loadLevel(
                PuzzleGenerator.generate(index, 9001L + index * 7919L).copy(
                    isDaily = true,
                    levelNumber = index + 1
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

    fun nextLevel() {
        val allLevels = LevelRepository.getAll()
        val total = if (allLevels.isNotEmpty()) allLevels.size else TOTAL_LEVELS
        val next = (currentLevelIndex + 1).coerceAtMost(total - 1)
        startLevel(next)
    }

    private fun loadLevel(level: GameLevel) {
        val saved = loadBoardState(level.id, level.size)
        // Every puzzle owns its own three-life pool. A mistake in one level no
        // longer penalises the player in every other level.
        val lives = prefs.getInt("lives_${level.id}", MAX_LIVES).coerceIn(0, MAX_LIVES)
        _gameState.value = GameState(
            level = level,
            board = saved?.first ?: level.clues.map { it.toList() },
            elapsedSeconds = saved?.second ?: 0,
            lives = lives,
            hints = _hints.value
        )
        startTimer()
        _screen.value = AppScreen.GAME
    }

    fun tapCell(row: Int, col: Int) {
        val state = _gameState.value ?: return
        if (state.isComplete) return  // board is locked when complete
        if (state.lives <= 0) return  // board is locked when out of lives
        if (_showBrokenHeart.value) return  // board is locked while the mistake animation plays
        val level = state.level
        if (level.clues[row][col] != CellElement.EMPTY) return
        val next = GameValidator.nextElement(state.board[row][col])
        playTone(
            when (next) {
                CellElement.FIRE -> ToneGenerator.TONE_PROP_BEEP
                CellElement.WATER -> ToneGenerator.TONE_PROP_BEEP2
                CellElement.EMPTY -> ToneGenerator.TONE_PROP_NACK
            },
            if (next == CellElement.EMPTY) 45 else 70
        )
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
            // Show errors after 2-second delay so user can self-correct; a confirmed
            // mistake at that point costs one life.
            errorJob = viewModelScope.launch {
                delay(2000L)
                val cur = _gameState.value ?: return@launch
                if (cur.isComplete) return@launch
                val newErrors = GameValidator.getErrors(cur.board, cur.level)
                if (newErrors.isNotEmpty() && cur.errorCells.isEmpty()) {
                    playTone(ToneGenerator.TONE_SUP_ERROR, 180)
                    val newLives = (cur.lives - 1).coerceAtLeast(0)
                    saveLevelLives(cur.level.id, newLives)
                    _gameState.value = cur.copy(errorCells = newErrors, lives = newLives)
                    _showBrokenHeart.value = true
                } else {
                    _gameState.value = cur.copy(errorCells = newErrors)
                }
            }
        }
        if (_hapticsEnabled.value) vibrate(if (complete) 60L else 20L)
        if (complete) {
            playTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 250)
            timerJob?.cancel()
            errorJob?.cancel()
            saveCompletedBoard(level.id, newBoard)
            clearBoardState(level.id)
            val coinsEarned = saveCompleted(level.id, state.elapsedSeconds, state.hintsUsed)
            _gameState.value = _gameState.value?.copy(coinsEarned = coinsEarned)
            advanceProgressIfNeeded()
            viewModelScope.launch {
                delay(400)
                _screen.value = AppScreen.VICTORY
            }
        }
    }

    /** Double-tapping an editable cell places Water directly. */
    fun doubleTapCell(row: Int, col: Int) {
        val state = _gameState.value ?: return
        if (state.isComplete || state.lives <= 0 || _showBrokenHeart.value) return
        if (state.level.clues[row][col] != CellElement.EMPTY) return
        when (state.board[row][col]) {
            CellElement.EMPTY -> {
                tapCell(row, col)
                tapCell(row, col)
            }
            CellElement.FIRE -> tapCell(row, col)
            CellElement.WATER -> Unit
        }
    }

    // Called once the broken-heart animation finishes playing: wipes only the
    // fire/water cell(s) that don't match the solution, not the whole
    // row/column that got flagged by the balance/constraint checks.
    fun onErrorAnimationEnd() {
        _showBrokenHeart.value = false
        val state = _gameState.value ?: return
        if (state.errorCells.isEmpty()) return
        val solution = state.level.solution
        val clearedBoard = state.board.mapIndexed { r, rowList ->
            rowList.mapIndexed { c, el ->
                if ((r to c) in state.errorCells && el != solution[r][c]) CellElement.EMPTY else el
            }
        }
        saveBoardState(state.level.id, clearedBoard, state.elapsedSeconds)
        _gameState.value = state.copy(board = clearedBoard, errorCells = emptySet())
    }

    private fun advanceProgressIfNeeded() {
        val allLevels = LevelRepository.getAll()
        val total = if (allLevels.isNotEmpty()) allLevels.size else TOTAL_LEVELS
        val idx = prefs.getInt("progress_index", 0)
        if (currentLevelIndex >= idx && idx < total - 1) {
            prefs.edit().putInt("progress_index", currentLevelIndex + 1).apply()
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
        // Restarting clears the board but keeps this level's remaining lives.
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
        if (state.hints <= 0) return
        val hintBoard = revealHintCell(state) ?: return
        playTone(ToneGenerator.TONE_PROP_ACK, 120)
        val newHints = state.hints - 1
        saveHints(newHints)
        _gameState.value = state.copy(
            board = hintBoard,
            hints = newHints,
            hintsUsed = state.hintsUsed + 1,
            errorCells = GameValidator.getErrors(hintBoard, state.level)
        )
    }

    // Called after the user watches a rewarded ad while out of hints — adds one
    // to the counter rather than revealing a cell directly.
    fun addHint() {
        val newHints = _hints.value + 1
        saveHints(newHints)
        _gameState.value?.let { state ->
            _gameState.value = state.copy(hints = newHints)
        }
    }

    /** Shop-safe free hint (works even with no active game, e.g. from HOME shop). */
    fun addHintShop() {
        addHint()
    }

    // Called after the user watches a rewarded ad while out of lives.
    fun addLife() {
        val state = _gameState.value ?: return
        val newLives = (state.lives + 1).coerceAtMost(MAX_LIVES)
        saveLevelLives(state.level.id, newLives)
        _gameState.value = state.copy(lives = newLives)
    }

    /** Atomically exchanges coins for hints. Returns false when funds are insufficient. */
    fun buyHints(amount: Int, price: Int): Boolean {
        if (amount <= 0 || price < 0 || _coins.value < price) return false
        val newCoins = _coins.value - price
        val newHints = _hints.value + amount
        prefs.edit().putInt("coins", newCoins).putInt("hints", newHints).apply()
        _coins.value = newCoins
        _hints.value = newHints
        _gameState.value = _gameState.value?.copy(hints = newHints)
        playTone(ToneGenerator.TONE_PROP_ACK, 160)
        return true
    }

    private fun revealHintCell(state: GameState): List<List<CellElement>>? {
        val empties = (0 until state.level.size).flatMap { r ->
            (0 until state.level.size).map { c -> r to c }
        }.filter { (r, c) -> state.board[r][c] == CellElement.EMPTY }
        if (empties.isEmpty()) return null
        val (hr, hc) = empties.random()
        return state.board.mapIndexed { r, row ->
            row.mapIndexed { c, el ->
                if (r == hr && c == hc) state.level.solution[r][c] else el
            }
        }
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

    fun openTutorial() { _showTutorial.value = true }

    fun finishTutorial() {
        prefs.edit().putBoolean("tutorial_seen", true).apply()
        _showTutorial.value = false
    }

    private fun playTone(tone: Int, durationMs: Int) {
        if (_soundEnabled.value) toneGenerator?.startTone(tone, durationMs)
    }
    fun toggleHaptics() {
        _hapticsEnabled.value = !_hapticsEnabled.value
        prefs.edit().putBoolean("haptics", _hapticsEnabled.value).apply()
    }

    override fun onCleared() {
        super.onCleared()
        toneGenerator?.release()
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

    private fun saveLevelLives(levelId: Int, lives: Int) {
        prefs.edit().putInt("lives_$levelId", lives.coerceIn(0, MAX_LIVES)).apply()
    }

    private fun saveHints(value: Int) {
        prefs.edit().putInt("hints", value).apply()
        _hints.value = value
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

    private fun saveCompleted(levelId: Int, elapsedSeconds: Int = 0, hintsUsed: Int = 0): Int {
        val set = prefs.getStringSet("completed", emptySet())!!.toMutableSet()
        val firstCompletion = set.add(levelId.toString())
        prefs.edit().putStringSet("completed", set).apply()
        val reward = if (firstCompletion) LEVEL_COIN_REWARD else 0
        if (reward > 0) {
            _coins.value += reward
            prefs.edit().putInt("coins", _coins.value).apply()
        }
        if (isDaily) {
            updateDailyStreak()
            prefs.edit()
                .putInt("daily_last_time", elapsedSeconds)
                .putInt("daily_last_hints", hintsUsed)
                .apply()
        }
        _stats.value = buildStats()
        return reward
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
            totalDays = totalDays,
            currentLevel = prefs.getInt("progress_index", 0) + 1
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
