/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.trino.tempto.internal.initialization.modules;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import io.trino.tempto.configuration.Configuration;
import io.trino.tempto.initialization.TestMethodModuleProvider;
import io.trino.tempto.internal.listeners.TestMetadata;
import io.trino.tempto.internal.listeners.TestMetadataReader;
import org.testng.ITestResult;

public class TestMethodInfoModuleProvider
        implements TestMethodModuleProvider
{
    private final TestMetadataReader testMetadataReader = new TestMetadataReader();

    public Module getModule(Configuration configuration, ITestResult testResult)
    {
        TestMetadata testMetadata = testMetadataReader.readTestMetadata(testResult);
        return new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(TestMetadata.class).toInstance(testMetadata);
            }
        };
    }
}
