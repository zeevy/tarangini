# Copyright (C) Silicon Circuits and the Tarangini contributors. This file
# is part of Tarangini, free software under the GNU General Public License,
# version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only

# Extra rules for the release app only in the end to end run (-Ptarangini.releaseEndToEnd).
# The test app leaves out the libraries the app already has, so the app must keep the parts of
# them that the test libraries call. The release app that ships is built without this file.
# The libraries are kept whole; the code of the app itself is still shrunk and renamed.
-keep class androidx.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class com.google.common.** { *; }
