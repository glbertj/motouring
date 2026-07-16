package com.valid.motouring.ui.scenic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.ScenicRoute
import com.valid.motouring.data.repository.ScenicRouteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScenicRouteDetailViewModel(
    scenicRouteRepository: ScenicRouteRepository,
    routeId: String,
) : ViewModel() {

    private val _route = MutableStateFlow(scenicRouteRepository.route(routeId))
    val route: StateFlow<ScenicRoute?> = _route.asStateFlow()

    companion object {
        fun factory(scenicRouteRepository: ScenicRouteRepository, routeId: String) = viewModelFactory {
            initializer { ScenicRouteDetailViewModel(scenicRouteRepository, routeId) }
        }
    }
}
