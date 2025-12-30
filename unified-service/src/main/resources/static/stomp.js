var wsUrl;
var prefix = window.location.pathname.substr(0, window.location.pathname.lastIndexOf('/'))

if (window.location.protocol == 'https:') {
 wsUrl = 'wss://' + window.location.host + prefix + '/websocket'
} else {
 wsUrl = 'ws://' + window.location.host + prefix + '/websocket'
}

const stompClient = new StompJs.Client({
    brokerURL: wsUrl
});

stompClient.onConnect = (frame) => {
    setConnected(true);
    console.log('Connected: ' + frame);
    let handleServerMsg = (msg) => {
        console.log("Raw msg: ", msg)
        let parsed = JSON.parse(msg.body)
        updateUi(parsed);
    }

    if (PAGE_TOPIC !== null) {
        stompClient.subscribe(PAGE_TOPIC, handleServerMsg);
    }
    stompClient.subscribe('/user/queue/reply', handleServerMsg);

    initPage()
};

stompClient.onWebSocketError = (error) => {
    console.error('Error with websocket', error);
};

stompClient.onStompError = (frame) => {
    console.error('Broker reported error: ' + frame.headers['message']);
    console.error('Additional details: ' + frame.body);
};

function setConnected(connected) {
    $("#connect").prop("disabled", connected);
    $("#disconnect").prop("disabled", !connected);
    if (connected) {
        $("#conversation").show();
    }
    else {
        $("#conversation").hide();
    }
    $("#greetings").html("");
}

function connect() {
    stompClient.activate();
}

function disconnect() {
    stompClient.deactivate();
    setConnected(false);
    console.log("Disconnected");
}

function sendName() {
    console.log("Doing nothing")
}

function showGreeting(message) {
    $("#el1")[0].innerText = message
    $("#greetings").append("<tr><td>" + message + "</td></tr>");
}

$(function () {
    $("form").on('submit', (e) => e.preventDefault());
    $( "#connect" ).click(() => connect());
    $( "#disconnect" ).click(() => disconnect());
    $( "#send" ).click(() => sendName());
    stompClient.activate();

});