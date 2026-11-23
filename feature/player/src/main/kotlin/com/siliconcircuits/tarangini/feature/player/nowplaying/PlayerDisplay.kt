/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

package com.siliconcircuits.tarangini.feature.player.nowplaying

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.siliconcircuits.tarangini.core.domain.model.PlaybackError
import com.siliconcircuits.tarangini.core.domain.model.RepeatMode
import com.siliconcircuits.tarangini.core.domain.model.SleepState
import com.siliconcircuits.tarangini.core.domain.model.VisualMode
import com.siliconcircuits.tarangini.core.ui.R as CoreUiR
import com.siliconcircuits.tarangini.core.ui.component.ChipKind
import com.siliconcircuits.tarangini.core.ui.component.ChipPlace
import com.siliconcircuits.tarangini.core.ui.component.LedChip
import com.siliconcircuits.tarangini.core.ui.component.formatDuration
import com.siliconcircuits.tarangini.core.ui.theme.DigitsFont
import com.siliconcircuits.tarangini.core.ui.theme.lookColors
import com.siliconcircuits.tarangini.feature.player.R

/**
 * The dark display of the player: the play mark, the time, Shuffle and Repeat, the status tags,
 * the visual and the song. With nothing loaded ([state] null) it says so.
 *
 * @param onOpenAlbum opens the album of the song, or null when the song has no album to open
 */
@Composable
internal fun PlayerDisplay(
    state: NowPlayingUiState?,
    actions: NowPlayingActions,
    parts: DisplayParts,
    onOpenAlbum: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Column {
            TopLine(state, actions)
            if (state != null) StatusTags(state, parts.levelling)
        }
        // The visual stands still in a pause, while the player waits for data and after an error.
        val moving = state != null && state.showPause && !state.isBuffering && state.error == null
        SoundVisualView(mode = parts.mode, shown = if (moving) parts.shown else Still, onCycle = parts.onCycleVisual)
        BottomLine(state, actions.onToggleFavourite, onOpenAlbum)
    }
}

/** SLEEP, the speed and LEVEL, each only while it is on. Nothing is shown while all are off. */
@Composable
private fun StatusTags(state: NowPlayingUiState, levelling: Boolean) {
    val tags = buildList {
        if (state.sleep != SleepState.Off) {
            add(stringResource(R.string.player_tag_sleep) to stringResource(R.string.player_tag_sleep_spoken))
        }
        if (state.speed != 1f && !state.isLive) {
            val speed = stringResource(R.string.now_playing_speed_value, state.speed.toString().removeSuffix(".0"))
            add(speed to stringResource(R.string.player_tag_speed_spoken, speed))
        }
        if (levelling) {
            add(
                stringResource(R.string.player_tag_level) to stringResource(R.string.player_tag_level_spoken),
            )
        }
    }
    if (tags.isEmpty()) return
    val look = lookColors
    val spoken = tags.joinToString(", ") { it.second }
    Row(
        modifier = Modifier.clearAndSetSemantics {
            contentDescription = spoken
            liveRegion = LiveRegionMode.Polite
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { (text, _) ->
            Row(
                modifier = Modifier
                    .heightIn(min = 18.dp)
                    .background(look.lcdTag, MaterialTheme.shapes.extraSmall)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(Modifier.size(4.dp).background(look.lcdAccent, CircleShape))
                Text(text = text, style = SmallWord.copy(fontSize = 10.sp), color = look.lcdAccent)
            }
        }
    }
}

/** The song with the way to its album and the heart, or why it does not play, or that nothing plays. */
@Composable
private fun BottomLine(state: NowPlayingUiState?, onToggleFavourite: () -> Unit, onOpenAlbum: (() -> Unit)?) {
    val look = lookColors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind { drawLine(look.lcdEdge, Offset.Zero, Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        when {
            state == null -> EmptyText(Modifier.weight(1f))
            state.error != null -> ErrorText(state.error, Modifier.weight(1f))
            else -> SongButton(state, onOpenAlbum, Modifier.weight(1f))
        }
        state?.isFavourite?.let { on -> HeartKey(on, onToggleFavourite) }
    }
}

/** The title, moving when it is too long, and the artist and album. A tap opens the album. */
@Composable
private fun SongButton(state: NowPlayingUiState, onOpenAlbum: (() -> Unit)?, modifier: Modifier = Modifier) {
    val look = lookColors
    val title = state.title ?: stringResource(CoreUiR.string.unknown_title)
    val artist = state.artist ?: stringResource(CoreUiR.string.unknown_artist)
    val album = state.album?.takeIf { it.isNotBlank() }
    val spoken = if (onOpenAlbum != null && album != null) {
        stringResource(R.string.player_song_open_album, title, artist, album)
    } else {
        stringResource(R.string.player_song_by, title, artist)
    }
    Column(
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (onOpenAlbum != null) Modifier.clickable(role = Role.Button, onClick = onOpenAlbum) else Modifier)
            .clearAndSetSemantics {
                contentDescription = spoken
                if (onOpenAlbum != null) role = Role.Button
            },
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = if (state.queueSize > 1) stringResource(R.string.player_song_line, state.place, title) else title,
            // A long title moves a few times while the song plays, then rests at its start.
            modifier = if (state.showPause) Modifier.basicMarquee(iterations = MARQUEE_TIMES) else Modifier,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            fontWeight = FontWeight.Bold,
            color = look.lcdText,
            maxLines = 1,
            overflow = if (state.showPause) TextOverflow.Clip else TextOverflow.Ellipsis,
        )
        // A station that sends no song title has only the station here.
        val second = if (album != null &&
            !state.isLive
        ) {
            stringResource(R.string.player_artist_album, artist, album)
        } else {
            artist
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = second,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = look.lcdMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (onOpenAlbum != null) {
                Icon(
                    painterResource(CoreUiR.drawable.ic_chevron_right),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = look.lcdMuted,
                )
            }
        }
    }
}

/** Why the item does not play. Play tries again. */
@Composable
private fun ErrorText(error: PlaybackError, modifier: Modifier = Modifier) {
    val look = lookColors
    val text = stringResource(
        when (error) {
            PlaybackError.NETWORK -> R.string.player_error_network
            PlaybackError.FILE_NOT_AVAILABLE -> R.string.player_error_file
            PlaybackError.CANNOT_PLAY -> R.string.player_error_cannot_play
            PlaybackError.UNKNOWN -> R.string.player_error_unknown
        },
    )
    Row(
        modifier = modifier.heightIn(min = 48.dp).semantics { liveRegion = LiveRegionMode.Assertive },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(CoreUiR.drawable.ic_warning), null, Modifier.size(18.dp), look.warn)
        Text(text = text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = look.warn)
    }
}

@Composable
private fun EmptyText(modifier: Modifier = Modifier) {
    val look = lookColors
    Column(
        modifier = modifier.heightIn(min = 48.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.player_nothing),
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
            fontWeight = FontWeight.ExtraBold,
            color = look.lcdText,
        )
        Text(
            text = stringResource(R.string.player_empty_text),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = look.lcdMuted,
        )
    }
}

/** The heart. Its name for a screen reader is what a tap does. */
@Composable
private fun HeartKey(on: Boolean, onToggle: () -> Unit) {
    val look = lookColors
    val label = stringResource(if (on) CoreUiR.string.song_remove_favourite else CoreUiR.string.song_add_favourite)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clickable(role = Role.Button, onClick = onToggle)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(if (on) CoreUiR.drawable.ic_favourite_filled else CoreUiR.drawable.ic_favourite),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = if (on) look.heart else look.lcdMuted,
        )
    }
}

/** Moves the content [by] to the start, into the padding of the display, so a key lines up with its edge. */
private val Still: () -> ShownVisual = { ShownVisual.STILL }
private const val MARQUEE_TIMES = 3
