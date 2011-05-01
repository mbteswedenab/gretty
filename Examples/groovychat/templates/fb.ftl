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

      <script type="text/javascript" src="swfobject.js"></script>
      <script type="text/javascript" src="FABridge.js"></script>
      <script type="text/javascript" src="web_socket.js"></script>

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
    <table style="width:100%">
        <tr>
            <td align='left'>
                <fb:login-button autologoutlink="true" perms="email"></fb:login-button>
            </td>
            <td style="width:90%" valign="top">
                <#if user?? >
                    <p>Welcome to WebSocket Chat, <i>${user.name}</i></p>
                <#else>
                    <p>You must be logged in with your Facebook account to enjoy our chat<br></p>
                </#if>
            </td>
        </tr>
    </table>
    <#if user?? >
        <div style="font-size:smaller;">Connection: <span id='connectionStatus'></span></div>
        <div><form onsubmit="sendChat(); return false"><input id="message" type="text" size="100"><input type="submit" value="Send" on></form></div>
        <div id='profilePicsDiv'></div>
        <div id="log"></div>
    </#if>

    <#include "./include/fbroot.ftl">
  </body>
</html>