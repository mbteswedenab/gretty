// Set URL of your WebSocketMain.swf here:
WEB_SOCKET_SWF_LOCATION = "WebSocketMain.swf";
// Set this to dump debug message from Flash to console.log:
WEB_SOCKET_DEBUG = true;

// Everything below is the same as using standard WebSocket.
var ws;

function onnameclick(id) {
    FB.ui({method: 'apprequests', to: id, message: 'You should learn more about this awesome game.', data: 'tracking information for the user'});
}

function load () {
  reconnect ()

//  FB.api("/me/friends", function(result) {
//    var markup = '<table>';
//    var numFriends = result ? result.data.length : 0;
//    if (numFriends > 0) {
//      for (var i=0; i<numFriends; i++) {
//        markup += ( '<tr><td><fb:profile-pic size="square" ' +
//                          'uid="' + result.data[i].id + '" ' +
//                          'facebook-logo="false"' + 'linked="false"' +
//          '></fb:profile-pic></td><td><a href="#" onclick="onnameclick(' + result.data[i].id + ')">' + result.data[i].name + '</a></td></tr>'
//        );
//      }
//    }
//    markup += '</table>'
//
//    var profilePicsDiv = $('profilePicsDiv')
//    profilePicsDiv.innerHTML = markup;
//    FB.XFBML.parse(profilePicsDiv);
//  });
}

function reconnect() {
    // Connect to Web Socket.
    // Change host/port here to your own Web Socket server.
    ws = new WebSocket("ws://" + document.location.host + "/");

    // Set event handlers.
    ws.onopen = function() {
        $('connectionStatus').innerHTML = "OK"
        ws.send(Object.toJSON({ msgType: 'getHistory'}))
    };
    ws.onmessage = function(e) {
        var msg = e.data.evalJSON()
        if(msg.msgType == 'newPost')
            output(msg)
        else {
            $('log').innerHTML = ''
            for(i =0; i != msg.events.size; ++i) {
                output(msg.events[i])
            }
        }
    };
    ws.onerror = ws.onclose = function() {
        $('connectionStatus').innerHTML = "NONE"
        ws.onclose = ws.onerror = null
        ws.close()
        window.setTimeout(function() {
            reconnect()
        }, 2000)
    };
}

function output(e) {
    var log = document.getElementById("log");
    var escaped = e.message.replace(/&/, "&amp;").replace(/</, "&lt;").
      replace(/>/, "&gt;").replace(/"/, "&quot;"); // "
    log.innerHTML = '<table><tr><td><img src="' + e.userImg + '"></td><td><div><i>' + e.userName + '</i></div><div>' + escaped + "</div></td></tr></table>" + log.innerHTML;
}

function sendChat() {
  ws.send(Object.toJSON({ msgType: 'newPost', userId: userId, userName: userName, userImg: userImg, message: $('message').value }))
  $('message').value = ''
}
