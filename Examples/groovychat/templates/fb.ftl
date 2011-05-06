<#-- @ftlvariable name="applicationId" type="String" -->
<#-- @ftlvariable name="userName" type="String" -->
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml"
      xmlns:fb="http://www.facebook.com/2008/fbml">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
      <title>Chat Demo with Facebook and Groovy Websockets</title>

  <#if user?? >
      <script type="text/javascript" src="http://ajax.googleapis.com/ajax/libs/prototype/1.6.1/prototype.js"></script>

      <script type="text/javascript" src="/swfobject.js"></script>
      <script type="text/javascript" src="/FABridge.js"></script>
      <script type="text/javascript" src="/web_socket.js"></script>

      <script type="text/javascript">
        WEB_SOCKET_SWF_LOCATION = "WebSocketMain.swf";
      </script>

      <script src="/json.js"></script> <!-- for ie -->
      <script src="/socket.io.js"></script>

      <script type="text/javascript">
          var accessToken = '${accessToken}'
          var userName = '${user.name}'
          var userId = '${user.id}'
          var userImg = 'http://graph.facebook.com/${user.id}/picture'
      </script>

      <script type="text/javascript" src="js/application.js"></script>
  <#else>
      <script type="text/javascript">function load() {}</script>
  </#if>

  </head>
  <body onload="load()">
  <table width="800px">
      <tr>
          <td valign=center>
              <h1>Sample chat with Facebook</h1>
          </td>
          <td valign=center align=right>
          <#if user?? >
              <i>User: ${user.name}</i>
          </#if>
              <fb:login-button autologoutlink="true" perms="email"></fb:login-button>
          </td>
      </tr>
  </table>

  <#if user?? >
      <div id="chat"><p>Connecting...</p></div>
      <form id="form" onSubmit="send(); return false">
        <input type="text" autocomplete="off" id="text"><input type="submit" value="Send">
      </form>
  <#else>
      <div id="warning">You must be logged in to be able to participate in the chat</div>
  </#if>

  <style>
    #chat { height: 300px; overflow: auto; width: 800px; border: 1px solid #eee; font: 13px Helvetica, Arial; }
    #chat p { padding: 8px; margin: 0; }
    #chat p:nth-child(odd) { background: #F6F6F6; }
    #form { width: 782px; background: #333; padding: 5px 10px; display: block; }
    #form input[type=text] { width: 700px; padding: 5px; background: #fff; border: 1px solid #fff; }
    #form input[type=submit] { cursor: pointer; background: #999; border: none; padding: 6px 8px; -moz-border-radius: 8px; -webkit-border-radius: 8px; margin-left: 5px; text-shadow: 0 1px 0 #fff; }
    #form input[type=submit]:hover { background: #A2A2A2; }
    #form input[type=submit]:active { position: relative; top: 2px; }
    #warning {border: 2px solid #eee; background: #f4a460; width: 800px; padding: 6px; font: 17px Helvetica, Arial; }
  </style>

    <#include "./include/fbroot.ftl">
  </body>
</html>