/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector

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

/** Makes the renderers of the player. It differs from the default only in the audio renderer. */
// A renderers factory is API that may still change.
@OptIn(UnstableApi::class)
internal class GaplessRenderersFactory(context: Context) : DefaultRenderersFactory(context) {
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
            )
    }
}

/** The audio renderer of Media3, with the silence of each track set for the decoder that plays it. */
// A renderer is API that may still change.
@OptIn(UnstableApi::class)
internal class GaplessAudioRenderer(
    context: Context,
    mediaCodecSelector: MediaCodecSelector,
    enableDecoderFallback: Boolean,
    eventHandler: Handler,
    eventListener: AudioRendererEventListener,
    audioSink: AudioSink,
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
}
