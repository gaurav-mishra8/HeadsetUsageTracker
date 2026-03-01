package com.headphonetracker.data

import org.json.JSONArray
import org.json.JSONObject

object BackupJsonUtils {

    fun createBackupJson(usages: List<HeadphoneUsage>): JSONObject {
        val json = JSONObject()
        json.put("version", 1)
        json.put("exported_at", System.currentTimeMillis())

        val dataArray = JSONArray()
        usages.forEach { usage ->
            val item = JSONObject()
            item.put("id", usage.id)
            item.put("date", usage.date)
            item.put("packageName", usage.packageName)
            item.put("appName", usage.appName)
            item.put("duration", usage.duration)
            item.put("startTime", usage.startTime)
            item.put("endTime", usage.endTime)
            dataArray.put(item)
        }

        json.put("data", dataArray)
        return json
    }

    fun parseBackupJson(jsonString: String): List<HeadphoneUsage> {
        val json = JSONObject(jsonString)
        val dataArray = json.optJSONArray("data") ?: JSONArray()
        val result = mutableListOf<HeadphoneUsage>()

        for (i in 0 until dataArray.length()) {
            val item = dataArray.optJSONObject(i) ?: continue
            val date = item.optString("date")
            val packageName = item.optString("packageName")

            if (date.isBlank() || packageName.isBlank()) {
                continue
            }

            val usage = HeadphoneUsage(
                id = 0,
                date = date,
                packageName = packageName,
                appName = item.optString("appName", "Unknown"),
                duration = item.optLong("duration", 0L),
                startTime = item.optLong("startTime", 0L),
                endTime = item.optLong("endTime", 0L)
            )
            result.add(usage)
        }

        return result
    }
}
