package me.dio.copa.catar.features

import android.content.res.Resources.NotFoundException
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.dio.copa.catar.core.BaseViewModel
import me.dio.copa.catar.domain.model.MatchDomain
import me.dio.copa.catar.domain.usecase.DisableNotificationUseCase
import me.dio.copa.catar.domain.usecase.EnableNotificationUseCase
import me.dio.copa.catar.domain.usecase.GetMatchesUseCase
import me.dio.copa.catar.remote.UnexpectedException
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val getMatchesUseCase: GetMatchesUseCase,
    private val disableNotificationUseCase: DisableNotificationUseCase,
    private val enableNotificationUseCase: EnableNotificationUseCase
) : BaseViewModel<MainUIState, MainUIAction>(MainUIState()){

    init {
        fetchMatches()
    }

    private fun fetchMatches() = viewModelScope.launch {
        getMatchesUseCase()
            .flowOn(Dispatchers.Main)
            .catch {
                when(it) {
                    is NotFoundException ->
                        sendAction(MainUIAction.MatchesNotFound(it.message ?: "Erro sem mensagem"))
                    is UnexpectedException ->
                        sendAction(MainUIAction.Unexpected)
                }
            }
            .collect { matches ->
                setState {
                    copy(matches = matches)
                }
            }
    }

    fun toggleNotification(match: MatchDomain){
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.Main){
                    val action = if(match.notificationEnabled){
                        disableNotificationUseCase(match.id)
                        MainUIAction.DisableNotification(match)
                    } else {
                        enableNotificationUseCase(match.id)
                        MainUIAction.EnableNotification(match)
                    }

                    sendAction(action)
                }
            }
        }
    }
}

data class MainUIState(
    val matches: List<MatchDomain> = emptyList()
)

sealed interface MainUIAction {
    data class MatchesNotFound(val message: String): MainUIAction
    object Unexpected: MainUIAction
    data class EnableNotification(val match: MatchDomain): MainUIAction
    data class DisableNotification(val match: MatchDomain): MainUIAction
}