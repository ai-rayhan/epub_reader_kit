/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.example.epub_reader_kit.reader.reader

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.EditText
import androidx.annotation.ColorInt
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.fragment.app.FragmentResultListener
import androidx.fragment.app.commit
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.*
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.html.HtmlDecorationTemplate
import org.readium.r2.navigator.html.toCss
import org.readium.r2.navigator.preferences.FontFamily
import org.readium.r2.navigator.preferences.Theme
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.epub.pageList
import com.example.epub_reader_kit.reader.LITERATA
import com.example.epub_reader_kit.reader.R
import com.example.epub_reader_kit.EpubReaderKitPlugin
import com.example.epub_reader_kit.reader.reader.preferences.UserPreferencesViewModel
import com.example.epub_reader_kit.reader.search.SearchFragment
import android.widget.SeekBar
import org.readium.r2.shared.publication.services.positions
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import android.widget.FrameLayout
import org.readium.r2.navigator.epub.EpubPreferencesEditor

@OptIn(ExperimentalReadiumApi::class)
class EpubReaderFragment : VisualReaderFragment() {

    override lateinit var navigator: EpubNavigatorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        val readerData = model.readerInitData as? EpubReaderInitData ?: run {
            // We provide a dummy fragment factory  if the ReaderActivity is restored after the
            // app process was killed because the ReaderRepository is empty. In that case, finish
            // the activity as soon as possible and go back to the previous one.
            childFragmentManager.fragmentFactory = EpubNavigatorFragment.createDummyFactory()
            super.onCreate(savedInstanceState)
            requireActivity().finish()
            return
        }

        childFragmentManager.fragmentFactory =
            readerData.navigatorFactory.createFragmentFactory(
                initialLocator = readerData.initialLocation,
                initialPreferences = readerData.preferencesManager.preferences.value,
                listener = model,
                configuration = EpubNavigatorFragment.Configuration {
                    // To customize the text selection menu.
                    selectionActionModeCallback = customSelectionActionModeCallback

                    // App assets which will be accessible from the EPUB resources.
                    // You can use simple glob patterns, such as "images/.*" to allow several
                    // assets in one go.
                    servedAssets = listOf(
                        // For the custom font Literata.
                        "fonts/.*",
                        // Icon for the annotation side mark, see [annotationMarkTemplate].
                        "annotation-icon.svg"
                    )

                    // Register the HTML templates for our custom decoration styles.
                    decorationTemplates[DecorationStyleAnnotationMark::class] = annotationMarkTemplate()
                    decorationTemplates[DecorationStylePageNumber::class] = pageNumberTemplate()

                    // Declare a custom font family for reflowable EPUBs.
                    addFontFamilyDeclaration(FontFamily.LITERATA) {
                        addFontFace {
                            addSource("fonts/Literata-VariableFont_opsz,wght.ttf")
                            setFontStyle(FontStyle.NORMAL)
                            // Literata is a variable font family, so we can provide a font weight range.
                            setFontWeight(200..900)
                        }
                        addFontFace {
                            addSource("fonts/Literata-Italic-VariableFont_opsz,wght.ttf")
                            setFontStyle(FontStyle.ITALIC)
                            setFontWeight(200..900)
                        }
                    }
                }
            )

        childFragmentManager.setFragmentResultListener(
            SearchFragment::class.java.name,
            this,
            FragmentResultListener { _, result ->
                closeSearchFromChrome()
                BundleCompat.getParcelable(
                    result,
                    SearchFragment::class.java.name,
                    Locator::class.java
                )?.let {
                    navigator.go(it)
                }
            }
        )

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.commitNow {
                add(
                    R.id.fragment_reader_container,
                    EpubNavigatorFragment::class.java,
                    Bundle(),
                    NAVIGATOR_FRAGMENT_TAG
                )
            }
        }
        navigator = childFragmentManager.findFragmentByTag(NAVIGATOR_FRAGMENT_TAG) as EpubNavigatorFragment

        return view
    }

    // Save seekbar snap points from book positions start locator
    private var booksPositionsStartProgression: List<Int> = emptyList()
    // save all book positions
    // to help navigate to these positions
    private var allBookPositions: List<Locator> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBar = view.findViewById<SeekBar>(R.id.readingProgressBar)

        val marginInPx = (12 * resources.displayMetrics.density).toInt()

        ViewCompat.setOnApplyWindowInsetsListener(progressBar) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.updateLayoutParams<FrameLayout.LayoutParams> {
                bottomMargin = systemBars.bottom + marginInPx
            }

            windowInsets
        }

        progressBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, thumbSwiped: Boolean) {
                // Handle progress changes
                if (thumbSwiped && seekBar != null && booksPositionsStartProgression.isNotEmpty()) {

                    val closestPoint = booksPositionsStartProgression.minByOrNull { Math.abs(it - progress) } ?: progress

                    if (progress != closestPoint) {
                        seekBar.progress = closestPoint
                    }
                }

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // handle start
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {

                // Navigate to selected progress
                // When you release your thumb.

                val progressOnThumbRelease = seekBar?.progress ?: return

                val targetPositionLocator = allBookPositions.minByOrNull {
                    val point = ((it.locations.totalProgression ?: 0.0) * 100).toInt()
                    Math.abs(point - progressOnThumbRelease)
                }

                targetPositionLocator?.let {
                    navigator.go(it)
                }
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            // update book positions
            allBookPositions = publication.positions()

            // update seekbar snap points from book positions
            booksPositionsStartProgression = allBookPositions.map {
                ((it.locations.totalProgression ?: 0.0) * 100).toInt()
            }.distinct().sorted()

            // add seekbar max as the last position total progression
            // This enables seekbar to show accurate complete status
            // As progression does not get to 100 percent fully
            if (booksPositionsStartProgression.isNotEmpty()) {
                progressBar.max = booksPositionsStartProgression.last()
            }

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigator.currentLocator.collect { locator ->
                    val progress = locator?.locations?.totalProgression ?: 0.0
                    val percentage = (progress * 100).toInt().coerceIn(0, 100)
                    progressBar.progress = percentage
                    EpubReaderKitPlugin.emitEpubPageChanged(percentage)
                }
            }
        }

        @Suppress("Unchecked_cast")
        (model.settings as UserPreferencesViewModel<EpubSettings, EpubPreferences>)
            .bind(navigator, viewLifecycleOwner)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Display page number labels if the book contains a `page-list` navigation document.
                (navigator as? DecorableNavigator)?.applyPageNumberDecorations()
            }
        }
    }

    /**
     * Will display margin labels next to page numbers in an EPUB publication with a `page-list`
     * navigation document.
     *
     * See http://kb.daisy.org/publishing/docs/navigation/pagelist.html
     */
    private suspend fun DecorableNavigator.applyPageNumberDecorations() {
        val decorations = publication.pageList
            .mapIndexedNotNull { index, link ->
                val label = link.title ?: return@mapIndexedNotNull null
                val locator = publication.locatorFromLink(link) ?: return@mapIndexedNotNull null

                Decoration(
                    id = "page-$index",
                    locator = locator,
                    style = DecorationStylePageNumber(label = label)
                )
            }

        applyDecorations(decorations, "pageNumbers")
    }

    fun openSearchDialogFromChrome() {
        val input = EditText(requireContext()).apply {
            hint = getString(R.string.epubactivity_search)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.epubactivity_search)
            .setView(input)
            .setPositiveButton(R.string.ok) { _, _ ->
                val query = input.text?.toString()?.trim().orEmpty()
                searchFromChrome(query)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun searchFromChrome(query: String) {
        if (query.isBlank()) return
        showSearchFragment()
        model.search(query)
    }

    fun closeSearchFromChrome() {
        val fragment = childFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)
        if (fragment != null && !navigator.isHidden) return

        childFragmentManager.popBackStack(SEARCH_FRAGMENT_TAG, 0)
    }

    fun currentThemeFromPreferences(): Theme? {
        val settings = model.settings as? UserPreferencesViewModel<EpubSettings, EpubPreferences> ?: return null
        val editor = settings.editor.value as? EpubPreferencesEditor ?: return null
        return editor.theme.value ?: editor.theme.effectiveValue
    }

    fun applyThemeFromChrome(theme: Theme) {
        val settings = model.settings as? UserPreferencesViewModel<EpubSettings, EpubPreferences> ?: return
        val editor = settings.editor.value as? EpubPreferencesEditor ?: return
        editor.theme.set(theme)
        settings.commit()
    }

    private fun showSearchFragment() {
        val current = childFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)
        if (current != null) return

        childFragmentManager.commit {
            childFragmentManager.findFragmentByTag(SEARCH_FRAGMENT_TAG)?.let { remove(it) }
            add(
                R.id.fragment_reader_container,
                SearchFragment::class.java,
                Bundle(),
                SEARCH_FRAGMENT_TAG
            )
            hide(navigator)
            addToBackStack(SEARCH_FRAGMENT_TAG)
        }
    }

    companion object {
        private const val SEARCH_FRAGMENT_TAG = "search"
        private const val NAVIGATOR_FRAGMENT_TAG = "navigator"
    }
}

// Examples of HTML templates for custom Decoration Styles.

/**
 * This Decorator Style will display a tinted "pen" icon in the page margin to show that a highlight
 * has an associated note.
 *
 * Note that the icon is served from the app assets folder.
 */
private fun annotationMarkTemplate(@ColorInt defaultTint: Int = Color.YELLOW): HtmlDecorationTemplate {
    val className = "testapp-annotation-mark"
    val iconUrl = checkNotNull(EpubNavigatorFragment.assetUrl("annotation-icon.svg"))
    return HtmlDecorationTemplate(
        layout = HtmlDecorationTemplate.Layout.BOUNDS,
        width = HtmlDecorationTemplate.Width.PAGE,
        element = { decoration ->
            val style = decoration.style as? DecorationStyleAnnotationMark
            val tint = style?.tint ?: defaultTint
            // Using `data-activable=1` prevents the whole decoration container from being
            // clickable. Only the icon will respond to activation events.
            """
            <div><div data-activable="1" class="$className" style="background-color: ${tint.toCss()} !important"/></div>"
            """
        },
        stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                width: 30px;
                height: 30px;
                border-radius: 50%;
                background: url('$iconUrl') no-repeat center;
                background-size: auto 50%;
                opacity: 0.8;
            }
            """
    )
}

/**
 * This Decoration Style is used to display the page number labels in the margins, when a book
 * provides a `page-list`. The label is stored in the [DecorationStylePageNumber] itself.
 *
 * See http://kb.daisy.org/publishing/docs/navigation/pagelist.html
 */
private fun pageNumberTemplate(): HtmlDecorationTemplate {
    val className = "testapp-page-number"
    return HtmlDecorationTemplate(
        layout = HtmlDecorationTemplate.Layout.BOUNDS,
        width = HtmlDecorationTemplate.Width.PAGE,
        element = { decoration ->
            val style = decoration.style as? DecorationStylePageNumber

            // Using `var(--RS__backgroundColor)` is a trick to use the same background color as
            // the Readium theme. If we don't set it directly inline in the HTML, it might be
            // forced transparent by Readium CSS.
            """
            <div><span class="$className" style="background-color: var(--RS__backgroundColor) !important">${style?.label}</span></div>"
            """
        },
        stylesheet = """
            .$className {
                float: left;
                margin-left: 8px;
                padding: 0px 4px 0px 4px;
                border: 1px solid;
                border-radius: 20%;
                box-shadow: rgba(50, 50, 93, 0.25) 0px 2px 5px -1px, rgba(0, 0, 0, 0.3) 0px 1px 3px -1px;
                opacity: 0.8;
            }
            """
    )
}
