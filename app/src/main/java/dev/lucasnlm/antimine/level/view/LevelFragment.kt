package dev.lucasnlm.antimine.level.view

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import androidx.lifecycle.Observer
import dagger.hilt.android.AndroidEntryPoint
import dev.lucasnlm.antimine.DeepLink
import dev.lucasnlm.antimine.common.R
import dev.lucasnlm.antimine.common.level.models.Difficulty
import dev.lucasnlm.antimine.common.level.models.Event
import dev.lucasnlm.antimine.common.level.view.CommonLevelFragment
import dev.lucasnlm.antimine.common.level.view.SpaceItemDecoration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
open class LevelFragment : CommonLevelFragment(R.layout.fragment_level) {
    override fun onPause() {
        super.onPause()
        GlobalScope.launch {
            viewModel.saveGame()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerGrid = view.findViewById(R.id.recyclerGrid)

        viewModel.run {
            field.observe(
                viewLifecycleOwner,
                Observer {
                    areaAdapter.bindField(it)
                }
            )

            fieldRefresh.observe(
                viewLifecycleOwner,
                Observer {
                    areaAdapter.notifyItemChanged(it)
                }
            )

            eventObserver.observe(
                viewLifecycleOwner,
                Observer {
                    when (it) {
                        Event.Pause,
                        Event.GameOver,
                        Event.Victory -> areaAdapter.setClickEnabled(false)
                        Event.Running,
                        Event.Resume,
                        Event.ResumeGame,
                        Event.StartNewGame -> areaAdapter.setClickEnabled(true)
                        null -> { }
                    }
                }
            )
        }
    }

    @FlowPreview
    @ExperimentalCoroutinesApi
    override fun onAttach(context: Context) {
        super.onAttach(context)

        GlobalScope.launch {
            val loadGameUid = checkLoadGameDeepLink()
            val newGameDeepLink = checkNewGameDeepLink()
            val retryDeepLink = checkRetryGameDeepLink()
            val difficulty = arguments?.getInt(LEVEL_DIFFICULTY, -1) ?: -1

            val levelSetup = when {
                difficulty > 0 -> viewModel.startNewGame(Difficulty.values()[difficulty])
                loadGameUid != null -> viewModel.loadGame(loadGameUid)
                newGameDeepLink != null -> viewModel.startNewGame(newGameDeepLink)
                retryDeepLink != null -> viewModel.retryGame(retryDeepLink)
                else -> viewModel.loadLastGame()
            }

            withContext(Dispatchers.Main) {
                recyclerGrid.apply {
                    val horizontalPadding = calcHorizontalPadding(levelSetup.width)
                    val verticalPadding = calcVerticalPadding(levelSetup.height)
                    addItemDecoration(SpaceItemDecoration(R.dimen.field_padding))
                    setPadding(horizontalPadding, verticalPadding, 0, 0)
                    layoutManager = makeNewLayoutManager(levelSetup.width)
                    setHasFixedSize(true)
                    adapter = areaAdapter
                    alpha = 0.0f

                    animate().apply {
                        alpha(1.0f)
                        duration = DateUtils.SECOND_IN_MILLIS
                    }.start()
                }
            }
        }
    }

    private fun checkNewGameDeepLink(): Difficulty? = activity?.intent?.data?.let { uri ->
        if (uri.scheme == DeepLink.SCHEME && uri.authority == DeepLink.NEW_GAME_AUTHORITY) {
            when (uri.pathSegments.firstOrNull()) {
                DeepLink.BEGINNER_PATH -> Difficulty.Beginner
                DeepLink.INTERMEDIATE_PATH -> Difficulty.Intermediate
                DeepLink.EXPERT_PATH -> Difficulty.Expert
                DeepLink.STANDARD_PATH -> Difficulty.Standard
                DeepLink.CUSTOM_PATH -> Difficulty.Custom
                else -> null
            }
        } else {
            null
        }
    }

    private fun checkLoadGameDeepLink(): Int? = activity?.intent?.data?.let { uri ->
        if (uri.scheme == DeepLink.SCHEME && uri.authority == DeepLink.LOAD_GAME_AUTHORITY) {
            uri.pathSegments.firstOrNull()?.toIntOrNull()
        } else {
            null
        }
    }

    private fun checkRetryGameDeepLink(): Int? = activity?.intent?.data?.let { uri ->
        if (uri.scheme == DeepLink.SCHEME && uri.authority == DeepLink.RETRY_HOST_AUTHORITY) {
            uri.pathSegments.firstOrNull()?.toIntOrNull()
        } else {
            null
        }
    }

    companion object {
        private const val LEVEL_DIFFICULTY = "level_difficulty"

        fun newInstance(newDifficulty: Int?): LevelFragment {
            return LevelFragment().apply {
                arguments = Bundle().apply {
                    newDifficulty?.let { putInt(LEVEL_DIFFICULTY, it) }
                }
            }
        }
    }
}
