package switchdektoptocompose.viewmodel

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import switchdektoptocompose.model.ClientInfo
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @Test
    fun testUpdateQrBitmaps() = runTest(testDispatcher) {
        val settingsViewModel = SettingsViewModel()
        val pendingRequests = MutableStateFlow<List<ClientInfo>>(emptyList())
        val viewModel = PairingViewModel(settingsViewModel, pendingRequests)

        val client1 = ClientInfo(id = "1", name = "Client 1", verificationCode = "123456")
        val client2 = ClientInfo(id = "2", name = "Client 2", verificationCode = "654321")

        // Update via Flow
        pendingRequests.value = listOf(client1, client2)
        
        // Give it some time for QR generation which might be on Dispatchers.Default
        // In a real CI environment, we might want to inject dispatchers.
        advanceUntilIdle()
        
        assertEquals(2, viewModel.qrBitmaps.value.size, "Should have 2 bitmaps")
        assertTrue(viewModel.qrBitmaps.value.containsKey("1"))
        assertTrue(viewModel.qrBitmaps.value.containsKey("2"))

        // Update with one stale removed and one new
        val client3 = ClientInfo(id = "3", name = "Client 3", verificationCode = "111111")
        pendingRequests.value = listOf(client2, client3)
        advanceUntilIdle()

        assertEquals(2, viewModel.qrBitmaps.value.size, "Should still have 2 bitmaps after update")
        assertTrue(viewModel.qrBitmaps.value.containsKey("2"), "Client 2 should remain")
        assertTrue(viewModel.qrBitmaps.value.containsKey("3"), "Client 3 should be added")
        assertTrue(!viewModel.qrBitmaps.value.containsKey("1"), "Client 1 should be removed")
    }
}
