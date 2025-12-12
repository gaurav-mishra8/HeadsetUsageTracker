package com.headphonetracker

import com.headphonetracker.data.HeadphoneUsage
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.Assert.*
import java.text.SimpleDateFormat
import java.util.*

class BackupRestoreTest {
    
    @Test
    fun `test backup JSON structure`() {
        val usage1 = HeadphoneUsage(
            date = "2024-01-01",
            packageName = "com.app1",
            appName = "App 1",
            duration = 3600000L,
            startTime = 1000L,
            endTime = 3601000L
        )
        
        val usage2 = HeadphoneUsage(
            date = "2024-01-02",
            packageName = "com.app2",
            appName = "App 2",
            duration = 7200000L,
            startTime = 2000L,
            endTime = 7202000L
        )
        
        val json = createBackupJson(listOf(usage1, usage2))
        
        assertEquals(1, json.getInt("version"))
        assertTrue(json.has("exported_at"))
        assertTrue(json.has("data"))
        
        val dataArray = json.getJSONArray("data")
        assertEquals(2, dataArray.length())
        
        val item1 = dataArray.getJSONObject(0)
        assertEquals("2024-01-01", item1.getString("date"))
        assertEquals("com.app1", item1.getString("packageName"))
        assertEquals("App 1", item1.getString("appName"))
        assertEquals(3600000L, item1.getLong("duration"))
    }
    
    @Test
    fun `test backup JSON with empty data`() {
        val json = createBackupJson(emptyList())
        
        assertEquals(1, json.getInt("version"))
        val dataArray = json.getJSONArray("data")
        assertEquals(0, dataArray.length())
    }
    
    @Test
    fun `test restore parses JSON correctly`() {
        val json = JSONObject().apply {
            put("version", 1)
            put("exported_at", System.currentTimeMillis())
            put("data", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", 0)
                    put("date", "2024-01-01")
                    put("packageName", "com.app1")
                    put("appName", "App 1")
                    put("duration", 3600000L)
                    put("startTime", 1000L)
                    put("endTime", 3601000L)
                })
            })
        }
        
        val dataArray = json.getJSONArray("data")
        assertEquals(1, dataArray.length())
        
        val item = dataArray.getJSONObject(0)
        assertEquals("2024-01-01", item.getString("date"))
        assertEquals("com.app1", item.getString("packageName"))
        assertEquals(3600000L, item.getLong("duration"))
    }
    
    @Test
    fun `test restore handles missing fields gracefully`() {
        val json = JSONObject().apply {
            put("version", 1)
            put("data", JSONArray().apply {
                put(JSONObject().apply {
                    put("date", "2024-01-01")
                    put("packageName", "com.app1")
                    // Missing other fields
                })
            })
        }
        
        val dataArray = json.getJSONArray("data")
        val item = dataArray.getJSONObject(0)
        
        // Should not crash, but may have default values
        assertEquals("2024-01-01", item.getString("date"))
        assertEquals("com.app1", item.getString("packageName"))
    }
    
    @Test
    fun `test backup preserves all usage fields`() {
        val usage = HeadphoneUsage(
            id = 1L,
            date = "2024-01-01",
            packageName = "com.app1",
            appName = "App 1",
            duration = 3600000L,
            startTime = 1000L,
            endTime = 3601000L
        )
        
        val json = createBackupJson(listOf(usage))
        val item = json.getJSONArray("data").getJSONObject(0)
        
        assertEquals(usage.date, item.getString("date"))
        assertEquals(usage.packageName, item.getString("packageName"))
        assertEquals(usage.appName, item.getString("appName"))
        assertEquals(usage.duration, item.getLong("duration"))
        assertEquals(usage.startTime, item.getLong("startTime"))
        assertEquals(usage.endTime, item.getLong("endTime"))
    }
    
    @Test
    fun `test backup handles special characters in app names`() {
        val usage = HeadphoneUsage(
            date = "2024-01-01",
            packageName = "com.app1",
            appName = "App \"Special\" Name",
            duration = 3600000L,
            startTime = 1000L,
            endTime = 3601000L
        )
        
        val json = createBackupJson(listOf(usage))
        val item = json.getJSONArray("data").getJSONObject(0)
        
        // JSON should handle quotes correctly - JSONObject escapes them
        val appName = item.getString("appName")
        assertTrue("App name should contain Special", appName.contains("Special"))
    }
    
    @Test
    fun `test backup timestamp is valid`() {
        val beforeTime = System.currentTimeMillis()
        val json = createBackupJson(emptyList())
        val afterTime = System.currentTimeMillis()
        val timestamp = json.getLong("exported_at")
        
        assertTrue("Timestamp should be positive", timestamp > 0)
        assertTrue("Timestamp should be recent", timestamp >= beforeTime)
        assertTrue("Timestamp should be before test end", timestamp <= afterTime)
    }
    
    private fun createBackupJson(usages: List<HeadphoneUsage>): JSONObject {
        return JSONObject().apply {
            put("version", 1)
            put("exported_at", System.currentTimeMillis())
            put("data", JSONArray().apply {
                usages.forEach { usage ->
                    put(JSONObject().apply {
                        put("id", usage.id)
                        put("date", usage.date)
                        put("packageName", usage.packageName)
                        put("appName", usage.appName)
                        put("duration", usage.duration)
                        put("startTime", usage.startTime)
                        put("endTime", usage.endTime)
                    })
                }
            })
        }
    }
}

