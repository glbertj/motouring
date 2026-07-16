package com.valid.motouring.ui.scenic

import com.valid.motouring.data.repository.ScenicRouteRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class ScenicRouteDetailViewModelTest {

    @Test
    fun `state exposes the looked-up route`() {
        val repo = ScenicRouteRepository()
        val target = repo.routes().first()
        val vm = ScenicRouteDetailViewModel(repo, target.id)
        assertEquals(target.id, vm.route.value?.id)
    }
}
