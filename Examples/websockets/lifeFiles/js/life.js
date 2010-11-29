var conn

var cells = {}
var editable = true
var iterator

function addArg(arg,method) {
  return function() {
    return method(arg)
  }
}

function init() {
    if(WebSocket.pseudo) {
        $('supported').show()
    }

    var table = $('table')
    for(i = 0; i != 48; i += 1) {
        cells [i] = {}
        var tr = document.createElement('tr')
        tr.style.height = '14px'
        for(j = 0; j != 48; j += 1) {
            var td = document.createElement('td')
            cells [i][j] = {x:i, y:j, live:false, td:td}
            td.setAttribute('style', 'width: 14px;  border-left:1px solid #5f9ea0; border-top: 1px solid #5f9ea0;')
            td.onclick = addArg(cells[i][j],function (cell) {
                if(editable) {
                    cell.live = !cell.live
                    cell.td.style.backgroundColor = cell.live ? 'blue' : 'aqua'
                }
            })
            tr.appendChild(td)
        }
        table.appendChild(tr)
    }

    conn = new WebSocket("ws://" + window.location.host + window.location.pathname);
    conn.onmessage = function (msg) {
        msg = msg.data.evalJSON(false)
        for(i = 0; i != 48; i += 1) {
            for(j = 0; j != 48; j += 1) {
                cells[i][j].live = false
            }
        }
        for(k = 0; k != msg.live.length; k += 1) {
            var c = msg.live[k]
            cells[c.x][c.y].live = true
        }
        for(i = 0; i != 48; i += 1) {
            for(j = 0; j != 48; j += 1) {
                var cell = cells[i][j]
                cell.td.style.backgroundColor = cell.live ? 'blue' : 'aqua'
            }
        }
        sendPos ()
    }
}

function sendPos() {
    var send = {width:48, height:48, live:[]}
    for (i = 0; i != 48; i += 1) {
        for (j = 0; j != 48; j += 1) {
            if (cells [i][j].live) {
                send.live.push({x:i,y:j})
            }
        }
    }
    conn.send(Object.toJSON(send))
}

function startStop () {
    if(iterator) {
        $('startStop').innerHTML = 'Start'
        clearInterval(iterator)
        iterator = null
        return
    }

    $('startStop').innerHTML = 'Pause'
    sendPos();
}
