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

import rx.plugins.RxJavaPlugins;

public final class RxJavaPluginUtils {

    public static void handleException(Throwable e) {
        try {
            RxJavaPlugins.getInstance().getErrorHandler().handleError(e);
        } catch (Throwable pluginException) {
            handlePluginException(pluginException);
        }
    }

    private static void handlePluginException(Throwable pluginException) {
        /*
         * We don't want errors from the plugin to affect normal flow.
         * Since the plugin should never throw this is a safety net
         * and will complain loudly to System.err so it gets fixed.
         */
        System.err.println("RxJavaErrorHandler threw an Exception. It shouldn't. => " + pluginException.getMessage());
        pluginException.printStackTrace();
    }
    
}
