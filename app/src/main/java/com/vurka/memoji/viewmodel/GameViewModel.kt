package com.vurka.memoji.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vurka.memoji.data.LevelCatalog
import com.vurka.memoji.data.ProgressRepository
import com.vurka.memoji.data.ThemeRepository
import com.vurka.memoji.game.GameState
import com.vurka.memoji.game.MemoryGameEngine
import com.vurka.memoji.model.LevelConfig
import com.vurka.memoji.util.HapticManager
import com.vurka.memoji.util.SoundManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class GameViewModel(application: Application, val levelNumber: Int) : AndroidViewModel(application) {
    private val progressRepository = ProgressRepository(application)
    private val themeRepository = ThemeRepository(application)
    val levelConfig: LevelConfig = LevelCatalog.getLevel(levelNumber)
    private val engine = MemoryGameEngine(levelConfig)

    val selectedThemeId: StateFlow<String> = themeRepository.selectedThemeId.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = "default",
    )

    /** Human friendly name of the world this level belongs to. */
    val worldName: String = LevelCatalog.getWorldName(levelNumber)

    /** Mirrors the sound preference so the victory/loss dialogs can play their own feedback. */
    val soundEnabled: StateFlow<Boolean> = progressRepository.soundEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    private val hapticsEnabled: StateFlow<Boolean> = progressRepository.hapticsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = true,
    )

    val isZenMode: StateFlow<Boolean> = progressRepository.isZenMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = false,
    )

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private var previewJob: Job? = null
    private var timerJob: Job? = null
    private var mismatchJob: Job? = null
    private var hintJob: Job? = null
    private var cooldownJob: Job? = null
    private var autoHideJob: Job? = null

    init {
        // Single authoritative "last played" write: entering a level from Home, the map, or a
        // victory shortcut all end up here.
        viewModelScope.launch {
            progressRepository.setLastPlayedLevel(levelNumber)
        }
        startNewGame()
    }

    fun calculateStars(totalPairs: Int, timeElapsedSec: Int, mistakes: Int, timeLimitSec: Int = 0): Int =
        engine.calculateStars(totalPairs, timeElapsedSec, mistakes, timeLimitSec)

    private fun cancelAllJobs() {
        previewJob?.cancel()
        timerJob?.cancel()
        mismatchJob?.cancel()
        hintJob?.cancel()
        cooldownJob?.cancel()
        autoHideJob?.cancel()
    }

    fun startNewGame() {
        cancelAllJobs()

        val deck = engine.generateDeck()
        _state.value = GameState(
            cards = deck.map { it.copy(isFlipped = true) },
            totalPairs = levelConfig.pairCount,
            isPreviewing = true,
            timeRemainingSec = levelConfig.timeLimitSec,
            hintsLeft = if (isZenMode.value) -1 else levelConfig.maxHints,
            shuffleCountdownSec = if (isZenMode.value) 0 else levelConfig.shuffleIntervalSec,
        )

        previewJob = viewModelScope.launch {
            delay(levelConfig.previewTimeSec.seconds)
            _state.update { current ->
                if (current.isPreviewing) {
                    current.copy(
                        cards = current.cards.map { it.copy(isFlipped = false) },
                        isPreviewing = false,
                    )
                } else current
            }
            if (!_state.value.isPaused) startTimer()
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1.seconds)
                val snapshot = _state.value
                if (snapshot.isPaused || snapshot.isGameOver || snapshot.isPreviewing) break
                tick()
            }
        }
    }

    private fun tick() {
        val zen = isZenMode.value
        var lostByTime = false
        var shuffled = false

        _state.update { current ->
            lostByTime = false
            shuffled = false
            if (current.isPaused || current.isGameOver) return@update current

            val elapsed = current.timeElapsedSec + 1
            var remaining = current.timeRemainingSec
            var lost = false
            if ((!zen) && (levelConfig.timeLimitSec > 0)) {
                remaining -= 1
                if (remaining <= 0) lost = true
            }

            var next = current.copy(
                timeElapsedSec = elapsed,
                timeRemainingSec = remaining.coerceAtLeast(0),
                isGameOver = lost,
                isWon = false,
                lossReason = if (lost) "time" else current.lossReason,
            )
            lostByTime = lost

            // Reshuffle countdown (only while the player is idle, so a pick is never disturbed).
            if ((!zen) && (!lost) && (levelConfig.shuffleIntervalSec > 0)) {
                val idle = (current.firstSelectedCardId == null) &&
                    (current.secondSelectedCardId == null) &&
                    (mismatchJob?.isActive != true)
                val left = current.shuffleCountdownSec - 1
                if (left <= 0) {
                    if (idle) {
                        next = next.copy(
                            cards = engine.shuffleUnmatched(current.cards),
                            shuffleCountdownSec = levelConfig.shuffleIntervalSec,
                            shuffleCount = current.shuffleCount + 1,
                        )
                        shuffled = true
                    } else {
                        next = next.copy(shuffleCountdownSec = 1) // retry next second
                    }
                } else {
                    next = next.copy(shuffleCountdownSec = left)
                }
            }
            next
        }

        if (shuffled) {
            SoundManager.playShuffle(soundEnabled.value)
            HapticManager.triggerHint(getApplication(), hapticsEnabled.value)
        }
        if (lostByTime) {
            timerJob?.cancel()
            SoundManager.playDefeat(soundEnabled.value)
            HapticManager.triggerMismatch(getApplication(), hapticsEnabled.value)
            recordLoss()
        } else {
            // Final ten seconds: a quiet tick, sharper and higher for the last three.
            val snapshot = _state.value
            if ((!zen) && (!snapshot.isGameOver) && (!snapshot.isPaused) && (levelConfig.timeLimitSec > 0)) {
                val remaining = snapshot.timeRemainingSec
                if (remaining in 1..10) {
                    SoundManager.playTick(urgent = remaining <= 3, enabled = soundEnabled.value)
                }
            }
        }
    }

    /**
     * A finished run counts once, whether it was won or lost: the daily streak should reward the
     * days you played, and "games played" must not silently ignore defeats.
     */
    private fun recordLoss() {
        viewModelScope.launch {
            progressRepository.updateDailyStreak()
            progressRepository.updatePlayStats(isPerfect = false)
        }
    }

    fun useHint() {
        val current = _state.value
        val zen = isZenMode.value
        if (current.isPreviewing || current.isPaused || current.isGameOver || current.isHintOnCooldown) return
        if ((current.hintsLeft == 0) && (!zen)) return

        SoundManager.playHint(soundEnabled.value)
        HapticManager.triggerHint(getApplication(), hapticsEnabled.value)

        _state.update { state ->
            state.copy(
                cards = state.cards.map { if (!it.isMatched) it.copy(isFlipped = true) else it },
                isHintOnCooldown = true,
                hintCooldownRemainingSec = 10,
                hintsLeft = if (zen) -1 else if (state.hintsLeft > 0) state.hintsLeft - 1 else state.hintsLeft,
            )
        }

        hintJob?.cancel()
        hintJob = viewModelScope.launch {
            // Higher levels show the board for a shorter time.
            val showMs = if (levelConfig.levelNumber > 300) 1500L else 2200L
            delay(showMs.milliseconds)
            _state.update { state ->
                state.copy(
                    cards = state.cards.map {
                        if ((!it.isMatched) && (it.id != state.firstSelectedCardId) && (it.id != state.secondSelectedCardId)) {
                            it.copy(isFlipped = false)
                        } else it
                    }
                )
            }
        }

        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (sec in 10 downTo 1) {
                delay(1.seconds)
                _state.update { it.copy(hintCooldownRemainingSec = sec - 1) }
            }
            _state.update { it.copy(isHintOnCooldown = false) }
        }
    }

    fun onCardClicked(cardId: Int) {
        val current = _state.value
        if (current.isPreviewing || current.isPaused || current.isGameOver || (mismatchJob?.isActive == true)) return

        val clickedCard = current.cards.find { it.id == cardId } ?: return
        if (clickedCard.isFlipped || clickedCard.isMatched) return

        SoundManager.playFlip(soundEnabled.value)
        HapticManager.triggerFlip(getApplication(), hapticsEnabled.value)

        val isAnimal = clickedCard.emoji in com.vurka.memoji.data.EmojiCatalog.getByCategory("Animals").map { it.emoji }
        viewModelScope.launch {
            progressRepository.incrementCardFlips(1, isAnimal)
        }

        val updatedCards = current.cards.map { if (it.id == cardId) it.copy(isFlipped = true) else it }

        // ---- First card of a pair ----
        if (current.firstSelectedCardId == null) {
            _state.value = current.copy(cards = updatedCards, firstSelectedCardId = cardId)
            scheduleAutoHide(cardId)
            return
        }
        if ((current.secondSelectedCardId != null) || (current.firstSelectedCardId == cardId)) return

        // ---- Second card of a pair ----
        autoHideJob?.cancel()
        val firstCard = updatedCards.find { it.id == current.firstSelectedCardId } ?: return
        val newMoves = current.moves + 1
        val zen = isZenMode.value

        if (firstCard.emojiId == clickedCard.emojiId) {
            val matchedCards = updatedCards.map {
                if ((it.id == cardId) || (it.id == firstCard.id)) it.copy(isMatched = true) else it
            }
            val newMatchedPairs = current.matchedPairsCount + 1
            val newCombo = current.combo + 1
            val newMaxCombo = maxOf(current.maxCombo, newCombo)
            val won = newMatchedPairs == current.totalPairs

            if (won) {
                // The final pair gets the fanfare only - playing the match chime first made the two
                // cues collide into noise.
                SoundManager.playVictory(soundEnabled.value)
                HapticManager.triggerVictory(getApplication(), hapticsEnabled.value)

                cancelAllJobs()

                val earnedStars = calculateStars(
                    totalPairs = current.totalPairs,
                    timeElapsedSec = current.timeElapsedSec,
                    mistakes = current.mistakes,
                    timeLimitSec = levelConfig.timeLimitSec
                )

                _state.value = current.copy(
                    cards = matchedCards,
                    firstSelectedCardId = null,
                    secondSelectedCardId = null,
                    moves = newMoves,
                    matchedPairsCount = newMatchedPairs,
                    combo = newCombo,
                    maxCombo = newMaxCombo,
                    isGameOver = true,
                    isWon = true,
                    stars = earnedStars
                )

                // Exactly one write per victory. The dialog appears immediately and the reward
                // arrives as a state update a moment later.
                viewModelScope.launch {
                    val result = progressRepository.recordLevelResult(
                        level = levelNumber,
                        stars = earnedStars,
                        moves = newMoves,
                        timeSec = current.timeElapsedSec,
                    )
                    progressRepository.updateDailyStreak()
                    progressRepository.updatePlayStats(current.mistakes == 0)
                    _state.update {
                        it.copy(
                            coinsAwarded = result.coinsAwarded,
                            isFirstClear = result.isFirstClear,
                            isPersonalBest = result.isPersonalBest,
                        )
                    }
                }
                return
            }

            // The match chime climbs the pentatonic ladder with the combo, so long chains sound
            // like a melody instead of the same beep repeated.
            SoundManager.playMatch(newCombo, soundEnabled.value)
            HapticManager.triggerMatch(getApplication(), hapticsEnabled.value)

            _state.value = current.copy(
                cards = matchedCards,
                firstSelectedCardId = null,
                secondSelectedCardId = null,
                moves = newMoves,
                matchedPairsCount = newMatchedPairs,
                combo = newCombo,
                maxCombo = newMaxCombo
            )
        } else {
            SoundManager.playMismatch(soundEnabled.value)
            HapticManager.triggerMismatch(getApplication(), hapticsEnabled.value)

            val firstId = current.firstSelectedCardId
            _state.value = current.copy(
                cards = updatedCards,
                secondSelectedCardId = cardId,
                moves = newMoves,
                mistakes = current.mistakes + 1,
                combo = 0
            )

            mismatchJob = viewModelScope.launch {
                delay(levelConfig.mismatchDelayMs.milliseconds)
                _state.update { state ->
                    state.copy(
                        cards = state.cards.map {
                            if ((it.id == firstId) || (it.id == cardId)) it.copy(isFlipped = false) else it
                        },
                        firstSelectedCardId = null,
                        secondSelectedCardId = null
                    )
                }
            }
        }

        // Move limit (not in Zen mode).
        if ((!zen) && (levelConfig.moveLimit > 0) && (newMoves >= levelConfig.moveLimit) && (!_state.value.isWon)) {
            cancelAllJobs()
            SoundManager.playDefeat(soundEnabled.value)
            HapticManager.triggerMismatch(getApplication(), hapticsEnabled.value)
            _state.update { it.copy(isGameOver = true, isWon = false, lossReason = "moves") }
            recordLoss()
        }
    }

    /** High levels: a lone face-up card flips back by itself, so the player must be quick. */
    private fun scheduleAutoHide(cardId: Int) {
        autoHideJob?.cancel()
        if ((levelConfig.autoHideMs <= 0) || (isZenMode.value)) return
        autoHideJob = viewModelScope.launch {
            delay(levelConfig.autoHideMs.milliseconds)
            _state.update { state ->
                if (state.firstSelectedCardId == cardId && state.secondSelectedCardId == null && !state.isPaused && !state.isGameOver) {
                    state.copy(
                        cards = state.cards.map { if (it.id == cardId) it.copy(isFlipped = false) else it },
                        firstSelectedCardId = null
                    )
                } else state
            }
        }
    }

    fun togglePause() {
        if (_state.value.isGameOver) return
        setPaused(!_state.value.isPaused)
    }

    /** Called when the app goes to the background, so the clock never runs while away. */
    fun pauseIfRunning() {
        val s = _state.value
        // Pause during the memorise preview too: otherwise the preview ends while the app is in
        // the background and the countdown starts ticking unseen.
        if (!s.isPaused && !s.isGameOver) setPaused(paused = true)
    }

    private fun setPaused(paused: Boolean) {
        _state.update { it.copy(isPaused = paused) }
        SoundManager.playPause(soundEnabled.value)
        if (paused) {
            timerJob?.cancel()
            autoHideJob?.cancel()
        } else {
            val s = _state.value
            if (!s.isGameOver && !s.isPreviewing) startTimer()
            s.firstSelectedCardId?.let { if (s.secondSelectedCardId == null) scheduleAutoHide(it) }
        }
    }

    override fun onCleared() {
        cancelAllJobs()
    }
}
