package de.westnordost.streetcomplete.quests.osmose

import android.content.SharedPreferences
import android.util.Log
import de.westnordost.streetcomplete.ApplicationConstants.USER_AGENT
import de.westnordost.streetcomplete.data.ConflictAlgorithm
import de.westnordost.streetcomplete.data.CursorPosition
import de.westnordost.streetcomplete.data.osm.mapdata.BoundingBox
import de.westnordost.streetcomplete.data.Database
import de.westnordost.streetcomplete.data.osm.edits.MapDataWithEditsSource
import de.westnordost.streetcomplete.data.osm.geometry.ElementPointGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.ElementKey
import de.westnordost.streetcomplete.data.osm.mapdata.ElementType
import de.westnordost.streetcomplete.data.osm.mapdata.LatLon
import de.westnordost.streetcomplete.data.othersource.OtherSourceQuest
import de.westnordost.streetcomplete.data.quest.QuestTypeRegistry
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.CLASS
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.ELEMENTS
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.ANSWERED
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.ITEM
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.LATITUDE
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.LEVEL
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.LONGITUDE
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.SUBTITLE
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.TIMESTAMP
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.TITLE
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.Columns.UUID
import de.westnordost.streetcomplete.quests.osmose.OsmoseTable.NAME
import de.westnordost.streetcomplete.quests.questPrefix
import de.westnordost.streetcomplete.util.math.enclosingBoundingBox
import okhttp3.OkHttpClient
import okhttp3.Request
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.IOException

class OsmoseDao(
    private val db: Database,
    private val prefs: SharedPreferences,
) : KoinComponent {
    private val client = OkHttpClient()
    val type = OsmoseQuest(this) // ugly, but at least it works...

    private val mapDataWithEditsSource: MapDataWithEditsSource by inject()
    private val questTypeRegistry: QuestTypeRegistry by inject()

    private val ignoredItems = hashSetOf<Int>()
    private val ignoredItemClassCombinations = hashSetOf<String>()
    private val allowedLevels = hashSetOf<Int>()
    init { reloadIgnoredItems() }
    fun reloadIgnoredItems() {
        val ignored = prefs.getString(questPrefix(prefs) + PREF_OSMOSE_ITEMS, "")!!.split(',')
        ignoredItems.clear()
        ignoredItems.addAll(
                ignored.mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
        )
        ignoredItemClassCombinations.clear()
        ignoredItemClassCombinations.addAll(
            ignored.mapNotNull { if (!it.contains('/')) null else it.trim() }
        )
        allowedLevels.clear()
        allowedLevels.addAll(
            prefs.getString(questPrefix(prefs) + PREF_OSMOSE_LEVEL, "1")!!.split("%2C").mapNotNull { it.toIntOrNull() }
        )
    }

    fun download(bbox: BoundingBox): List<OtherSourceQuest> {
        if (!prefs.getBoolean(questPrefix(prefs) + PREF_OSMOSE_ENABLE_DOWNLOAD, false)) return emptyList()
        // https://osmose.openstreetmap.fr/api/0.3/issues.csv?zoom=18&item=xxxx&level=1&limit=500&bbox=16.412324309349064%2C48.18403988244578%2C16.41940534114838%2C48.1871908341706
        // replace bbox
        val csvUrl = "https://osmose.openstreetmap.fr/api/0.3/issues.csv"
        val zoom = 16 // what is the use?
        val level = prefs.getString(questPrefix(prefs) + PREF_OSMOSE_LEVEL, "1")
        val request = Request.Builder()
            .url("$csvUrl?zoom=$zoom&item=xxxx&level=$level&limit=500&bbox=${bbox.min.longitude}%2C${bbox.min.latitude}%2C${bbox.max.longitude}%2C${bbox.max.latitude}")
            .header("User-Agent", USER_AGENT)
            .build()
        Log.d(TAG, "downloading for bbox: $bbox using request ${request.url()}")
        val issues = mutableListOf<OsmoseIssue>()
        try {
            val response = client.newCall(request).execute()
            val body = response.body() ?: return emptyList()
            // drop first, it's just column names
            // drop last, it's an empty line
            // trim each line because there was some additional newline in logs (maybe windows line endings?)
            val bodylines = body.string().split("\n").drop(1).dropLast(1)
            Log.d(TAG, "got ${bodylines.size} problems")

            val downloadTimestamp = System.currentTimeMillis()
            db.replaceMany(NAME,
                arrayOf(UUID, ITEM, CLASS, LEVEL, TITLE, SUBTITLE, LATITUDE, LONGITUDE, ELEMENTS, ANSWERED, TIMESTAMP),
                bodylines.mapNotNull {
                    val split = it.trim().split(splitRegex) // from https://stackoverflow.com/questions/53997728/parse-csv-to-kotlin-list
                    if (split.size != 14) {
                        Log.i(TAG, "skip line, not split into 14 items: $split")
                        null
                    } else {
                        val item = split[2].toIntOrNull()
                        val itemClass = split[3].toIntOrNull()
                        val itemLevel = split[4].toIntOrNull()
                        val lat = split[11].toDoubleOrNull()
                        val lon = split[12].toDoubleOrNull()
                        if (item == null || itemClass == null || itemLevel == null || lat == null || lon == null) {
                            Log.i(TAG, "skip line, could not parse some numbers: $split")
                            return@mapNotNull null
                        }
                        // don't create quest for ignored items. this is less flexible than
                        if (item in ignoredItems || "$item/$itemClass" in ignoredItemClassCombinations)
                            return@mapNotNull null
                        issues.add(OsmoseIssue(
                            split[0], item, itemClass, itemLevel, split[5], split[6], LatLon(lat, lon), parseElementKeys(split[13])
                        ))
                        arrayOf(split[0], item, itemClass, itemLevel, split[5], split[6], lat, lon, split[13], 0, downloadTimestamp)
                    }
                }
            )
            db.delete(NAME, where = "${inBoundsSql(bbox)} AND $TIMESTAMP < $downloadTimestamp") // delete old issues inside the bbox
        } catch (e: Exception) {
            Log.e(TAG, "error while downloading / inserting: ${e.message}", e)
        }
        return issues.mapNotNull { it.toQuest() }
    }

    fun getQuest(uuid: String): OtherSourceQuest? =
        db.queryOne(NAME, where = "$UUID = '$uuid' AND $ANSWERED = 0") { it.toOsmoseIssue().toQuest() }

    fun getIssue(uuid: String): OsmoseIssue? =
        db.queryOne(NAME, where = "$UUID = '$uuid' AND $ANSWERED = 0") { c -> c.toOsmoseIssue().takeIf { !it.isIgnored() } }

    fun getAllQuests(bbox: BoundingBox): List<OtherSourceQuest> =
        db.query(NAME, where = "${inBoundsSql(bbox)} AND $ANSWERED = 0") {
            it.toOsmoseIssue()
        }.mapNotNull { it.toQuest() }

    private fun OsmoseIssue.toQuest(): OtherSourceQuest? =
        if (isIgnored()) null
        else
            OtherSourceQuest(
                uuid,
                if (elements.size == 1) mapDataWithEditsSource.getGeometry(elements.single().type, elements.single().id) ?: ElementPointGeometry(position)
                else ElementPointGeometry(position),
                type
        )

    fun setFromDoneToNotAnsweredNear(position: LatLon) {
        db.update(NAME,
            values = listOf(ANSWERED to -1),
            where = "${inBoundsSql(position.enclosingBoundingBox(1.0))} AND $ANSWERED = -1",
            conflictAlgorithm = ConflictAlgorithm.IGNORE
        )
    }

    private fun reportChange(uuid: String, falsePositive: Boolean) {
        val url = "https://osmose.openstreetmap.fr/api/0.3/issue/$uuid/" +
            if (falsePositive) "false"
            else "done"
        val request = Request.Builder().url(url).build()
        try {
            client.newCall(request).execute()
            db.delete(NAME, where = "$UUID = '$uuid'")
        } catch (e: IOException) {
            // just do nothing, so it's later tried again (hopefully...)
            Log.i(TAG, "error while uploading: ${e.message} to $url")
        }
    }

    fun reportChanges() {
        db.query(NAME, where = "$ANSWERED != 0") {
            Pair(
                it.getString(UUID),
                it.getInt(ANSWERED) == 1
            ) }.forEach { reportChange(it.first, it.second) }

    }

    fun setAsFalsePositive(uuid: String) {
        db.update(NAME, values = listOf(ANSWERED to 1), where = "$UUID = '$uuid'",
            conflictAlgorithm = ConflictAlgorithm.IGNORE
        )
    }

    fun setDone(uuid: String) {
        db.update(NAME, values = listOf(ANSWERED to -1), where = "$UUID = '$uuid'",
            conflictAlgorithm = ConflictAlgorithm.IGNORE
        )
    }

    fun setNotAnswered(uuid: String) {
        db.update(NAME, values = listOf(ANSWERED to 0), where = "$UUID = '$uuid'",
            conflictAlgorithm = ConflictAlgorithm.IGNORE
        )
    }

    fun delete(uuid: String) = db.delete(NAME, where = "$UUID = '$uuid'") > 0

    fun deleteOlderThan(timestamp: Long) {
        db.delete(NAME, where = "$TIMESTAMP < $timestamp")
    }

    fun clear() {
        db.delete(NAME)
    }

    private fun OsmoseIssue.isIgnored() = item in ignoredItems || level !in allowedLevels || "$item/$itemClass" in ignoredItemClassCombinations

}

private const val TAG = "OsmoseDao"

data class OsmoseIssue(
    val uuid: String,
    val item: Int,
    val itemClass: Int,
    val level: Int,
    val title: String,
    val subtitle: String,
    val position: LatLon,
    val elements: List<ElementKey>,
)

private fun CursorPosition.toOsmoseIssue() = OsmoseIssue(
    getString(UUID),
    getInt(ITEM),
    getInt(CLASS),
    getInt(LEVEL),
    getString(TITLE),
    getString(SUBTITLE),
    LatLon(getDouble(LATITUDE), getDouble(LONGITUDE)),
    parseElementKeys(getString(ELEMENTS))
)

private fun parseElementKeys(elementString: String): List<ElementKey> {
    return try {
        elementString.split("_").mapNotNull { e ->
            when {
                e.startsWith("node") -> e.substringAfter("node").toLongOrNull()?.let { ElementKey(ElementType.NODE, it) }
                e.startsWith("way") -> e.substringAfter("way").toLongOrNull()?.let { ElementKey(ElementType.WAY, it) }
                e.startsWith("relation") -> e.substringAfter("relation").toLongOrNull()?.let { ElementKey(ElementType.RELATION, it) }
                else -> null
            }
        }
    } catch (e: Exception) {
        Log.w(TAG, "could not parse element string: $elementString")
        emptyList()
    }
}

object OsmoseTable {
    const val NAME = "osmose_issues_v2"
    private const val NAME_INDEX = "osmose_issues_spatial_index"

    object Columns {
        const val UUID = "uuid"
        const val ITEM = "item"
        const val CLASS = "class"
        const val LEVEL = "level"
        const val TITLE = "title"
        const val SUBTITLE = "subtitle"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val ELEMENTS = "elements"
        const val ANSWERED = "answered"
        const val TIMESTAMP = "download_timestamp"
    }

    const val CREATE_IF_NOT_EXISTS = """
        CREATE TABLE IF NOT EXISTS $NAME (
            $UUID varchar(255) PRIMARY KEY NOT NULL,
            $ITEM int NOT NULL,
            $CLASS int NOT NULL,
            $LEVEL int NOT NULL,
            $TITLE text,
            $SUBTITLE text,
            $LATITUDE float NOT NULL,
            $LONGITUDE float NOT NULL,
            $ELEMENTS text,
            $ANSWERED int NOT NULL,
            $TIMESTAMP int NOT NULL
        );
    """

    const val CREATE_SPATIAL_INDEX_IF_NOT_EXISTS = """
        CREATE INDEX IF NOT EXISTS $NAME_INDEX ON $NAME (
            $LATITUDE,
            $LONGITUDE
        );
    """

}

private val splitRegex = ",(?=([^\"]*\"[^\"]*\")*[^\"]*$)".toRegex()

private fun inBoundsSql(bbox: BoundingBox): String = """
    ($LATITUDE BETWEEN ${bbox.min.latitude} AND ${bbox.max.latitude}) AND
    ($LONGITUDE BETWEEN ${bbox.min.longitude} AND ${bbox.max.longitude})
""".trimIndent()
