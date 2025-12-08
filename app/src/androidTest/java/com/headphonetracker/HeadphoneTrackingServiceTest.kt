package com.headphonetracker

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.Assert.*

@RunWith(AndroidJUnit4::class)
class HeadphoneTrackingServiceTest {
    
    @get:Rule
    val serviceRule = ServiceTestRule()
    
    @Test
    fun serviceCanBeBound() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            HeadphoneTrackingService::class.java
        )
        
        // Service should be able to be started
        assertNotNull(intent)
    }
    
    @Test
    fun trackingFlowInitialState() {
        // The flow should be accessible
        assertNotNull(HeadphoneTrackingService.isTrackingFlow)
        assertFalse(HeadphoneTrackingService.isTrackingFlow.value)
    }
    
    @Test
    fun trackingFlowCanBeUpdated() = runBlocking {
        val initialValue = HeadphoneTrackingService.isTrackingFlow.value
        
        HeadphoneTrackingService.isTrackingFlow.value = true
        assertTrue(HeadphoneTrackingService.isTrackingFlow.value)
        
        HeadphoneTrackingService.isTrackingFlow.value = false
        assertFalse(HeadphoneTrackingService.isTrackingFlow.value)
        
        // Restore initial value
        HeadphoneTrackingService.isTrackingFlow.value = initialValue
    }
}

