/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.example.epub_reader_kit.reader.reader

import android.app.AlertDialog
import android.app.SearchManager
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.graphics.RectF
import android.os.Bundle
import android.os.Build
import android.view.ActionMode
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ListPopupWindow
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color as ComposeColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.readium.navigator.media.tts.android.AndroidTtsEngine
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.OverflowableNavigator
import org.readium.r2.navigator.SelectableNavigator
import org.readium.r2.navigator.VisualNavigator
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.navigator.util.BaseActionModeCallback
import org.readium.r2.navigator.util.DirectionalNavigationAdapter
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.util.Language
import com.example.epub_reader_kit.reader.R
import com.example.epub_reader_kit.reader.data.model.Highlight
import com.example.epub_reader_kit.reader.databinding.FragmentReaderBinding
import org.readium.r2.navigator.preferences.Theme
import com.example.epub_reader_kit.reader.reader.tts.TtsControls
import com.example.epub_reader_kit.reader.reader.tts.TtsPreferencesBottomSheetDialogFragment
import com.example.epub_reader_kit.reader.reader.tts.TtsViewModel
import com.example.epub_reader_kit.reader.reader.preferences.MainPreferencesBottomSheetDialogFragment
import com.example.epub_reader_kit.reader.utils.extensions.asStateWhenStarted
import com.example.epub_reader_kit.reader.utils.clearPadding
import com.example.epub_reader_kit.reader.utils.extensions.confirmDialog
import com.example.epub_reader_kit.reader.utils.extensions.throttleLatest
import com.example.epub_reader_kit.reader.utils.hideSystemUi
import com.example.epub_reader_kit.reader.utils.observeWhenStarted
import com.example.epub_reader_kit.reader.utils.padSystemUi
import com.example.epub_reader_kit.reader.utils.showSystemUi
import com.example.epub_reader_kit.reader.utils.toggleSystemUi
import com.example.epub_reader_kit.reader.utils.viewLifecycle

/*
 * Base reader fragment class
 *
 * Provides common menu items and saves last location on stop.
 */
@OptIn(ExperimentalReadiumApi::class)
abstract class VisualReaderFragment : BaseReaderFragment() {

    protected var binding: FragmentReaderBinding by viewLifecycle()

    private lateinit var navigatorFragment: Fragment
    private var isReaderChromeVisible by mutableStateOf(false)
    private var isThemePanelVisible by mutableStateOf(false)
    private var isFontPanelVisible by mutableStateOf(false)
    private var isBookmarked by mutableStateOf(false)
    private var isReadingChapter by mutableStateOf(false)
    private var isSpeechPlaying by mutableStateOf(false)
    private var isFullscreen by mutableStateOf(false)
    private var isTtsBarVisible by mutableStateOf(false)   // selected-text TTS bar
    private var isTtsListening by mutableStateOf(false)    // is selected-text TTS playing

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * When true, the user won't be able to interact with the navigator.
     */
    private var disableTouches by mutableStateOf(false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navigatorFragment = navigator as Fragment

        (navigator as OverflowableNavigator).apply {
            // This will automatically turn pages when tapping the screen edges or arrow keys.
            addInputListener(DirectionalNavigationAdapter(this))
        }

        (navigator as VisualNavigator).apply {
            addInputListener(object : InputListener {
                override fun onTap(event: TapEvent): Boolean {
                    requireActivity().toggleSystemUi()
                    return true
                }
            })
        }

        setupObservers()

        childFragmentManager.addOnBackStackChangedListener {
            updateSystemUiVisibility()
        }
        binding.fragmentReaderContainer.setOnApplyWindowInsetsListener { container, insets ->
            updateSystemUiPadding(container, insets)
            insets
        }

        binding.overlay.setContent {
            LaunchedEffect(isReaderChromeVisible) {
                if (!isReaderChromeVisible) {
                    isThemePanelVisible = false
                }
            }

            if (disableTouches) {
                // Add an invisible box on top of the navigator to intercept touch gestures.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures {
                                requireActivity().toggleSystemUi()
                            }
                        }
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding(),
                content = { Overlay() }
            )
        }

        @Suppress("DEPRECATION")
        val visibility = requireActivity().window.decorView.systemUiVisibility
        isReaderChromeVisible = (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0

        @Suppress("DEPRECATION")
        requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener { newVisibility ->
            isReaderChromeVisible = (newVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0
        }

        val menuHost: MenuHost = requireActivity()

        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    menu.findItem(R.id.tts).isVisible = (model.tts != null)
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    when (menuItem.itemId) {
                        R.id.tts -> {
                            checkNotNull(model.tts).start(navigator)
                            return true
                        }
                    }
                    return false
                }
            },
            viewLifecycleOwner
        )

        model.visualFragmentChannel.receive(viewLifecycleOwner) { event ->
            when (event) {
                is ReaderViewModel.VisualFragmentCommand.ShowPopup ->
                    showFootnotePopup(event.text)
            }
        }
    }

    @Composable
    private fun BoxScope.Overlay() {
        if (isReaderChromeVisible) {
            ReaderChrome(
                title = model.publication.metadata.title ?: "",
                onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                onSearch = { (this@VisualReaderFragment as? EpubReaderFragment)?.openSearchDialogFromChrome() },
                onBookmark = {
                    isBookmarked = !isBookmarked
                    model.insertBookmark(navigator.currentLocator.value)
                },
                onOpenOutline = {
                    model.activityChannel.send(
                        ReaderViewModel.ActivityCommand.OpenOutlineRequested
                    )
                },
                onOpenSettings = {
                    MainPreferencesBottomSheetDialogFragment()
                        .show(childFragmentManager, "Settings")
                },
                onToggleThemePanel = {
                    isThemePanelVisible = !isThemePanelVisible
                    if (isThemePanelVisible) isFontPanelVisible = false
                },
                onToggleFontPanel = {
                    isFontPanelVisible = !isFontPanelVisible
                    if (isFontPanelVisible) isThemePanelVisible = false
                },
                onToggleFullscreen = { isFullscreen = !isFullscreen },
                onToggleChapterReading = {
                    isReadingChapter = !isReadingChapter
                    if (isReadingChapter) {
                        isSpeechPlaying = true
                        isTtsBarVisible = false   // close selection-TTS bar when chapter reading starts
                        isTtsListening = false
                    }
                },
                isThemePanelVisible = isThemePanelVisible,
                isFontPanelVisible = isFontPanelVisible,
                isBookmarked = isBookmarked,
                isReadingChapter = isReadingChapter,
                isSpeechPlaying = isSpeechPlaying,
                isFullscreen = isFullscreen,
                isTtsBarVisible = isTtsBarVisible,
                isTtsListening = isTtsListening,
                onCloseTtsBar = {
                    isTtsBarVisible = false
                    isTtsListening = false
                },
                onToggleTtsListen = { isTtsListening = !isTtsListening },
                selectedTheme = (this@VisualReaderFragment as? EpubReaderFragment)?.currentThemeFromPreferences(),
                onThemeSelected = { theme ->
                    (this@VisualReaderFragment as? EpubReaderFragment)?.applyThemeFromChrome(theme)
                    isThemePanelVisible = false
                },
                ttsModel = model.tts,
                onTtsPreferences = {
                    TtsPreferencesBottomSheetDialogFragment()
                        .show(childFragmentManager, "TtsSettings")
                }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    //  ReaderChrome — matches HTML design exactly
    // ─────────────────────────────────────────────────────────────────────
    @Composable
    private fun BoxScope.ReaderChrome(
        title: String,
        onBack: () -> Unit,
        onSearch: () -> Unit,
        onBookmark: () -> Unit,
        onOpenOutline: () -> Unit,
        onOpenSettings: () -> Unit,
        onToggleThemePanel: () -> Unit,
        onToggleFontPanel: () -> Unit,
        onToggleFullscreen: () -> Unit,
        onToggleChapterReading: () -> Unit,
        isThemePanelVisible: Boolean,
        isFontPanelVisible: Boolean,
        isBookmarked: Boolean,
        isReadingChapter: Boolean,
        isSpeechPlaying: Boolean,
        isFullscreen: Boolean,
        isTtsBarVisible: Boolean,
        isTtsListening: Boolean,
        onCloseTtsBar: () -> Unit,
        onToggleTtsListen: () -> Unit,
        selectedTheme: Theme?,
        onThemeSelected: (Theme) -> Unit,
        ttsModel: TtsViewModel?,
        onTtsPreferences: () -> Unit,
    ) {
        // Design tokens matching the HTML
        val bgColor       = ComposeColor.White
        val primaryColor  = ComposeColor(0xFF4B39EF)
        val textColor     = ComposeColor(0xFF14181B)
        val text2Color    = ComposeColor(0xFF57636C)
        val borderColor   = ComposeColor(0x14000000)  // rgba(0,0,0,0.08)
        val alternateColor = ComposeColor(0xFFE0E3E7)
        val goldColor     = ComposeColor(0xFFFFD700)

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // ── TOP APP BAR ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .shadow(elevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = textColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Title (centered)
                    Text(
                        text = title,
                        color = textColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Right actions
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search
                        AppBarIconBtn(
                            icon = Icons.Default.Search,
                            description = "Search",
                            tint = textColor,
                            onClick = onSearch
                        )
                        // TTS / headphone
                        AppBarIconBtn(
                            icon = Icons.Default.Headphones,
                            description = "Read Aloud",
                            tint = if (isReadingChapter) primaryColor else textColor,
                            onClick = onToggleChapterReading
                        )
                        // Bookmark — stays textColor when active (matches HTML)
                        AppBarIconBtn(
                            icon = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            description = "Bookmark",
                            tint = textColor,
                            onClick = onBookmark
                        )
                        // More (settings)
                        AppBarIconBtn(
                            icon = Icons.Default.MoreVert,
                            description = "More",
                            tint = textColor,
                            onClick = onOpenSettings
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── BOTTOM AREA ──────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .shadow(elevation = 1.dp)
            ) {

                // 0. TTS Selection Bar (shown when user selects text, not reading chapter)
                AnimatedVisibility(
                    visible = isTtsBarVisible && !isReadingChapter,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Close button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(alternateColor.copy(alpha = 0.5f))
                                .clickable(onClick = onCloseTtsBar),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Listen / Stop button (flex: 1)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isTtsListening) ComposeColor(0xFFF44336) else primaryColor)
                                .clickable(onClick = onToggleTtsListen),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isTtsListening) Icons.Default.Stop else Icons.Default.VolumeUp,
                                    contentDescription = if (isTtsListening) "Stop" else "Listen",
                                    tint = ComposeColor.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = if (isTtsListening) "Stop" else "Listen",
                                    color = ComposeColor.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Settings button
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(alternateColor.copy(alpha = 0.5f))
                                .clickable(onClick = onTtsPreferences),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "TTS Settings",
                                tint = textColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // 1. Speech Player Bar (when reading chapter) ─────────────
                AnimatedVisibility(
                    visible = isReadingChapter && ttsModel != null,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    ttsModel?.let { tts ->
                        val showControls by tts.showControls.asStateWhenStarted()
                        val isPlaying by tts.isPlaying.asStateWhenStarted()
                        if (showControls) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(width = 1.dp, color = borderColor, shape = RoundedCornerShape(0.dp))
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(alternateColor.copy(alpha = 0.4f))
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Settings
                                        SpeechBtn(
                                            icon = Icons.Default.Settings,
                                            desc = "Settings",
                                            tint = text2Color,
                                            onClick = onTtsPreferences
                                        )
                                        // Previous sentence
                                        SpeechBtn(
                                            icon = Icons.Default.SkipPrevious,
                                            desc = "Previous",
                                            tint = text2Color,
                                            onClick = { tts.previous() }
                                        )
                                        // Play / Pause
                                        SpeechBtn(
                                            icon = if (isSpeechPlaying && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            desc = if (isSpeechPlaying && isPlaying) "Pause" else "Play",
                                            tint = textColor,
                                            size = 28.dp,
                                            onClick = { if (isPlaying) tts.pause() else tts.play() }
                                        )
                                        // Next sentence
                                        SpeechBtn(
                                            icon = Icons.Default.SkipNext,
                                            desc = "Next",
                                            tint = text2Color,
                                            onClick = { tts.next() }
                                        )
                                        // Stop
                                        SpeechBtn(
                                            icon = Icons.Default.Stop,
                                            desc = "Stop",
                                            tint = text2Color,
                                            onClick = {
                                                tts.stop()
                                                if (isReadingChapter) {
                                                    onToggleChapterReading()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Theme Widget ─────────────────────────────────────────
                AnimatedVisibility(
                    visible = isThemePanelVisible && !isReadingChapter,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Brightness slider row
                        var brightness by remember { mutableFloatStateOf(0.8f) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Brightness",
                                tint = primaryColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Slider(
                                value = brightness,
                                onValueChange = { brightness = it },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = primaryColor,
                                    activeTrackColor = primaryColor,
                                    inactiveTrackColor = alternateColor
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = "Max brightness",
                                tint = primaryColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Theme circles row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(
                                Triple("Light", Theme.LIGHT, ComposeColor.White),
                                Triple("Sepia", Theme.SEPIA, ComposeColor(0xFFF5DEB3)),
                                Triple("Dark", Theme.DARK, ComposeColor.Black)
                            ).forEach { (label, theme, circleColor) ->
                                val isSelected = selectedTheme == theme
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(circleColor)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.5.dp,
                                                color = if (isSelected) primaryColor else alternateColor,
                                                shape = CircleShape
                                            )
                                            .clickable { onThemeSelected(theme) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = if (theme == Theme.DARK) ComposeColor.White else ComposeColor.Black,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        color = if (isSelected) primaryColor else text2Color,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Font Widget ──────────────────────────────────────────
                AnimatedVisibility(
                    visible = isFontPanelVisible && !isReadingChapter,
                    enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                    exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        // Font size slider
                        var fontSize by remember { mutableFloatStateOf(0.4f) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = "Font size small",
                                tint = primaryColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Slider(
                                value = fontSize,
                                onValueChange = { fontSize = it },
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = primaryColor,
                                    activeTrackColor = primaryColor,
                                    inactiveTrackColor = alternateColor
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.TextFields,
                                contentDescription = "Font size large",
                                tint = primaryColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Font family options (scrollable)
                        val fontOptions = listOf("Default", "Serif", "Monospace", "Cursive", "Noto Sans")
                        var selectedFont by remember { mutableStateOf(fontOptions[0]) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            fontOptions.forEach { font ->
                                val isFontSelected = selectedFont == font
                                Text(
                                    text = font,
                                    fontSize = 12.sp,
                                    color = if (isFontSelected) primaryColor else text2Color,
                                    fontWeight = if (isFontSelected) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { selectedFont = font }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Page Slider — hidden when: reading chapter, OR theme/font panel visible
                //    Matches HTML: pageIndicator { display: none } when theme/font widget visible
                if (!isReadingChapter && !isThemePanelVisible && !isFontPanelVisible && !isTtsBarVisible) {
                    var pageProgress by remember { mutableFloatStateOf(0f) }
                    Slider(
                        value = pageProgress,
                        onValueChange = { pageProgress = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 0.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = goldColor,
                            activeTrackColor = goldColor,
                            inactiveTrackColor = alternateColor
                        )
                    )
                }

                // 5. Bottom Icons Row — hidden when: reading chapter OR tts bar visible
                if (!isReadingChapter && !isTtsBarVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 12.dp, top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Contents — use FormatListBulleted (list with lines, matches HTML)
                        BottomNavIconBtn(
                            icon = Icons.Default.FormatListBulleted,
                            label = "Contents",
                            tint = text2Color,
                            onClick = onOpenOutline
                        )
                        // Bookmarks
                        BottomNavIconBtn(
                            icon = Icons.Default.Bookmark,
                            label = "Bookmarks",
                            tint = text2Color,
                            onClick = onBookmark
                        )
                        // Fullscreen
                        BottomNavIconBtn(
                            icon = if (isFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                            label = "Full Screen",
                            tint = text2Color,
                            onClick = onToggleFullscreen
                        )
                        // Rotation  (reusing RotateRight as rotation icon)
                        BottomNavIconBtn(
                            icon = Icons.Default.RotateRight,
                            label = "Rotation",
                            tint = text2Color,
                            onClick = { /* rotation toggle */ }
                        )
                        // Brightness / Theme
                        BottomNavIconBtn(
                            icon = Icons.Default.WbSunny,
                            label = "Brightness",
                            tint = if (isThemePanelVisible) primaryColor else text2Color,
                            onClick = onToggleThemePanel
                        )
                        // Font
                        BottomNavIconBtn(
                            icon = Icons.Default.TextFields,
                            label = "Font",
                            tint = if (isFontPanelVisible) primaryColor else text2Color,
                            onClick = onToggleFontPanel
                        )
                    }
                }
            }
        }
    }

    // ── Small composable helpers ──────────────────────────────────────────

    @Composable
    private fun AppBarIconBtn(
        icon: ImageVector,
        description: String,
        tint: ComposeColor,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(24.dp)
            )
        }
    }

    @Composable
    private fun BottomNavIconBtn(
        icon: ImageVector,
        label: String,
        tint: ComposeColor,
        onClick: () -> Unit,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = ComposeColor(0xFF57636C),
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }

    @Composable
    private fun SpeechBtn(
        icon: ImageVector,
        desc: String,
        tint: ComposeColor,
        size: androidx.compose.ui.unit.Dp = 24.dp,
        onClick: () -> Unit,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = desc,
                tint = tint,
                modifier = Modifier.size(size)
            )
        }
    }

    @Composable
    private fun BottomBarItem(
        iconRes: Int,
        label: String,
        onClick: () -> Unit,
        accentColor: ComposeColor = ComposeColor.White,
        isActive: Boolean = false,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isActive) accentColor.copy(alpha = 0.12f) else ComposeColor.Transparent
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = label,
                tint = accentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = accentColor,
                fontSize = 10.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                letterSpacing = 0.3.sp
            )
        }
    }

    @Composable
    private fun ThemeChip(
        label: String,
        theme: Theme,
        selectedTheme: Theme?,
        onThemeSelected: (Theme) -> Unit,
        accentColor: ComposeColor = ComposeColor(0xFFFFA726),
    ) {
        val isSelected = selectedTheme == theme
        val swatchColor = when (theme) {
            Theme.LIGHT -> ComposeColor(0xFFF5F5F5)
            Theme.SEPIA -> ComposeColor(0xFFE8D5B0)
            Theme.DARK  -> ComposeColor(0xFF1A1A2E)
        }
        val swatchGradientEnd = when (theme) {
            Theme.LIGHT -> ComposeColor(0xFFDDDDDD)
            Theme.SEPIA -> ComposeColor(0xFFCFAE77)
            Theme.DARK  -> ComposeColor(0xFF0D0D1A)
        }
        val checkColor = when (theme) {
            Theme.DARK -> ComposeColor.White
            else       -> ComposeColor(0xFF222222)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .width(80.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onThemeSelected(theme) }
                .padding(vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .shadow(elevation = if (isSelected) 8.dp else 2.dp, shape = CircleShape)
                    .clip(CircleShape)
                    .background(brush = Brush.radialGradient(colors = listOf(swatchColor, swatchGradientEnd)))
                    .then(
                        if (isSelected) Modifier.border(2.dp, accentColor, CircleShape)
                        else Modifier.border(1.dp, ComposeColor.White.copy(alpha = 0.15f), CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Text(
                        text = "\u2713",
                        color = checkColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = if (isSelected) accentColor else ComposeColor.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                letterSpacing = 0.2.sp
            )
        }
    }


    @Composable
    private fun IconButton(
        iconRes: Int,
        contentDescription: String,
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Box(
            modifier = modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(ComposeColor.White.copy(alpha = 0.1f))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = ComposeColor.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                navigator.currentLocator
                    .onEach { model.saveProgression(it) }
                    .launchIn(this)
            }
        }

        (navigator as? DecorableNavigator)
            ?.addDecorationListener("highlights", decorationListener)

        viewLifecycleOwner.lifecycleScope.launch {
            setupHighlights(viewLifecycleOwner.lifecycleScope)
            setupSearch(viewLifecycleOwner.lifecycleScope)
            setupTts()
        }
    }

    private suspend fun setupHighlights(scope: CoroutineScope) {
        (navigator as? DecorableNavigator)?.let { navigator ->
            model.highlightDecorations
                .onEach { navigator.applyDecorations(it, "highlights") }
                .launchIn(scope)
        }
    }

    private suspend fun setupSearch(scope: CoroutineScope) {
        (navigator as? DecorableNavigator)?.let { navigator ->
            model.searchDecorations
                .onEach { navigator.applyDecorations(it, "search") }
                .launchIn(scope)
        }
    }

    /**
     * Setup text-to-speech observers, if available.
     */
    private suspend fun setupTts() {
        model.tts?.apply {
            events
                .observeWhenStarted(viewLifecycleOwner) { event ->
                    when (event) {
                        is TtsViewModel.Event.OnError -> {
                            showError(event.error.toUserError())
                        }
                        is TtsViewModel.Event.OnMissingVoiceData ->
                            confirmAndInstallTtsVoice(event.language)
                    }
                }

            // Navigate to the currently spoken word.
            // This will automatically turn pages when needed.
            position
                .filterNotNull()
                // Improve performances by throttling the moves to maximum one per second.
                .throttleLatest(1.seconds)
                .observeWhenStarted(viewLifecycleOwner) { locator ->
                    navigator.go(locator, animated = false)
                }

            // Prevent interacting with the publication (including page turns) while the TTS is
            // playing.
            isPlaying
                .observeWhenStarted(viewLifecycleOwner) { isPlaying ->
                    disableTouches = isPlaying
                }

            // Highlight the currently spoken utterance.
            (navigator as? DecorableNavigator)?.let { navigator ->
                highlight
                    .observeWhenStarted(viewLifecycleOwner) { locator ->
                        val decoration = locator?.let {
                            Decoration(
                                id = "tts",
                                locator = it,
                                style = Decoration.Style.Highlight(tint = Color.RED)
                            )
                        }
                        navigator.applyDecorations(listOfNotNull(decoration), "tts")
                    }
            }
        }
    }

    /**
     * Confirms with the user if they want to download the TTS voice data for the given language.
     */
    private suspend fun confirmAndInstallTtsVoice(language: Language) {
        val activity = activity ?: return
        model.tts ?: return

        if (
            activity.confirmDialog(
                getString(
                    R.string.tts_error_language_support_incomplete,
                    language.locale.displayLanguage
                )
            )
        ) {
            AndroidTtsEngine.requestInstallVoice(activity)
        }
    }

    override fun go(locator: Locator, animated: Boolean) {
        model.tts?.stop()
        super.go(locator, animated)
    }

    override fun onDestroyView() {
        @Suppress("DEPRECATION")
        requireActivity().window.decorView.setOnSystemUiVisibilityChangeListener(null)
        (navigator as? DecorableNavigator)?.removeDecorationListener(decorationListener)
        super.onDestroyView()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        setMenuVisibility(!hidden)
        requireActivity().invalidateOptionsMenu()
    }

    // DecorableNavigator.Listener

    private val decorationListener by lazy { DecorationListener() }

    inner class DecorationListener : DecorableNavigator.Listener {
        override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
            val decoration = event.decoration
            // We stored the highlight's database ID in the `Decoration.extras` map, for
            // easy retrieval. You can store arbitrary information in the map.
            val id = (decoration.extras["id"] as Long)
                .takeIf { it > 0 } ?: return false

            // This listener will be called when tapping on any of the decorations in the
            // "highlights" group. To differentiate between the page margin icon and the
            // actual highlight, we check for the type of `decoration.style`. But you could
            // use any other information, including the decoration ID or the extras bundle.
            if (decoration.style is DecorationStyleAnnotationMark) {
                showAnnotationPopup(id)
            } else {
                event.rect?.let { rect ->
                    val isUnderline = (decoration.style is Decoration.Style.Underline)
                    showHighlightPopup(
                        rect,
                        style = if (isUnderline) {
                            Highlight.Style.UNDERLINE
                        } else {
                            Highlight.Style.HIGHLIGHT
                        },
                        highlightId = id
                    )
                }
            }

            return true
        }
    }

    // Highlights

    private var popupWindow: PopupWindow? = null
    private var mode: ActionMode? = null

    // Available tint colors for highlight and underline annotations.
    private val highlightTints = mapOf</*@IdRes*/ Int, /*@ColorInt*/ Int>(
        R.id.red to Color.rgb(247, 124, 124),
        R.id.green to Color.rgb(173, 247, 123),
        R.id.blue to Color.rgb(124, 198, 247),
        R.id.yellow to Color.rgb(249, 239, 125),
        R.id.purple to Color.rgb(182, 153, 255)
    )

    val customSelectionActionModeCallback: ActionMode.Callback by lazy { SelectionActionModeCallback() }

    private inner class SelectionActionModeCallback : BaseActionModeCallback() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_action_mode, menu)
            if (navigator is DecorableNavigator) {
                menu.findItem(R.id.highlight).isVisible = true
                menu.findItem(R.id.underline).isVisible = true
                menu.findItem(R.id.note).isVisible = true
                menu.findItem(R.id.copy).isVisible = true
                menu.findItem(R.id.web_search).isVisible = true
            }
            // Show TTS selection bar when user selects text
            if (!isReadingChapter) {
                isTtsBarVisible = true
                isTtsListening = false
            }
            return true
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            super.onDestroyActionMode(mode)
            // Don't hide TTS bar on destroy — user may still want to Listen/Stop
            // It closes via the X button: onCloseTtsBar
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            when (item.itemId) {
                R.id.highlight -> showHighlightPopupWithStyle(Highlight.Style.HIGHLIGHT)
                R.id.underline -> showHighlightPopupWithStyle(Highlight.Style.UNDERLINE)
                R.id.note -> showAnnotationPopup()
                R.id.copy -> copySelectionToClipboard()
                R.id.web_search -> searchSelectionOnWeb()
                else -> return false
            }

            mode.finish()
            return true
        }
    }

    private fun showHighlightPopupWithStyle(style: Highlight.Style) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Get the rect of the current selection to know where to position the highlight
            // popup.
            (navigator as? SelectableNavigator)?.currentSelection()?.rect?.let { selectionRect ->
                showHighlightPopup(selectionRect, style)
            }
        }
    }

    private fun showHighlightPopup(rect: RectF, style: Highlight.Style, highlightId: Long? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            if (popupWindow?.isShowing == true) return@launch

            model.activeHighlightId.value = highlightId

            val isReverse = (rect.top > 60)
            val popupView = layoutInflater.inflate(
                if (isReverse) R.layout.view_action_mode_reverse else R.layout.view_action_mode,
                null,
                false
            )
            popupView.measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            popupWindow = PopupWindow(
                popupView,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                isFocusable = true
                setOnDismissListener {
                    model.activeHighlightId.value = null
                }
            }

            val x = rect.left
            val y = if (isReverse) rect.top else rect.bottom + rect.height()

            popupWindow?.showAtLocation(popupView, Gravity.NO_GRAVITY, x.toInt(), y.toInt())

            val highlight = highlightId?.let { model.highlightById(it) }
            popupView.run {
                findViewById<View>(R.id.notch).run {
                    setX(rect.left * 2)
                }

                fun selectTint(view: View) {
                    val tint = highlightTints[view.id] ?: return
                    selectHighlightTint(highlightId, style, tint)
                }

                findViewById<View>(R.id.red).setOnClickListener(::selectTint)
                findViewById<View>(R.id.green).setOnClickListener(::selectTint)
                findViewById<View>(R.id.blue).setOnClickListener(::selectTint)
                findViewById<View>(R.id.yellow).setOnClickListener(::selectTint)
                findViewById<View>(R.id.purple).setOnClickListener(::selectTint)

                findViewById<View>(R.id.annotation).setOnClickListener {
                    popupWindow?.dismiss()
                    showAnnotationPopup(highlightId)
                }
                findViewById<View>(R.id.del).run {
                    visibility = if (highlight != null) View.VISIBLE else View.GONE
                    setOnClickListener {
                        highlightId?.let {
                            model.deleteHighlight(highlightId)
                        }
                        popupWindow?.dismiss()
                        mode?.finish()
                    }
                }
            }
        }
    }

    private fun selectHighlightTint(
        highlightId: Long? = null,
        style: Highlight.Style,
        @ColorInt tint: Int,
    ) =
        viewLifecycleOwner.lifecycleScope.launch {
            if (highlightId != null) {
                model.updateHighlightStyle(highlightId, style, tint)
            } else {
                (navigator as? SelectableNavigator)?.let { navigator ->
                    navigator.currentSelection()?.let { selection ->
                        model.addHighlight(
                            locator = selection.locator,
                            style = style,
                            tint = tint
                        )
                    }
                    navigator.clearSelection()
                }
            }

            popupWindow?.dismiss()
            mode?.finish()
        }

    private fun showAnnotationPopup(highlightId: Long? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            val activity = activity ?: return@launch
            val view = layoutInflater.inflate(R.layout.popup_note, null, false)
            val note = view.findViewById<EditText>(R.id.note)
            val alert = AlertDialog.Builder(activity)
                .setView(view)
                .create()

            fun dismiss() {
                alert.dismiss()
                mode?.finish()
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(
                        note.applicationWindowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS
                    )
            }

            with(view) {
                val highlight = highlightId?.let { model.highlightById(it) }
                if (highlight != null) {
                    note.setText(highlight.annotation)
                    findViewById<View>(R.id.sidemark).setBackgroundColor(highlight.tint)
                    findViewById<TextView>(R.id.select_text).text =
                        highlight.locator.text.highlight

                    findViewById<TextView>(R.id.positive).setOnClickListener {
                        val text = note.text.toString()
                        model.updateHighlightAnnotation(highlight.id, annotation = text)
                        dismiss()
                    }
                } else {
                    val tint = highlightTints.values.random()
                    findViewById<View>(R.id.sidemark).setBackgroundColor(tint)
                    val navigator =
                        navigator as? SelectableNavigator ?: return@launch
                    val selection = navigator.currentSelection() ?: return@launch
                    navigator.clearSelection()
                    findViewById<TextView>(R.id.select_text).text =
                        selection.locator.text.highlight

                    findViewById<TextView>(R.id.positive).setOnClickListener {
                        model.addHighlight(
                            locator = selection.locator,
                            style = Highlight.Style.HIGHLIGHT,
                            tint = tint,
                            annotation = note.text.toString()
                        )
                        dismiss()
                    }
                }

                findViewById<TextView>(R.id.negative).setOnClickListener {
                    dismiss()
                }
            }

            alert.show()
        }
    }

    private fun copySelectionToClipboard() {
        viewLifecycleOwner.lifecycleScope.launch {
            val navigator = navigator as? SelectableNavigator ?: return@launch
            val selection = navigator.currentSelection() ?: return@launch
            val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("selected_text", selection.locator.text.highlight.toString())
            clipboard.setPrimaryClip(clip)
            
            //Only show Toast in Android 12L (API level 32) and lower. Android 13 and higher has standard feedback when content enters the clipboard
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2)
                Toast.makeText(context, R.string.copied, Toast.LENGTH_SHORT).show()

            navigator.clearSelection()
        }
    }

    private fun searchSelectionOnWeb() {
        viewLifecycleOwner.lifecycleScope.launch {
            val navigator = navigator as? SelectableNavigator ?: return@launch
            val selection = navigator.currentSelection() ?: return@launch

            if (selection.locator.text.toString().isNotEmpty()) {
                val query = selection.locator.text.highlight.toString()
                val webSearchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                }
                startActivity(webSearchIntent)

                navigator.clearSelection()
            } else {
                Toast.makeText(context, "No text selected for web search", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showFootnotePopup(
        text: CharSequence,
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            // Initialize a new instance of LayoutInflater service
            val inflater =
                requireActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

            // Inflate the custom layout/view
            val customView = inflater.inflate(R.layout.popup_footnote, null)

            // Initialize a new instance of popup window
            val mPopupWindow = PopupWindow(
                customView,
                ListPopupWindow.WRAP_CONTENT,
                ListPopupWindow.WRAP_CONTENT
            )
            mPopupWindow.isOutsideTouchable = true
            mPopupWindow.isFocusable = true

            // Set an elevation value for popup window
            // Call requires API level 21
            mPopupWindow.elevation = 5.0f

            val textView = customView.findViewById(R.id.footnote) as TextView
            textView.text = text

            // Get a reference for the custom view close button
            val closeButton = customView.findViewById(R.id.ib_close) as ImageButton

            // Set a click listener for the popup window close button
            closeButton.setOnClickListener {
                // Dismiss the popup window
                mPopupWindow.dismiss()
            }

            // Finally, show the popup window at the center location of root relative layout
            // FIXME: should anchor on noteref and be scrollable if the note is too long.
            mPopupWindow.showAtLocation(
                requireView(),
                Gravity.CENTER,
                0,
                0
            )
        }
    }

    fun updateSystemUiVisibility() {
        if (navigatorFragment.isHidden) {
            requireActivity().showSystemUi()
        } else {
            requireActivity().hideSystemUi()
        }

        requireView().requestApplyInsets()
    }

    private fun updateSystemUiPadding(container: View, insets: WindowInsets) {
        if (navigatorFragment.isHidden) {
            container.padSystemUi(insets, requireActivity() as AppCompatActivity)
        } else {
            container.clearPadding()
        }
    }
}

/**
 * Decoration Style for a page margin icon.
 *
 * This is an example of a custom Decoration Style declaration.
 */
@Parcelize
data class DecorationStyleAnnotationMark(@ColorInt val tint: Int) : Decoration.Style

/**
 * Decoration Style for a page number label.
 *
 * This is an example of a custom Decoration Style declaration.
 *
 * @param label Page number label as declared in the `page-list` link object.
 */
@Parcelize
data class DecorationStylePageNumber(val label: String) : Decoration.Style
