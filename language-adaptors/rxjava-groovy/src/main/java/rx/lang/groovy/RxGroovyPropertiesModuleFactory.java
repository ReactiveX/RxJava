/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.lang.groovy;

import java.util.Properties;

import org.codehaus.groovy.runtime.m12n.ExtensionModule;
import org.codehaus.groovy.runtime.m12n.PropertiesModuleFactory;

/**
 * Factory for {@link RxGroovyExtensionModule} to add extension methods.
 * <p>
 * This is loaded from /META-INF/services/org.codehaus.groovy.runtime.ExtensionModule
 * <p>
 * The property is defined as: moduleFactory=rx.lang.groovy.RxGroovyPropertiesModuleFactory
 */
public class RxGroovyPropertiesModuleFactory extends PropertiesModuleFactory {

    @Override
    public ExtensionModule newModule(Properties properties, ClassLoader classLoader) {
        return new RxGroovyExtensionModule();
    }

}
