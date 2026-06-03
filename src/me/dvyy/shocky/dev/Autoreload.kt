package me.dvyy.shocky.dev

import org.intellij.lang.annotations.Language

fun js(@Language("javascript") text: String): String = text

fun autoReloadScript() = js(
    """
let socket;
let reconnectInterval = 1000; // Initial reconnect interval in ms

function connectWebSocket() {
    socket = new WebSocket('ws://' + location.host + '/ping');

    socket.onopen = function(event) {
        console.log('WebSocket is open now.');
        reconnectInterval = 1000; // Reset the reconnect interval on successful connection
    };

    socket.onmessage = function(event) {
        console.log('WebSocket message received:', event.data);
        location.reload();
    };

    socket.onclose = function(event) {
        console.log('WebSocket is closed now. Reconnecting...');
        setTimeout(connectWebSocket, reconnectInterval);
        reconnectInterval = Math.min(reconnectInterval * 2, 5000); // Exponential backoff up to 5 seconds
    };

    socket.onerror = function(error) {
        console.error('WebSocket error observed:', error);
        socket.close();
    };
}

// Start the WebSocket connection
connectWebSocket();
""".trimIndent()
)
