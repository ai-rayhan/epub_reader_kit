/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.example.epub_reader_kit.reader.outline

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.epub.landmarks
import org.readium.r2.shared.publication.epub.pageList
import org.readium.r2.shared.publication.opds.images
import com.example.epub_reader_kit.reader.R
import com.example.epub_reader_kit.reader.databinding.FragmentOutlineBinding
import com.example.epub_reader_kit.reader.reader.ReaderActivity
import com.example.epub_reader_kit.reader.reader.ReaderViewModel
import com.example.epub_reader_kit.reader.utils.viewLifecycle

class OutlineFragment : Fragment() {

    private lateinit var publication: Publication
    private var binding: FragmentOutlineBinding by viewLifecycle()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewModelProvider(requireActivity()).get(ReaderViewModel::class.java).let {
            publication = it.publication
        }

        (activity as ReaderActivity?)?.supportActionBar?.setDisplayHomeAsUpEnabled(true)

        childFragmentManager.setFragmentResultListener(
            OutlineContract.REQUEST_KEY,
            this,
            FragmentResultListener { requestKey, bundle -> setFragmentResult(requestKey, bundle) }
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentOutlineBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.clear()
                }

                override fun onMenuItemSelected(menuItem: android.view.MenuItem): Boolean = false
            },
            viewLifecycleOwner
        )

        val outlines: List<Outline> = when {
            publication.conformsTo(Publication.Profile.EPUB) -> listOf(
                Outline.Contents,
                Outline.Bookmarks,
                Outline.Highlights,
                Outline.PageList,
                Outline.Landmarks
            )
            else -> listOf(Outline.Contents, Outline.Bookmarks)
        }

        binding.outlinePager.adapter = OutlineFragmentStateAdapter(this, publication, outlines)
        TabLayoutMediator(binding.outlineTabLayout, binding.outlinePager) { tab, idx ->
            tab.setText(
                outlines[idx].label
            )
        }.attach()

        val initialOutline = arguments?.getString(ARG_INITIAL_OUTLINE)
        if (initialOutline == INITIAL_BOOKMARKS) {
            val bookmarksTabIndex = outlines.indexOf(Outline.Bookmarks)
            if (bookmarksTabIndex >= 0) {
                binding.outlinePager.setCurrentItem(bookmarksTabIndex, false)
            }
        }
    }

    companion object {
        private const val ARG_INITIAL_OUTLINE = "initial_outline"
        private const val INITIAL_BOOKMARKS = "bookmarks"

        fun createArguments(openBookmarksTab: Boolean = false): Bundle =
            Bundle().apply {
                if (openBookmarksTab) {
                    putString(ARG_INITIAL_OUTLINE, INITIAL_BOOKMARKS)
                }
            }
    }
}

private class OutlineFragmentStateAdapter(
    fragment: Fragment,
    val publication: Publication,
    val outlines: List<Outline>,
) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int {
        return outlines.size
    }

    override fun createFragment(position: Int): Fragment {
        return when (this.outlines[position]) {
            Outline.Bookmarks -> BookmarksFragment()
            Outline.Highlights -> HighlightsFragment()
            Outline.Landmarks -> createLandmarksFragment()
            Outline.Contents -> createContentsFragment()
            Outline.PageList -> createPageListFragment()
        }
    }

    private fun createContentsFragment() =
        NavigationFragment.newInstance(
            when {
                publication.tableOfContents.isNotEmpty() -> publication.tableOfContents
                publication.readingOrder.isNotEmpty() -> publication.readingOrder
                publication.images.isNotEmpty() -> publication.images
                else -> mutableListOf()
            }
        )

    private fun createPageListFragment() =
        NavigationFragment.newInstance(publication.pageList)

    private fun createLandmarksFragment() =
        NavigationFragment.newInstance(publication.landmarks)
}

private enum class Outline(val label: Int) {
    Contents(R.string.contents_tab_label),
    Bookmarks(R.string.bookmarks_tab_label),
    Highlights(R.string.highlights_tab_label),
    PageList(R.string.pagelist_tab_label),
    Landmarks(R.string.landmarks_tab_label),
}
