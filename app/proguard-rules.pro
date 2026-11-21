# Copyright (C) Silicon Circuits and the Tarangini contributors. This file
# is part of Tarangini, free software under the GNU General Public License,
# version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only

# Rules of the app for R8. The libraries (Hilt, Room, Media3, Compose, the serialization
# library) bring their own rules, so only what is special to this app goes here.

# Keep file names and line numbers, so the stack trace of a crash can be read. The real file
# name is replaced by "SourceFile"; the mapping file of the build gives it back.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
