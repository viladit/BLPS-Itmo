var stompClient = null;

function connect() {
    var orderId = document.getElementById('orderId').value;
    if(!orderId) {
        alert("Пожалуйста, введите ID заказа");
        return;
    }

    var socket = new SockJS('/ws-notifications');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {
        console.log('Подключено: ' + frame);
        var notifBox = document.getElementById("notifications");
        notifBox.innerHTML = "<div style='color: green;'>Успешно подключено к топику заказа #" + orderId + "</div>";

        stompClient.subscribe('/topic/orders/' + orderId, function (message) {
            showNotification(JSON.parse(message.body));
        });
    });
}

function showNotification(data) {
    var box = document.getElementById('notifications');

    var html = `
        <div class='notification'>
            <div>Заказ #${data.orderId} — <span class="status-badge">${data.newStatus}</span></div>
            <div style="margin-top: 5px; color: #555;">Клиент: ${data.customerName}</div>
            <div style="margin-top: 5px;"><i>${data.message}</i></div>
        </div>
    `;

    box.innerHTML += html;
    box.scrollTop = box.scrollHeight;
}