package de.westnordost.streetcomplete.quests.lgbtq

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.view.image_select.DisplayItem
import de.westnordost.streetcomplete.view.image_select.Item

enum class LGTBQAccess(val osmValue: String) {
    NO("no"),
    WELCOME("welcome"),
    PRIMARY("primary"),
    ONLY("only"),
}

fun List<LGTBQAccess>.toItems() = this.map { it.asItem() }
fun LGTBQAccess.asItem(): DisplayItem<LGTBQAccess> = Item(this, iconResId, titleResId)

val LGTBQAccess.titleResId: Int get() = when (this) {
    LGTBQAccess.NO -> R.string.quest_lgbtq_access_no
    LGTBQAccess.WELCOME -> R.string.quest_lgbtq_access_welcome
    LGTBQAccess.PRIMARY -> R.string.quest_lgbtq_access_primary
    LGTBQAccess.ONLY -> R.string.quest_lgbtq_access_only
}

// TOOD: populate icons
val LGTBQAccess.iconResId: Int get() = when (this) {
    LGTBQAccess.NO -> R.drawable.surface_asphalt
    LGTBQAccess.WELCOME -> R.drawable.surface_asphalt
    LGTBQAccess.PRIMARY -> R.drawable.surface_asphalt
    LGTBQAccess.ONLY -> R.drawable.surface_asphalt
}
