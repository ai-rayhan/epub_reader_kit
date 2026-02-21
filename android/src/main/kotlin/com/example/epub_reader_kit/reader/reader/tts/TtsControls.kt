/*
 * Copyright 2021 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.example.epub_reader_kit.reader.reader.tts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.epub_reader_kit.reader.R
import com.example.epub_reader_kit.reader.utils.extensions.asStateWhenStarted

/**
 * TTS controls bar displayed at the bottom of the screen when speaking a publication.
 * Redesigned with modern light/white theme and purple primary (#4b39ef).
 */
@Composable
fun TtsControls(
    model: TtsViewModel,
    onPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val showControls by model.showControls.asStateWhenStarted()
    val isPlaying by model.isPlaying.asStateWhenStarted()

    if (showControls) {
        TtsControls(
            playing = isPlaying,
            onPlayPause = { if (isPlaying) model.pause() else model.play() },
            onStop = model::stop,
            onPrevious = model::previous,
            onNext = model::next,
            onPreferences = onPreferences,
            modifier = modifier
        )
    }
}

@Composable
fun TtsControls(
    playing: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPreferences: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primaryColor = Color(0xFF4B39EF)
    val textColor    = Color(0xFF14181B)
    val mutedColor   = Color(0xFF57636C)
    val bgColor      = Color.White
    val surfaceColor = Color(0xFFE0E3E7)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .background(bgColor, shape = RoundedCornerShape(16.dp))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Settings
            IconButton(onClick = onPreferences) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.tts_settings),
                    tint = mutedColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Previous
            IconButton(onClick = onPrevious) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = stringResource(R.string.tts_previous),
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Play / Pause — primary button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
                    .clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = stringResource(
                        if (playing) R.string.tts_pause else R.string.tts_play
                    ),
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Next
            IconButton(onClick = onNext) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = stringResource(R.string.tts_next),
                    tint = textColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Stop
            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = stringResource(R.string.tts_stop),
                    tint = mutedColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
