window.jane = {};

jane.getTime = function () {
    return (new Date()).toLocaleString();
};

jane.closed = false;

jane.close = function(message) {
    jane.closed = true;
    document.title = message;
    document.body.innerHTML = jane.getTime() + ' ' + message;
};

jane['reload page'] = function () {
    jane.closed = true;
    document.title = "Reloading page......";
    window.location.reload();
};

jane['close page'] = function () {    
    jane.close("Page is closed");
};

jane.counter = 0;

jane['heartbeat'] = function () {    
    if (jane.counter === 0) {
        document.title = '00 ' + document.title;
        jane.counter = 101;
        return;
    }

    document.title = String(jane.counter).slice(1) + document.title.slice(2);
    jane.counter++;
    if (jane.counter === 1000) {
        jane.counter = 100;
    }
};

jane.onMessage = function (event) {
    var func = jane[event.data];

    if (!func) {
        jane.close('Unknown command: ' + event.data);
        return;
    }

    func();
};

jane.onDisconnect = function () {
    jane.close("Connetction with the development websocked server has broken");
};

jane.onError = function() {
    jane.close("Failed to connect to the development websocked server");
};

jane.connect = function() {
    var ws = new WebSocket('ws://' + location.host + '/reload-on-change');
    ws.addEventListener('error', jane.onError);
    ws.addEventListener('message', jane.onMessage);
    ws.addEventListener('close', jane.onDisconnect);
};

jane.connect();