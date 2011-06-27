/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

(function() {
    function initPseudoWebSockets() {
        window.WebSocket = Class.create({
            initialize: function(url,protocol) {
                this.onopen    = Prototype.emptyFunction,
                this.onclose   = Prototype.emptyFunction,
                this.onerror   = Prototype.emptyFunction,
                this.onmessage = Prototype.emptyFunction,

                this.url = url.replace('ws://', 'http://');
                this.pendingMessages = [];
                this.sessionId = 'null';
                this.protocol = protocol || 'undefined'
                var that = this
                this.writeRequest = new Ajax.Request(this.url, {
                    method: 'post',
                    contentType: "application/json",
                    requestHeaders: ['Pseudo-WebSocket', this.sessionId, 'Pseudo-WebSocket-Protocol', this.protocol],
                    postBody: '',
                    onSuccess: function(response) {
                        try {
                            that.sessionId = response.responseJSON.sessionId
                            that.writeRequest.onFailure = Prototype.emptyFunction
                            that.writeRequest.transport.abort()
                            that.writeRequest = null
                            that.readyState = 1
                            that.onopen()
                            that.sendPending_ ()
                        }
                        catch(e) {
                            that.onerror(e)
                        }
                    },
                    onFailure: function(response) {
                        that.onclose()
                    }
                })
            },

            sendPending_: function () {
                if (this.readyState == 1 && !this.writeRequest) {
                    var data = this.pendingMessages
                    this.pendingMessages = []
                    var that = this
                    this.writeRequest = new Ajax.Request(this.url, {
                        method: 'post',
                        contentType: "application/json",
                        requestHeaders: ['Pseudo-WebSocket', this.sessionId, 'Pseudo-WebSocket-Protocol', this.protocol],
                        postBody: Object.toJSON({sessionId:this.sessionId, protocol:this.protocol, messages:data}),
                        onSuccess: function(response) {
                            var messages = response.responseJSON.messages;
                            for(var i = 0; i != messages.length; i+=1)
                                that.onmessage({data:messages[i]})
                            that.writeRequest.onFailure = Prototype.emptyFunction
                            that.writeRequest.transport.abort()
                            that.writeRequest = null
                            that.sendPending_()
                        },
                        onFailure: function(response) {
                            that.writeRequest = null
                            log('write attempt failed')
                        }
                    })
                    
                }
            },

            send: function(message) {
                this.pendingMessages.push(message)
                if(this.writeRequest || this.sessionId == 'null') {
                    return
                }

                this.sendPending_()
            },

            close: function() {
                this.readyState = 2
                this.onclose()
            },

            readyState: 0
        })
    }

    if(!window.WebSocket) {
        initPseudoWebSockets()
        WebSocket.pseudo = true
    }
})()
