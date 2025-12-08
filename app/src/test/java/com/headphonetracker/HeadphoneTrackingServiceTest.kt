package com.headphonetracker

import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class HeadphoneTrackingServiceTest {
    
    @Before
    fun setUp() {
        // Reset tracking flow state
        HeadphoneTrackingService.isTrackingFlow.value = false
    }
    
    @Test
    fun `test tracking flow initial state`() {
        assertFalse(HeadphoneTrackingService.isTrackingFlow.value)
    }
    
    @Test
    fun `test tracking flow can be set to true`() {
        HeadphoneTrackingService.isTrackingFlow.value = true
        assertTrue(HeadphoneTrackingService.isTrackingFlow.value)
    }
    
    @Test
    fun `test tracking flow can be set to false`() {
        HeadphoneTrackingService.isTrackingFlow.value = true
        HeadphoneTrackingService.isTrackingFlow.value = false
        assertFalse(HeadphoneTrackingService.isTrackingFlow.value)
    }
    
    @Test
    fun `test tracking flow is mutable state flow`() {
        assertTrue(HeadphoneTrackingService.isTrackingFlow is MutableStateFlow<Boolean>)
    }
}

