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
import androidx.media3.common.Format
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecAdapter
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
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
 * audio sink, which runs [audioProcessors] (the equalizer) before the processors of Media3.
 */
// A renderers factory is API that may still change.
@OptIn(UnstableApi::class)
internal class GaplessRenderersFactory(
    context: Context,
    private val sound: LastSound,
    private val audioProcessors: List<AudioProcessor> = emptyList(),
) : DefaultRenderersFactory(context) {
    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean,
    ): AudioSink = DefaultAudioSink.Builder(context)
        .setEnableFloatOutput(enableFloatOutput)
        .setEnableAudioOutputPlaybackParameters(enableAudioTrackPlaybackParams)
        .setAudioProcessors(audioProcessors.toTypedArray())
        .build()

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
) : MediaCodecAudioRenderer(
    context,
    mediaCodecSelector,
    enableDecoderFallback,
    eventHandler,
    eventListener,
    audioSink,
) {
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
