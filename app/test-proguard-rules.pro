# Copyright (C) Silicon Circuits and the Tarangini contributors. This file
# is part of Tarangini, free software under the GNU General Public License,
# version 3 only. See the LICENSE file. SPDX-License-Identifier: GPL-3.0-only

# Rules for the test app of the end to end test when it runs against the shrunk release app
# (-Ptarangini.releaseEndToEnd). The test app is shrunk too, but it must keep the tests and the
# test runner whole, as the runner finds them by name.
-dontobfuscate
-keep class com.siliconcircuits.tarangini.MainFlowTest { *; }
-keep class com.siliconcircuits.tarangini.TestAudioFiles { *; }
-keep class androidx.test.** { *; }
-keep class org.junit.** { *; }
-keep class junit.** { *; }

# The annotations of Error Prone name a class of the Java compiler that Android does not have.
-dontwarn javax.lang.model.element.Modifier
