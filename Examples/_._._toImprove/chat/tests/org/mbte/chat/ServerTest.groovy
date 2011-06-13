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

package org.mbte.chat

import org.mbte.gretty.httpclient.GrettyClient
import org.jboss.netty.channel.local.LocalAddress
import org.jboss.netty.handler.codec.http.HttpMethod

@Typed class ServerTest extends ChatAppTest {
    void testCreateUser () {
        GrettyClient client = [new LocalAddress("test_server")]
        client.connect().await()

        def postResp = client.request([
                method:HttpMethod.POST,
                uri:"/user/create",
                json: [userName:'alex', password:'secret'].toJsonString()
        ]).get()
        def resp = Map.fromJson(postResp.contentText)

        def getResp = client.request([uri:"/user/${resp.userId}"]).get()
        def res = Map.fromJson(getResp.contentText)

        println res
        assert res.userName == 'alex'
        assert res.userId == resp.userId
    }
}
