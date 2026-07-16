package com.valid.motouring.ui.scenic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valid.motouring.data.model.ScenicRoute
import com.valid.motouring.data.repository.ScenicRouteRepository
import kotlinx.coroutines.flow.StateFlow

class ScenicRoutesViewModel(
    scenicRouteRepository: ScenicRouteRepository,
) : ViewModel() {

    val routes: StateFlow<List<ScenicRoute>> = scenicRouteRepository.observeRoutes()

    companion object {
        fun factory(scenicRouteRepository: ScenicRouteRepository) = viewModelFactory {
            initializer { ScenicRoutesViewModel(scenicRouteRepository) }
        }
    }
}
