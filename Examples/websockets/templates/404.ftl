<#-- @ftlvariable name="user" type="String" -->
<#-- @ftlvariable name="request" type="org.mbte.gretty.httpserver.GrettyHttpRequest" -->
<html>
    <head>
        <title>404 - Page Not Found</title>
    </head>
    <body>
        <p>Hello, ${user}!</p>
        <p>Unfortunately, I have no idea about page <i>${request.uri}</i>, which you've requested</p>
        <p>Try our <a href='/'>main page</a> please</p>
    </body>
</html>
