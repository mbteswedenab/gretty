/*
 * Copyright 2009-2010 MBTE Sweden AB.
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
package org.mbte.gretty

import javax.servlet.http.HttpSession
import javax.servlet.ServletConfig

/**
 * Extension methods for classes of servlet framework
 */
class ServletCategory {
    /**
     * Allows array-like syntax to access session attributes
     *
     * @param session
     * @param name
     * @return
     */
    static def getAt(HttpSession session, String name) {
        session.getAttribute(name)
    }

    /**
     * Allows array-like syntax to access session attributes
     *
     * @param session
     * @param name
     * @param value
     * @return
     */
    static void putAt(HttpSession session, String name, def value) {
        session.setAttribute(name, value)
    }

    /**
     * Allows property-like syntax to access session attributes
     *
     * @param session
     * @param name
     * @return
     */
    static def getUnresolvedProperty(HttpSession session, String name) {
        session.getAttribute(name)
    }

    /**
     * Allows property-like syntax to access session attributes
     *
     * @param session
     * @param name
     * @param value
     */
    static void setUnresolvedProperty(HttpSession session, String name, Object value) {
        session.setAttribute(name, value)
    }

    /**
     * Property-like access to initial parameters of ServletConfig
     * 
     * @param config
     * @param name
     * @return
     */
    static String getUnresolvedProperty(ServletConfig config, String name) {
        config.getInitParameter(name)
    }
}
