const isLocalHost = ["localhost", "127.0.0.1"].includes(
  window.location.hostname,
);

window.__APP_CONFIG__ = {
  // Lokal testde avtomatik olaraq lokal backend-e qoşulur.
  apiBaseUrl: isLocalHost
    ? "http://localhost:8081"
    : "https://lirios-management-system.onrender.com",
};
