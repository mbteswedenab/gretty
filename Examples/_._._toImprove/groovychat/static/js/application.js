// Set URL of your WebSocketMain.swf here:
WEB_SOCKET_SWF_LOCATION = "WebSocketMain.swf";
// Set this to dump debug message from Flash to console.log:
WEB_SOCKET_DEBUG = true;

// Everything below is the same as using standard WebSocket.
var ws;

function onnameclick(id) {
    FB.ui({method: 'apprequests', to: id, message: 'You should learn more about this awesome game.', data: 'tracking information for the user'});
}

function message(obj){
  var el = document.createElement('p');
  if ('announcement' in obj) {
      el.innerHTML += '<em>' + esc(obj.announcement) + '</em>';
  }
  else {
      if ('post' in obj) {
          el.innerHTML = '<img src="http://graph.facebook.com/' + obj.post.userId + '/picture">' + esc(obj.post.message)
      }
      else {
          if ('message' in obj) {
              el.innerHTML = '<b>' + esc(obj.message[0]) + ':</b> ' + esc(obj.message[1]);
              if( obj.message && window.console && console.log ) console.log(obj.message[0], obj.message[1]);
          }
          else {
              el = null
          }
      }
  }

    if(el) {
        document.getElementById('chat').appendChild(el);
        document.getElementById('chat').scrollTop = 1000000;
    }
}

function send(){
  var val = document.getElementById('text').value;
  var post = { post: { message: val, userName: userName, userId: userId} };
  socket.send(post);
  document.getElementById('text').value = '';
}

function esc(msg){
  return msg.replace(/</g, '&lt;').replace(/>/g, '&gt;');
};

var socket = new io.Socket(null, {port: document.location.port, rememberTransport: false, resource: 'chat'});
socket.connect();
socket.on('message', function(obj) {
    if ('post' in obj) {
            message(obj);
    } else {
        if ('announcement' in obj) {
            message(obj);
        }
        else {
            for (var i in obj.buffer) message(obj.buffer[i]);
        }
    }
}).on('connect', function(){
    socket.send({history:true, userName: userName})
    document.getElementById('chat').innerHTML = '';
    message({ message: ['System', 'Connected']})
}).on('disconnect', function(){
    message({ message: ['System', 'Disconnected']})
}).on('reconnect', function(){
    message({ message: ['System', 'Reconnected to server']})
}).on('reconnecting', function( nextRetry ){
    message({ message: ['System', 'Attempting to re-connect to the server, next attempt in ' + nextRetry + 'ms']})
}).on('reconnect_failed', function(){
    message({ message: ['System', 'Reconnected to server FAILED.']})
});
