package de.westnordost.streetcomplete.screens.user.links

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.westnordost.streetcomplete.ui.util.composableContent
import org.koin.androidx.viewmodel.ext.android.viewModel

class LinksFragment : Fragment() {
    private val viewModel by viewModel<LinksViewModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        composableContent { LinksScreen(viewModel) }
}
