package com.valid.motouring.ui.profile

import com.valid.motouring.data.repository.RideBuddyRepository
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedContactsViewModelTest {

    @Test
    fun `state lists friends and toggle flags a trusted contact`() {
        val repo = RideBuddyRepository()
        val vm = TrustedContactsViewModel(repo)
        val friends = vm.state.value
        assertTrue(friends.isNotEmpty())
        // Pick an un-flagged friend so this is robust to FakeDataProvider pre-flagging (Task 10).
        val target = friends.first { !it.isTrustedContact }
        vm.toggle(target.user.id, true)
        assertTrue(vm.state.value.first { it.user.id == target.user.id }.isTrustedContact)
    }
}
