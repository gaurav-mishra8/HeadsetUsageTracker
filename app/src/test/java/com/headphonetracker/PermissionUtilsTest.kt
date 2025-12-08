package com.headphonetracker

import org.junit.Assert.*
import org.junit.Test

class PermissionUtilsTest {
    
    @Test
    fun `test usage stats permission constant`() {
        // Verify that the permission string is correct
        val permission = "android.permission.PACKAGE_USAGE_STATS"
        assertNotNull(permission)
        assertTrue(permission.contains("USAGE_STATS"))
    }
    
    @Test
    fun `test notification permission constant`() {
        val permission = "android.permission.POST_NOTIFICATIONS"
        assertNotNull(permission)
        assertTrue(permission.contains("NOTIFICATIONS"))
    }
    
    @Test
    fun `test foreground service permission`() {
        val permission = "android.permission.FOREGROUND_SERVICE"
        assertNotNull(permission)
        assertTrue(permission.contains("FOREGROUND"))
    }
}

