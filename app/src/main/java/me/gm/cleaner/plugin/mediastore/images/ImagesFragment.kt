/*
 * Copyright 2021 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.cleaner.plugin.mediastore.images

import android.os.Bundle
import android.transition.TransitionInflater
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.app.SharedElementCallback
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.gm.cleaner.plugin.R
import me.gm.cleaner.plugin.databinding.ImagesFragmentBinding
import me.gm.cleaner.plugin.mediastore.MediaStoreFragment
import me.gm.cleaner.plugin.util.initFastScroller

class ImagesFragment : MediaStoreFragment() {
    private val viewModel: ImagesViewModel by activityViewModels()
    private lateinit var list: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = ImagesFragmentBinding.inflate(inflater)

        val adapter = ImagesAdapter(this)
        list = binding.list
        list.adapter = adapter
        list.layoutManager = GridLayoutManager(requireContext(), 3)
        list.setHasFixedSize(true)
        list.initFastScroller()
        lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.images.collect { images ->
                    adapter.submitList(images) {
                        if (images.isEmpty()) {
                            // No image
                            startPostponedEnterTransition()
                        }
                    }
                }
            }
        }

        prepareTransitions()
        postponeEnterTransition()
        return binding.root
    }

    /**
     * Prepares the shared element transition to the pager fragment, as well as the other transitions
     * that affect the flow.
     */
    private fun prepareTransitions() {
        exitTransition = TransitionInflater.from(requireContext())
            .inflateTransition(R.transition.grid_exit_transition)

        // A similar mapping is set at the ImagePagerFragment with a setEnterSharedElementCallback.
        setExitSharedElementCallback(object : SharedElementCallback() {
            override fun onMapSharedElements(
                names: List<String>, sharedElements: MutableMap<String, View>
            ) {
                // Locate the ViewHolder for the clicked position.
                val selectedViewHolder =
                    list.findViewHolderForAdapterPosition(viewModel.currentPosition) ?: return

                // Map the first shared element name to the child ImageView.
                sharedElements[names[0]] = selectedViewHolder.itemView.findViewById(R.id.image)
            }
        })
    }

    override fun onRequestPermissionsSuccess(
        permissions: Set<String>, savedInstanceState: Bundle?
    ) {
        super.onRequestPermissionsSuccess(permissions, savedInstanceState)
        savedInstanceState ?: viewModel.loadImages()
        scrollToPosition()
    }

    /**
     * Scrolls the recycler view to show the last viewed item in the grid. This is important when
     * navigating back from the grid.
     */
    private fun scrollToPosition() {
        list.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View, left: Int, top: Int, right: Int, bottom: Int,
                oldLeft: Int, oldTop: Int, oldRight: Int, oldBottom: Int
            ) {
                list.removeOnLayoutChangeListener(this)
                val layoutManager = list.layoutManager ?: return
                val viewAtPosition = layoutManager.findViewByPosition(viewModel.currentPosition)
                // Scroll to position if the view for the current position is null (not currently part of
                // layout manager children), or it's not completely visible.
                if (viewAtPosition == null ||
                    layoutManager.isViewPartiallyVisible(viewAtPosition, false, true)
                ) {
                    list.post { layoutManager.scrollToPosition(viewModel.currentPosition) }
                }
            }
        })
    }

    // TODO: refresh by media scanner
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.i(javaClass.simpleName, "useful overriding method")
        return super.onOptionsItemSelected(item)
    }
}
