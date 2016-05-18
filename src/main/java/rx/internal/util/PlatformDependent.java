/**
 * Copyright 2014 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package rx.internal.util;

import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * Allow platform dependent logic such as checks for Android.
 *
 * Modeled after Netty with some code copy/pasted from: https://github.com/netty/netty/blob/master/common/src/main/java/io/netty/util/internal/PlatformDependent.java
 */
public final class PlatformDependent {

    /**
     * Possible value of {@link #getAndroidApiVersion()} which means that the current platform is not Android.
     */
    public static final int ANDROID_API_VERSION_IS_NOT_ANDROID = 0;

    private static final int ANDROID_API_VERSION = resolveAndroidApiVersion();

    private static final boolean IS_ANDROID = ANDROID_API_VERSION != ANDROID_API_VERSION_IS_NOT_ANDROID;

    /**
     * Returns {@code true} if and only if the current platform is Android.
     * @return {@code true} if and only if the current platform is Android
     */
    public static boolean isAndroid() {
        return IS_ANDROID;
    }

    /**
     * Returns version of Android API.
     *
     * @return version of Android API or {@link #ANDROID_API_VERSION_IS_NOT_ANDROID } if version
     * can not be resolved or if current platform is not Android.
     */
    public static int getAndroidApiVersion() {
        return ANDROID_API_VERSION;
    }

    /**
     * Resolves version of Android API.
     *
     * @return version of Android API or {@link #ANDROID_API_VERSION_IS_NOT_ANDROID} if version can not be resolved
     * or if the current platform is not Android.
     * @see <a href="http://developer.android.com/reference/android/os/Build.VERSION.html#SDK_INT">Documentation</a>
     */
    private static int resolveAndroidApiVersion() {
        try {
            return (Integer) Class
                    .forName("android.os.Build$VERSION", true, getSystemClassLoader())
                    .getField("SDK_INT")
                    .get(null);
        } catch (Exception e) {
            // Can not resolve version of Android API, maybe current platform is not Android
            // or API of resolving current Version of Android API has changed in some release of Android
            return ANDROID_API_VERSION_IS_NOT_ANDROID;
        }
    }

    /**
     * Return the system {@link ClassLoader}.
     */
    static ClassLoader getSystemClassLoader() {
        if (System.getSecurityManager() == null) {
            return ClassLoader.getSystemClassLoader();
        } else {
            return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
                @Override
                public ClassLoader run() {
                    return ClassLoader.getSystemClassLoader();
                }
            });
        }
    }
}
