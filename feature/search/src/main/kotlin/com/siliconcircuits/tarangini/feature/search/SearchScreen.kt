/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.siliconcircuits.tarangini.core.domain.model.Album
import com.siliconcircuits.tarangini.core.domain.model.Artist
import com.siliconcircuits.tarangini.core.domain.model.SearchResults
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.AlbumRow
import com.siliconcircuits.tarangini.core.ui.component.ArtistRow
import com.siliconcircuits.tarangini.core.ui.component.BackButton
import com.siliconcircuits.tarangini.core.ui.component.LcdPanel
import com.siliconcircuits.tarangini.core.ui.component.ListCard
import com.siliconcircuits.tarangini.core.ui.component.MessageState
import com.siliconcircuits.tarangini.core.ui.component.SectionTitle
import com.siliconcircuits.tarangini.core.ui.component.SongRow
import com.siliconcircuits.tarangini.core.ui.component.SongRowActions
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import kotlinx.coroutines.flow.filter

/** The search screen: one field, and under it the songs, albums and artists that were found. */
@Composable
internal fun SearchScreen(
    onBack: () -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SearchContent(
        text = viewModel.text,
        state = state,
        songActions = viewModel.songActions,
        onTextChange = viewModel::onTextChange,
        onOpenAlbum = onOpenAlbum,
        onOpenArtist = onOpenArtist,
        onBack = onBack,
        modifier = modifier,
    )
}

/** The search screen in the look of the app: the field on the display, the results on a list card. */
@Composable
internal fun SearchContent(
    text: String,
    state: SearchUiState,
    songActions: SongRowActions,
    onTextChange: (String) -> Unit,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 10.dp),
    ) {
        Row(Modifier.heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            BackButton(onClick = onBack)
            SearchField(text, onTextChange, Modifier.weight(1f))
        }
        val results = state.results
        ListCard(
            count = resultCount(results),
            modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp, bottom = 10.dp),
        ) {
            when {
                state.failed -> MessageState(
                    title = stringResource(R.string.search_error_title),
                    text = stringResource(R.string.search_error_text),
                    icon = painterResource(CoreUiR.drawable.ic_search),
                )

                results == null -> MessageState(
                    title = stringResource(R.string.search_start_title),
                    text = stringResource(R.string.search_start_text),
                    icon = painterResource(CoreUiR.drawable.ic_search),
                )

                results.isEmpty -> MessageState(
                    title = stringResource(R.string.search_none_title),
                    // The text that was searched, which can be older than what is in the field now.
                    text = stringResource(R.string.search_none_text, state.searched),
                    icon = painterResource(CoreUiR.drawable.ic_search),
                )

                else -> Results(results, songActions, onOpenAlbum, onOpenArtist)
            }
        }
    }
}

/** "3 results", or "Search" while nothing was searched. */
@Composable
private fun resultCount(results: SearchResults?): String {
    if (results == null) return stringResource(CoreUiR.string.action_search)
    val count = results.songs.size + results.albums.size + results.artists.size
    return pluralStringResource(R.plurals.search_results, count, count)
}

/** The field on the dark display, with a magnifier in front and a button that clears it. */
@Composable
private fun SearchField(text: String, onTextChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val label = stringResource(R.string.search_field)
    val look = lookColors
    // The person opened this screen to type, so the field has the cursor from the start. Coming
    // back from an album with a text in the field, the results matter and the keyboard stays away.
    LaunchedEffect(Unit) { if (text.isEmpty()) focus.requestFocus() }
    LcdPanel(modifier = modifier, padding = PaddingValues(start = 12.dp, end = 0.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(CoreUiR.drawable.ic_search),
                contentDescription = null,
                Modifier.size(18.dp),
                look.lcdMuted,
            )
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                // The hint is gone once a text is typed, so a screen reader gets a name that stays.
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .padding(start = 8.dp)
                    .focusRequester(focus)
                    .semantics { contentDescription = label },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = look.lcdText, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(look.lcdAccent),
                singleLine = true,
                // The results are there already, so the search key only closes the keyboard.
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (text.isEmpty()) {
                            Text(
                                stringResource(R.string.search_hint),
                                color = look.lcdMuted,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        field()
                    }
                },
            )
            if (text.isNotEmpty()) {
                IconButton(onClick = { onTextChange("") }) {
                    Icon(
                        painter = painterResource(CoreUiR.drawable.ic_close),
                        contentDescription = stringResource(R.string.search_clear),
                        tint = look.lcdMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun Results(
    results: SearchResults,
    songActions: SongRowActions,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
) {
    val list = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    // The keyboard covers half of the results. It goes away when the person starts to scroll them.
    LaunchedEffect(list, keyboard) {
        snapshotFlow { list.isScrollInProgress }.filter { it }.collect { keyboard?.hide() }
    }
    LazyColumn(state = list, modifier = Modifier.fillMaxSize()) {
        // A song, an album and an artist can have the same number, so every key gets the kind in front.
        if (results.songs.isNotEmpty()) {
            item(key = "songs") { SectionTitle(stringResource(CoreUiR.string.section_songs)) }
            items(items = results.songs, key = { "song-${it.id}" }) { SongRow(it, songActions) }
        }
        if (results.albums.isNotEmpty()) {
            item(key = "albums") { SectionTitle(stringResource(CoreUiR.string.section_albums)) }
            items(items = results.albums, key = { "album-${it.id}" }) { album ->
                AlbumRow(album, onClick = { onOpenAlbum(album) })
            }
        }
        if (results.artists.isNotEmpty()) {
            item(key = "artists") { SectionTitle(stringResource(CoreUiR.string.section_artists)) }
            items(items = results.artists, key = { "artist-${it.id}" }) { artist ->
                ArtistRow(artist, onClick = { onOpenArtist(artist) })
            }
        }
    }
}
