package com.valid.motouring.data.repository

import com.valid.motouring.data.fake.FakeDataProvider
import com.valid.motouring.data.model.ScenicRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ScenicRouteRepository {
    private val routes = MutableStateFlow(FakeDataProvider.scenicRoutes)

    fun observeRoutes(): StateFlow<List<ScenicRoute>> = routes.asStateFlow()

    fun routes(): List<ScenicRoute> = routes.value

    fun route(id: String): ScenicRoute? = routes.value.firstOrNull { it.id == id }
}
