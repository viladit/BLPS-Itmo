const username = document.querySelector("#username");
const password = document.querySelector("#password");
const failNotification = document.querySelector("#failNotification");
const result = document.querySelector("#result");
const orders = document.querySelector("#orders");
const notifications = document.querySelector("#notifications");
const wsStatus = document.querySelector("#wsStatus");
const toastRoot = document.querySelector("#toastRoot");
const knownNotificationIds = new Set();
let notificationsLoaded = false;
let stompSocket = null;
let stompBuffer = "";

document.querySelector("#createOrder").addEventListener("click", createOrder);
document.querySelector("#runReminders").addEventListener("click", runPendingReminders);
document.querySelector("#refreshData").addEventListener("click", refreshData);
const createOrderButton = document.querySelector("#createOrder");

function authHeader() {
  return "Basic " + btoa(`${username.value}:${password.value}`);
}

function notificationSocketUrl() {
  return `${window.location.protocol === "https:" ? "wss" : "ws"}://${window.location.hostname}:8081/ws-notifications-native`;
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      "Authorization": authHeader(),
      "Content-Type": "application/json",
      ...(options.headers || {})
    }
  });
  const text = await response.text();
  const body = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw body || { message: `HTTP ${response.status}` };
  }
  return body;
}

async function createOrder() {
  const demoDelaySeconds = 10;
  const query = failNotification.checked
    ? `?notificationPauseSeconds=${demoDelaySeconds}`
    : "";
  const payload = {
    customerName: "Ivan Petrov",
    deliveryAddress: "Saint Petersburg, Nevsky 1",
    items: [
      { sku: "SKU-1", productName: "Phone", quantity: 2, unitPrice: 499.99 },
      { sku: "SKU-2", productName: "Charger", quantity: 1, unitPrice: 29.99 }
    ]
  };

  const stopTimer = failNotification.checked ? startFailureDemoTimer(demoDelaySeconds) : () => {};
  createOrderButton.disabled = true;
  try {
    const created = await api(`/api/orders${query}`, {
      method: "POST",
      body: JSON.stringify(payload)
    });
    result.textContent = JSON.stringify(created, null, 2);
  } catch (error) {
    result.textContent = JSON.stringify(error, null, 2);
  } finally {
    stopTimer();
    createOrderButton.disabled = false;
  }
  await refreshData();
}

function startFailureDemoTimer(seconds) {
  let left = seconds;
  const command = "docker compose stop notifications-db";
  result.textContent = [
    "JTA-транзакция началась: заказ записан в orders-db, уведомление записано в notifications-db.",
    `В течение ${left} сек остановите БД уведомлений:`,
    command
  ].join("\n");

  const timer = setInterval(() => {
    left -= 1;
    result.textContent = [
      "Транзакция всё ещё открыта. После таймера приложение снова обратится к notifications-db.",
      `Осталось ${Math.max(left, 0)} сек. Контейнер notifications-db должен быть остановлен.`,
      command
    ].join("\n");
  }, 1000);

  return () => clearInterval(timer);
}

async function refreshData() {
  try {
    const [orderData, notificationData] = await Promise.all([
      api("/api/orders"),
      api("/api/notifications")
    ]);
    renderOrders(orderData);
    renderNotifications(notificationData);
  } catch (error) {
    result.textContent = JSON.stringify(error, null, 2);
  }
}

async function runPendingReminders() {
  try {
    const response = await api("/api/orders/pending-reminders/run", {
      method: "POST"
    });
    result.textContent = JSON.stringify(response, null, 2);
  } catch (error) {
    result.textContent = JSON.stringify(error, null, 2);
  }
  await refreshData();
}

function renderOrders(items) {
  orders.innerHTML = "";
  if (!items.length) {
    orders.innerHTML = `<p class="muted">Заказов пока нет.</p>`;
    return;
  }
  for (const order of items) {
    orders.insertAdjacentHTML("beforeend", `
      <div class="item">
        <strong>#${order.id} ${order.status}</strong>
        <div>${order.customerName}</div>
        <div class="muted">${order.deliveryAddress}</div>
      </div>
    `);
  }
}

function renderNotifications(items) {
  notifications.innerHTML = "";
  if (!items.length) {
    notifications.innerHTML = `<p class="muted">Уведомлений пока нет.</p>`;
    notificationsLoaded = true;
    return;
  }
  for (const notification of items) {
    const isNew = notificationsLoaded && !knownNotificationIds.has(notification.id);
    knownNotificationIds.add(notification.id);

    notifications.insertAdjacentHTML("beforeend", `
      <div class="item notification-item${isNew ? " notification-item--new" : ""}">
        <div class="notification-item__head">
          <strong>${notification.type}</strong>
          ${isNew ? `<span>new</span>` : ""}
        </div>
        <div>Заказ #${notification.orderId}</div>
        <div>${notification.message}</div>
        <div class="muted">${notification.createdAt}</div>
      </div>
    `);
  }
  notificationsLoaded = true;
}

function connectNotifications() {
  wsStatus.textContent = "WebSocket уведомления: подключение...";
  stompSocket = new WebSocket(notificationSocketUrl());

  stompSocket.addEventListener("open", () => {
    sendStomp("CONNECT", {
      "accept-version": "1.2",
      "heart-beat": "10000,10000"
    });
  });

  stompSocket.addEventListener("message", event => {
    stompBuffer += event.data;
    let frameEnd = stompBuffer.indexOf("\0");
    while (frameEnd !== -1) {
      const rawFrame = stompBuffer.slice(0, frameEnd);
      stompBuffer = stompBuffer.slice(frameEnd + 1);
      handleStompFrame(rawFrame);
      frameEnd = stompBuffer.indexOf("\0");
    }
  });

  stompSocket.addEventListener("close", () => {
    wsStatus.textContent = "WebSocket уведомления: переподключение...";
    setTimeout(connectNotifications, 3000);
  });

  stompSocket.addEventListener("error", () => {
    wsStatus.textContent = "WebSocket уведомления: ошибка подключения";
  });
}

function sendStomp(command, headers = {}, body = "") {
  const lines = [command, ...Object.entries(headers).map(([key, value]) => `${key}:${value}`), "", body];
  stompSocket.send(`${lines.join("\n")}\0`);
}

function handleStompFrame(rawFrame) {
  if (!rawFrame || rawFrame === "\n") {
    return;
  }

  const [headerPart, body = ""] = rawFrame.split("\n\n");
  const command = headerPart.split("\n")[0];

  if (command === "CONNECTED") {
    wsStatus.textContent = "WebSocket уведомления: подключено";
    sendStomp("SUBSCRIBE", {
      id: "orders-live",
      destination: "/topic/orders",
      ack: "auto"
    });
    return;
  }

  if (command === "MESSAGE") {
    showToast(JSON.parse(body));
  }
}

function showToast(notification) {
  const toast = document.createElement("div");
  toast.className = "toast";
  toast.innerHTML = `
    <div class="toast__top">
      <strong>Заказ #${notification.orderId}</strong>
      <span>${notification.newStatus}</span>
    </div>
    <div>${notification.customerName}</div>
    <p>${notification.message}</p>
  `;
  toastRoot.appendChild(toast);

  setTimeout(() => {
    toast.classList.add("toast--leaving");
    toast.addEventListener("animationend", () => toast.remove(), { once: true });
  }, 5200);
}

refreshData();
connectNotifications();
