package de.westnordost.streetcomplete.overlays.way_lit

import androidx.fragment.app.Fragment
import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.filter
import de.westnordost.streetcomplete.osm.ALL_PATHS
import de.westnordost.streetcomplete.osm.ALL_ROADS
import de.westnordost.streetcomplete.overlays.Color
import de.westnordost.streetcomplete.overlays.Overlay
import de.westnordost.streetcomplete.overlays.PolylineStyle

class WayLitOverlay : Overlay {

    override val title = R.string.overlay_lit
    override val icon = R.drawable.ic_quest_lantern

    override fun getStyledElements(mapData: MapDataWithGeometry) =
        mapData
            .filter("ways with highway ~ ${(ALL_ROADS + ALL_PATHS).joinToString("|")}")
            .map { it to PolylineStyle(createLitStatus(it).color) }

    override fun createForm(element: Element): Fragment? {
        TODO("Not yet implemented")
    }
}

private val LitStatus?.color: String get() = when (this) {
    LitStatus.YES ->           "#ccff00"
    LitStatus.NIGHT_AND_DAY -> "#33ff00"
    LitStatus.AUTOMATIC ->     "#00aaff"
    LitStatus.NO ->            "#555555"
    LitStatus.UNSUPPORTED ->   Color.UNSUPPORTED
    null ->                    Color.UNSPECIFIED
}
