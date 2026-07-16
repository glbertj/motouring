package com.valid.motouring.ui.scenic

import com.valid.motouring.data.repository.ScenicRouteRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class ScenicRoutesViewModelTest {

    @Test
    fun `routes are exposed from the repository`() {
        val vm = ScenicRoutesViewModel(ScenicRouteRepository())
        assertTrue(vm.routes.value.isNotEmpty())
    }
}
