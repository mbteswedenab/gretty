/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



package org.mbte.gretty.httpserver.template

import groovypp.text.GppSimpleTemplateEngine
import groovypp.text.GppTemplateScript
import org.jboss.netty.logging.InternalLoggerFactory
import org.jboss.netty.logging.InternalLogger

@Typed class GrettyTemplateEngine extends GppSimpleTemplateEngine {
    private static final InternalLogger logger = InternalLoggerFactory.getInstance(GrettyTemplateEngine)

    GrettyTemplateEngine (ClassLoader classLoader, String scriptClass = GrettyTemplateScript.name) {
        super(classLoader, scriptClass)
    }

    protected Class<GppTemplateScript> compile(File file) {
        if(file.name.endsWith(".gpptl"))
            return super.compile(file)

        logAttemptToCompile(file)
        def clazz = loader.parseClass(new GroovyCodeSource(file), false)
        cache.put(file, new GppSimpleTemplateEngine.CacheEntry(cache, file, clazz))
        clazz
    }

    protected void logCompilationError(Throwable throwable) {
        if(logger.isInfoEnabled())
            logger.info "Compilation error", throwable
    }

    protected void logAttemptToCompile(File file) {
        if(logger.isInfoEnabled())
            logger.info("Trying to compile ${file.canonicalPath}")
    }
}
