/*
 * Copyright 2022 Readium Foundation. All rights reserved.
 * Use of this source code is governed by the BSD-style license
 * available in the top-level LICENSE file of the project.
 */

package com.example.epub_reader_kit.reader.shared.views

import androidx.compose.runtime.Composable
import com.example.epub_reader_kit.reader.utils.compose.Emphasis
import com.example.epub_reader_kit.reader.utils.compose.EmphasisProvider
import com.example.epub_reader_kit.reader.utils.compose.LocalContentEmphasis

/**
 * Sets the emphasis (alpha) of a group of [Composable] views.
 */
@Composable
fun Group(enabled: Boolean = true, content: @Composable () -> Unit) {
    val emphasis = when {
        !enabled -> Emphasis.Disabled
        else -> Emphasis.Medium
    }
    EmphasisProvider(LocalContentEmphasis provides emphasis) {
        content()
    }
}
