/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
 *
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
 *
 * Contributors:
 *     mhilaire
 *
 */

package org.nuxeo.ecm.core.cache;

import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;
import org.nuxeo.runtime.test.runner.LocalDeploy;
import org.nuxeo.runtime.test.runner.SimpleFeature;

import com.google.inject.Binder;
import com.google.inject.Provider;
import com.google.inject.name.Names;

@Features(CacheFeature.class)
@LocalDeploy("org.nuxeo.ecm.core.cache:inmemory-cache-config.xml")
public class InMemoryCacheFeature extends SimpleFeature {

    public static final String MAXSIZE_TEST_CACHE_NAME = "maxsize-test-cache";

    @Override
    public void initialize(FeaturesRunner runner) throws Exception {
        runner.getFeature(CacheFeature.class).enable();
    }

    @Override
    public void configure(FeaturesRunner runner, Binder binder) {
        binder.bind(Cache.class).annotatedWith(Names.named(MAXSIZE_TEST_CACHE_NAME)).toProvider(new Provider<Cache>() {

            @Override
            public Cache get() {
                return Framework.getService(CacheService.class).getCache(MAXSIZE_TEST_CACHE_NAME);
            }

        });
    }

}
