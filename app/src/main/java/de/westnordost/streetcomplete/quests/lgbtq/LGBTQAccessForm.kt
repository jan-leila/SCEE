package de.westnordost.streetcomplete.quests.lgbtq

import de.westnordost.streetcomplete.R
import de.westnordost.streetcomplete.quests.AImageListQuestForm
import de.westnordost.streetcomplete.quests.AnswerItem

class LGBTQAccessForm: AImageListQuestForm<LGTBQAccess, LGBTQAccessAnswer>() {

    override val items get() = listOf(
        LGTBQAccess.NO,
        LGTBQAccess.WELCOME,
        LGTBQAccess.PRIMARY,
        LGTBQAccess.ONLY
    ).toItems()

    override val itemsPerRow = 3

    override val otherAnswers get() = listOfNotNull(
        AnswerItem(R.string.quest_lgbtq_access_not_marked) {
            applyAnswer(LGBTQAccessAnswer(null))
        }
    )

    override fun onClickOk(selectedItems: List<LGTBQAccess>) {
        val value = selectedItems.single()
        applyAnswer(LGBTQAccessAnswer(value))
    }
}
