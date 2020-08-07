package dev.lucasnlm.antimine.common.level

import dev.lucasnlm.antimine.common.level.database.models.FirstOpen
import dev.lucasnlm.antimine.common.level.database.models.Save
import dev.lucasnlm.antimine.common.level.database.models.SaveStatus
import dev.lucasnlm.antimine.common.level.database.models.Stats
import dev.lucasnlm.antimine.common.level.logic.FlagAssistant
import dev.lucasnlm.antimine.common.level.logic.MinefieldCreator
import dev.lucasnlm.antimine.common.level.logic.MinefieldHandler
import dev.lucasnlm.antimine.common.level.logic.filterNeighborsOf
import dev.lucasnlm.antimine.common.level.models.Area
import dev.lucasnlm.antimine.common.level.models.Difficulty
import dev.lucasnlm.antimine.common.level.models.Mark
import dev.lucasnlm.antimine.common.level.models.Minefield
import dev.lucasnlm.antimine.common.level.models.Score
import dev.lucasnlm.antimine.common.level.models.StateUpdate
import dev.lucasnlm.antimine.common.level.solver.LimitedBruteForceSolver
import dev.lucasnlm.antimine.core.control.ActionResponse
import dev.lucasnlm.antimine.core.control.GameControl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.random.Random

class GameController {
    val minefield: Minefield
    private val startTime = System.currentTimeMillis()
    private var saveId = 0
    private var firstOpen: FirstOpen = FirstOpen.Unknown
    private var gameControl: GameControl = GameControl.Standard
    private var mines: Sequence<Area> = emptySequence()
    private var useQuestionMark = true

    var hasMines = false
        private set

    val seed: Long
    val roundedMap: Boolean

    private var minefieldCreator: MinefieldCreator? = null
    var field: List<Area>
        private set

    constructor(
        minefield: Minefield,
        seed: Long,
        roundedMap: Boolean,
        saveId: Int? = null
    ) {
        this.minefield = minefield
        this.seed = seed
        this.roundedMap = roundedMap
        this.saveId = saveId ?: 0

        val minefieldCreator = MinefieldCreator(minefield, roundedMap, Random(seed))
        this.field = minefieldCreator.createEmpty()
    }

    constructor(save: Save) {
        this.minefield = save.minefield
        this.saveId = save.uid
        this.seed = save.seed
        this.roundedMap = false
        this.firstOpen = save.firstOpen

        this.field = save.field
        this.mines = this.field.filter { it.hasMine }.asSequence()
        this.hasMines = this.mines.count() != 0
    }

    private fun getArea(id: Int) = field.first { it.id == id }

    private fun plantMinesExcept(safeId: Int) {
        val solver = LimitedBruteForceSolver()
        val minefieldCreator = MinefieldCreator(minefield, roundedMap, Random(seed))

        do {
            val useSafeZone = minefield.width > 9 && minefield.height > 9
            field = minefieldCreator.create(safeId, useSafeZone)
            val fieldCopy = field.map { it.copy() }.toMutableList()
            val minefieldHandler = MinefieldHandler(fieldCopy, false)
            minefieldHandler.openAt(safeId)
        } while (solver.keepTrying() && !solver.trySolve(minefieldHandler.result().toMutableList()))

        mines = field.filter { it.hasMine }.asSequence()
        firstOpen = FirstOpen.Position(safeId)
        hasMines = mines.count() != 0
    }

    private fun handleAction(target: Area, actionResponse: ActionResponse?) = flow {
        val mustPlantMines = !hasMines

        val minefieldHandler: MinefieldHandler

        if (mustPlantMines) {
            plantMinesExcept(target.id)
            minefieldHandler = MinefieldHandler(field.toMutableList(), useQuestionMark)
            minefieldHandler.openAt(target.id)
            emit(StateUpdate.Multiple)
        } else {
            minefieldHandler = MinefieldHandler(field.toMutableList(), useQuestionMark)
            minefieldHandler.turnOffAllHighlighted()

            when (actionResponse) {
                ActionResponse.OpenTile -> {
                    if (target.mark.isNotNone()) {
                        minefieldHandler.removeMarkAt(target.id)
                    } else {
                        minefieldHandler.openAt(target.id)
                    }
                }
                ActionResponse.SwitchMark -> {
                    if (!hasMines) {
                        if (target.mark.isNotNone()) {
                            minefieldHandler.removeMarkAt(target.id)
                        } else {
                            minefieldHandler.openAt(target.id)
                        }
                    } else {
                        minefieldHandler.switchMarkAt(target.id)
                    }
                }
                ActionResponse.HighlightNeighbors -> {
                    if (target.minesAround != 0) {
                        minefieldHandler.highlightAt(target.id)
                    }
                }
                ActionResponse.OpenNeighbors -> {
                    minefieldHandler.openOrFlagNeighborsOf(target.id)
                }
            }

            field = minefieldHandler.result()
            emit(minefieldHandler.getStateUpdate())
        }
    }

    @ExperimentalCoroutinesApi
    fun singleClick(index: Int) = flow {
        val target = getArea(index)
        val action = if (target.isCovered) gameControl.onCovered.singleClick else gameControl.onOpen.singleClick
        action?.let {
            emit(action to handleAction(target, action))
        }
    }

    @ExperimentalCoroutinesApi
    fun doubleClick(index: Int) = flow {
        val target = getArea(index)
        val action = if (target.isCovered) gameControl.onCovered.doubleClick else gameControl.onOpen.doubleClick
        action?.let {
            emit(action to handleAction(target, action))
        }
    }

    @ExperimentalCoroutinesApi
    fun longPress(index: Int) = flow {
        val target = getArea(index)
        val action = if (target.isCovered) gameControl.onCovered.longPress else gameControl.onOpen.longPress
        action?.let {
            emit(action to handleAction(target, action))
        }
    }

    @ExperimentalCoroutinesApi
    fun runFlagAssistant(): Flow<Int> {
        return FlagAssistant(field.toMutableList()).runFlagAssistant()
    }

    fun getScore() = Score(
        mines.count { !it.mistake && it.mark.isFlag() },
        mines.count(),
        field.count()
    )

    fun getMinesCount() = mines.count()

    fun showAllMines() =
        mines.filter { it.mark != Mark.Flag }.forEach { it.isCovered = false }

    fun findExplodedMine() = mines.filter { it.mistake }.firstOrNull()

    fun takeExplosionRadius(target: Area): Sequence<Area> =
        mines.filter { it.isCovered && it.mark.isNone() }.sortedBy {
            val dx1 = (it.posX - target.posX)
            val dy1 = (it.posY - target.posY)
            dx1 * dx1 + dy1 * dy1
        }

    fun flagAllMines() = mines.forEach { it.mark = Mark.Flag }

    fun showWrongFlags() = field.filter { it.mark.isNotNone() && !it.hasMine }.forEach { it.mistake = true }

    fun revealAllEmptyAreas() = field.filterNot { it.hasMine }.forEach { it.isCovered = false }

    fun hasAnyMineExploded(): Boolean = mines.firstOrNull { it.mistake } != null

    fun hasFlaggedAllMines(): Boolean = rightFlags() == minefield.mines

    fun hasIsolatedAllMines() =
        mines.map {
            val neighbors = field.filterNeighborsOf(it)
            val neighborsCount = neighbors.count()
            val isolatedNeighborsCount = neighbors.count { neighbor ->
                !neighbor.isCovered || neighbor.hasMine
            }
            neighborsCount != isolatedNeighborsCount
        }.count { it } == 0

    private fun rightFlags() = mines.count { it.mark.isFlag() }

    fun checkVictory(): Boolean =
        hasMines && hasIsolatedAllMines() && !hasAnyMineExploded()

    fun isGameOver(): Boolean =
        checkVictory() || hasAnyMineExploded()

    fun remainingMines(): Int {
        val flagsCount = field.count { it.mark.isFlag() }
        val minesCount = mines.count()
        return (minesCount - flagsCount).coerceAtLeast(0)
    }

    fun getSaveState(duration: Long, difficulty: Difficulty): Save {
        val saveStatus: SaveStatus = when {
            checkVictory() -> SaveStatus.VICTORY
            hasAnyMineExploded() -> SaveStatus.DEFEAT
            else -> SaveStatus.ON_GOING
        }
        return Save(
            saveId,
            seed,
            startTime,
            duration,
            minefield,
            difficulty,
            firstOpen,
            saveStatus,
            field.toList()
        )
    }

    fun getStats(duration: Long): Stats? {
        val gameStatus: SaveStatus = when {
            checkVictory() -> SaveStatus.VICTORY
            hasAnyMineExploded() -> SaveStatus.DEFEAT
            else -> SaveStatus.ON_GOING
        }
        return if (gameStatus == SaveStatus.ON_GOING) {
            null
        } else {
            Stats(
                0,
                duration,
                mines.count(),
                if (gameStatus == SaveStatus.VICTORY) 1 else 0,
                minefield.width,
                minefield.height,
                mines.count { !it.isCovered }
            )
        }
    }

    fun setCurrentSaveId(id: Int) {
        this.saveId = id.coerceAtLeast(0)
    }

    fun updateGameControl(newGameControl: GameControl) {
        this.gameControl = newGameControl
    }

    fun useQuestionMark(useQuestionMark: Boolean) {
        this.useQuestionMark = useQuestionMark
    }
}
