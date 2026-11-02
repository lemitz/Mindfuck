package com.mindfuck.app.data

import org.json.JSONArray
import org.json.JSONObject

/** Plain JSON backup of the whole database. */
object Backup {
    private const val VERSION = 1

    suspend fun export(dao: MoodDao): String {
        val root = JSONObject()
        root.put("app", "mindfuck")
        root.put("version", VERSION)
        root.put("entries", JSONArray().apply {
            dao.allEntries().forEach { e ->
                put(JSONObject().apply {
                    put("day", e.day)
                    putOpt("sleepHours", e.sleepHours?.toDouble())
                    putOpt("depression", e.depression)
                    putOpt("elevated", e.elevated)
                    putOpt("irritability", e.irritability)
                    putOpt("anxiety", e.anxiety)
                    putOpt("psychotic", e.psychotic)
                    putOpt("therapy", e.therapy)
                    put("notes", e.notes)
                })
            }
        })
        root.put("medications", JSONArray().apply {
            dao.allMeds().forEach { m ->
                put(JSONObject().put("id", m.id).put("name", m.name).put("dose", m.dose).put("active", m.active))
            }
        })
        root.put("intakes", JSONArray().apply {
            dao.allIntakes().forEach { put(JSONObject().put("day", it.day).put("medId", it.medId).putOpt("dose", it.dose)) }
        })
        root.put("trackers", JSONArray().apply {
            dao.allTrackers().forEach { t ->
                put(JSONObject().put("id", t.id).put("name", t.name).put("type", t.type).put("active", t.active))
            }
        })
        root.put("values", JSONArray().apply {
            dao.allValues().forEach {
                put(JSONObject().put("day", it.day).put("trackerId", it.trackerId).put("value", it.value))
            }
        })
        return root.toString(1)
    }

    /** Replaces all data. Throws on malformed input, leaving the DB untouched. */
    suspend fun import(dao: MoodDao, json: String) {
        val root = JSONObject(json)
        require(root.optString("app") == "mindfuck") { "Not a Mindfuck backup" }

        fun JSONObject.intOrNull(k: String) = if (has(k) && !isNull(k)) getInt(k) else null
        fun JSONObject.boolOrNull(k: String) = if (has(k) && !isNull(k)) getBoolean(k) else null
        fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }

        val entries = root.getJSONArray("entries").objects().map { o ->
            DayEntry(
                day = o.getLong("day"),
                sleepHours = if (o.has("sleepHours") && !o.isNull("sleepHours")) o.getDouble("sleepHours").toFloat() else null,
                depression = o.intOrNull("depression"),
                elevated = o.intOrNull("elevated"),
                irritability = o.intOrNull("irritability"),
                anxiety = o.intOrNull("anxiety"),
                psychotic = o.boolOrNull("psychotic"),
                therapy = o.boolOrNull("therapy"),
                notes = o.optString("notes", ""),
            )
        }
        val meds = root.optJSONArray("medications")?.objects()?.map {
            Medication(it.getLong("id"), it.getString("name"), it.optString("dose"), it.optBoolean("active", true))
        }.orEmpty()
        val intakes = root.optJSONArray("intakes")?.objects()?.map {
            MedIntake(it.getLong("day"), it.getLong("medId"), if (it.has("dose") && !it.isNull("dose")) it.getString("dose") else null)
        }.orEmpty()
        val trackers = root.optJSONArray("trackers")?.objects()?.map {
            CustomTracker(it.getLong("id"), it.getString("name"), it.getInt("type"), it.optBoolean("active", true))
        }.orEmpty()
        val values = root.optJSONArray("values")?.objects()?.map {
            CustomValue(it.getLong("day"), it.getLong("trackerId"), it.getInt("value"))
        }.orEmpty()

        dao.replaceAll(entries, meds, intakes, trackers, values)
    }
}
