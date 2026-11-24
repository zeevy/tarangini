/*
 * Copyright (C) Silicon Circuits and the Tarangini contributors. This file
 * is part of Tarangini, free software under the GNU General Public License,
 * version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only
 */

// Media3 hands its own Handler to every renderer. This file only passes it on and starts no work on it (K2).
@file:Suppress("ForbiddenImport")

package com.siliconcircuits.tarangini.core.playback.player

import android.content.Context
import android.media.MediaFormat
import android.os.Handler
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Timeline
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.FormatHolder
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.MediaSource
import com.siliconcircuits.tarangini.core.domain.audio.ReplayGain
import com.siliconcircuits.tarangini.core.playback.audio.CrossfadeSink
import com.siliconcircuits.tarangini.core.playback.audio.replayGainOf
import java.nio.ByteBuffer

/**
 * The samples an MP3 decoder gives out before the first real one. Media3 adds this number to the
 * start silence of a file from the LAME or FFmpeg encoder, and takes it off the end silence.
 */
internal const val MP3_DECODER_DELAY = 529

/** The MP3 decoders of Android. They cut their own delay, so Media3 must not cut it a second time. */
private val DECODERS_THAT_CUT_THEIR_DELAY = setOf("c2.android.mp3.decoder", "OMX.google.mp3.decoder")

/**
 * The format with the start and end silence that is right for the decoder named [decoderName].
 *
 * Without this, each MP3 track loses its first 12 ms of sound and ends with 12 ms of silence, which
 * is heard as a tick between the tracks of an album. See `docs/adr/0007-gapless-playback.md`.
 */
// The silence fields of a format are API that may still change.
@OptIn(UnstableApi::class)
internal fun Format.forDecoder(decoderName: String?): Format {
    val cutTwice = sampleMimeType == MimeTypes.AUDIO_MPEG &&
        decoderName in DECODERS_THAT_CUT_THEIR_DELAY &&
        encoderDelay >= MP3_DECODER_DELAY
    if (!cutTwice) return this
    return buildUpon()
        .setEncoderDelay(encoderDelay - MP3_DECODER_DELAY)
        .setEncoderPadding(encoderPadding + MP3_DECODER_DELAY)
        .build()
}

/**
 * Makes the renderers of the player. It differs from the default in the audio renderer, and in the
 * audio sink, which runs [audioProcessors] (the equalizer) before the processors of Media3 and has
 * the [CrossfadeSink] in front of it, fading for [crossfadeUs].
 */
// A renderers factory is API that may still change.
@OptIn(UnstableApi::class)
internal class GaplessRenderersFactory(
    context: Context,
    private val sound: LastSound,
    private val audioProcessors: List<AudioProcessor> = emptyList(),
    private val onSongStarts: SongStarts = SongStarts { _, _ -> },
    private val crossfadeUs: () -> Long = { 0L },
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = CrossfadeSink(
        DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(enableFloatOutput)
            .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
            .setAudioProcessors(audioProcessors.toTypedArray())
            .build(),
        crossfadeUs,
    )

    override fun buildAudioRenderers(
        context: Context,
        extensionRendererMode: Int,
        mediaCodecSelector: MediaCodecSelector,
        enableDecoderFallback: Boolean,
        audioSink: AudioSink,
        eventHandler: Handler,
        eventListener: AudioRendererEventListener,
        out: ArrayList<Renderer>,
    ) {
        out +=
            GaplessAudioRenderer(
                context,
                mediaCodecSelector,
                enableDecoderFallback,
                eventHandler,
                eventListener,
                audioSink,
                sound,
                onSongStarts,
            )
    }
}

/** The audio renderer of Media3, with the silence of each track set for the decoder that plays it. */
// A renderer is API that may still change. Its parameters are the ones of the Media3 renderer, plus one.
@OptIn(UnstableApi::class)
@Suppress("LongParameterList")
internal class GaplessAudioRenderer(
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    enableDecoderFallback: Boolean,
    eventHandler: Handler,
    eventListener: AudioRendererEventListener,
    audioSink: AudioSink,
    private val sound: LastSound,
    private val onSongStarts: SongStarts = SongStarts { _, _ -> },
) : MediaCodecAudioRenderer(
    context,
    mediaCodecSelector,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    audioSink,
) {
    private val fade = audioSink as? CrossfadeSink
    private val songs = StreamSongs<Any>()
    private val gainOf = LinkedHashMap<Any, ReplayGain>()
    private var reading: Any? = null
    private var playing: Any? = null

    override fun onStreamChanged(
        formats: Array<out Format>,
        startPositionUs: Long,
        offsetUs: Long,
        mediaPeriodId: MediaSource.MediaPeriodId,
    ) {
        // Kept before Media3 takes the stream, because for the first stream it tells of the start at once.
        songs.read(offsetUs, mediaPeriodId.periodUid)
        reading = mediaPeriodId.periodUid
        super.onStreamChanged(formats, startPositionUs, offsetUs, mediaPeriodId)
        songs.playsAtOnce(offsetUs, outputStreamOffsetUs)?.let(::songStarts)
    }

    // Called on the playback thread after the last sound of a song went to the sink and before the
    // first sound of the next one, so the equalizer can switch at the exact sample.
    override fun onOutputStreamOffsetUsChanged(outputStreamOffsetUs: Long) {
        super.onOutputStreamOffsetUsChanged(outputStreamOffsetUs)
        fade?.nextSongStarts()
        songStarts(songs.started(outputStreamOffsetUs))
    }

    /**
     * Tells of the song of [periodUid]. A song that is not known (any more) plays as one with no
     * genre and no tags.
     */
    private fun songStarts(periodUid: Any?) {
        playing = periodUid
        val timeline = timeline
        val period = periodUid?.let(timeline::getIndexOfPeriod)?.takeIf { it != C.INDEX_UNSET }
        val item = period?.let {
            timeline.getWindow(timeline.getPeriod(it, Timeline.Period()).windowIndex, Timeline.Window()).mediaItem
        }
        onSongStarts.songStarts(item ?: MediaItem.EMPTY, periodUid?.let(gainOf::get))
    }

    /** The length of the song that plays, from the timeline, or null when it is not known. */
    fun playingLengthUs(): Long? {
        val period = playing?.let(timeline::getIndexOfPeriod)?.takeIf { it != C.INDEX_UNSET } ?: return null
        return timeline.getPeriod(period, Timeline.Period()).durationUs.takeIf { it != C.TIME_UNSET }
    }

    /** The stream offset of the sound that goes out now. */
    fun outputOffsetUs(): Long = outputStreamOffsetUs

    // The format of a stream comes from its file before its first sound, with the tags of the file.
    // For the first song of a queue it comes only after the start was told, so the tags follow.
    override fun onInputFormatChanged(formatHolder: FormatHolder): DecoderReuseEvaluation? {
        val period = reading
        if (period != null) {
            val gain = replayGainOf(formatHolder.format?.metadata)
            if (gain == null) gainOf.remove(period) else gainOf[period] = gain
            while (gainOf.size > KEPT_GAINS) gainOf.remove(gainOf.keys.first())
            if (period == playing) songStarts(period)
        }
        return super.onInputFormatChanged(formatHolder)
    }

    override fun onOutputFormatChanged(format: Format, mediaFormat: MediaFormat?) {
        super.onOutputFormatChanged(format.forDecoder(codecInfo?.name), mediaFormat)
    }

    // Called for every piece of sound the decoder gives, so it tells that the file still has sound.
    @Suppress("LongParameterList")
    override fun processOutputBuffer(
        positionUs: Long,
        elapsedRealtimeUs: Long,
        codec: MediaCodecAdapter?,
        buffer: ByteBuffer?,
        bufferIndex: Int,
        bufferFlags: Int,
        sampleCount: Int,
        bufferPresentationTimeUs: Long,
        isDecodeOnlyBuffer: Boolean,
        isLastBuffer: Boolean,
        format: Format,
    ): Boolean {
        sound.heard()
        if (!isDecodeOnlyBuffer) fade?.let { tellTheEnd(it, bufferPresentationTimeUs) }
        return super.processOutputBuffer(
            positionUs,
            elapsedRealtimeUs,
            codec,
            buffer,
            bufferIndex,
            bufferFlags,
            sampleCount,
            bufferPresentationTimeUs,
            isDecodeOnlyBuffer,
            isLastBuffer,
            format,
        )
    }
}

/**
 * Tells [sink] how much of the playing song is left when the sound at [timeUs] goes out. The end
 * comes from the length in the timeline. A file that states a longer length than it has (ADR 0024)
 * ends before its fade would start, so it gets none and the next song follows with no gap.
 */
@OptIn(UnstableApi::class)
private fun GaplessAudioRenderer.tellTheEnd(sink: CrossfadeSink, timeUs: Long) {
    sink.songEndsIn { playingLengthUs()?.let { durationUs -> durationUs - (timeUs - outputOffsetUs()) } }
}

/** Hears of each song whose first sound goes to the processors next, with the tags of its file. */
internal fun interface SongStarts {
    /** Called on the playback thread with the [item] that starts and its ReplayGain tags, or null. */
    fun songStarts(item: MediaItem, gain: ReplayGain?)
}

/** The tags of a few songs read ahead are kept; older ones are done. */
private const val KEPT_GAINS = 8
