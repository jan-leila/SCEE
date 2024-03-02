package de.westnordost.streetcomplete.quests.lgbtq

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.data.osm.geometry.ElementGeometry
import de.westnordost.streetcomplete.data.osm.mapdata.Element
import de.westnordost.streetcomplete.data.osm.mapdata.MapDataWithGeometry
import de.westnordost.streetcomplete.data.osm.osmquests.OsmFilterQuestType
import de.westnordost.streetcomplete.data.user.achievements.EditTypeAchievement
import de.westnordost.streetcomplete.osm.Tags

class LGBTQAccessQuest : OsmFilterQuestType<LGBTQAccessAnswer>() {
    override val elementFilter = """
        nodes, ways with (!lgbtq and lgbtq:signed = yes)
        and !memorial and !historic
    """

    // countries that are listed here ban lgbtq people
    override val enabledInCountries = LGBTExcludedCountries

    override val changesetComment = "Survey lgbtq access"
    override val wikiLink = "Key:lgbtq"
    // TODO: replace me
    override val icon = R.drawable.ic_quest_shop
    override val achievements = listOf(EditTypeAchievement.CITIZEN)

    override fun getTitle(tags: Map<String, String>) = R.string.quest_lgbtq_access

    override fun getApplicableElements(mapData: MapDataWithGeometry): Iterable<Element> =
        mapData.filter { isApplicableTo(it) }

    override fun isApplicableTo(element: Element): Boolean = filter.matches(element)
    override fun createForm() = LGBTQAccessForm()

    override fun applyAnswerTo(answer: LGBTQAccessAnswer, tags: Tags, geometry: ElementGeometry, timestampEdited: Long) {
        if (answer.access == null) {
            tags["lgbtq:signed"] = "no"
        } else {
            tags["lgbtq"] = answer.access.osmValue
            tags["lgbtq:signed"] = "yes"
        }
    }
}
