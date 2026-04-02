// ═══════════════════════════════════════════════════════════
// LIRIOS BEAUTY CRM - FULL PRODUCTION VERSION v2
// API base URL: configurable via window.__APP_CONFIG__.apiBaseUrl
// ═══════════════════════════════════════════════════════════

const API =
  window.__APP_CONFIG__?.apiBaseUrl ||
  (["localhost:5500", "127.0.0.1:5500"].includes(window.location.host)
    ? "http://localhost:8080"
    : "");
const ACCESS_TOKEN_KEY = "auth_token";
const REFRESH_TOKEN_KEY = "refresh_token";

const nativeFetch = window.fetch.bind(window);
window.fetch = async (input, init = {}) => {
  const url = typeof input === "string" ? input : input?.url || "";
  const headers = new Headers(init.headers || {});

  const isApiRequest =
    url.startsWith(`${API}/api/`) ||
    url.startsWith("/api/") ||
    url.includes("/api/");
  const isPublicAuth =
    url.includes("/api/auth/login") || url.includes("/api/auth/refresh");

  if (isApiRequest && !isPublicAuth) {
    const token = localStorage.getItem(ACCESS_TOKEN_KEY);
    if (token) {
      headers.set("Authorization", `Bearer ${token}`);
    }
  }

  const response = await nativeFetch(input, { ...init, headers });

  if (
    isApiRequest &&
    !isPublicAuth &&
    (response.status === 401 || response.status === 403)
  ) {
    handleAuthFailure();
  }

  return response;
};

async function readApiError(res, fallback = "Xəta baş verdi") {
  try {
    const text = await res.text();
    if (!text) return fallback;
    try {
      const parsed = JSON.parse(text);
      return parsed.message || parsed.error || fallback;
    } catch (_) {
      return text;
    }
  } catch (_) {
    return fallback;
  }
}

async function fetchJson(url, options = {}, fallback = "Sorğu uğursuz oldu") {
  const res = await fetch(url, options);
  if (!res.ok) {
    throw new Error(await readApiError(res, fallback));
  }
  if (res.status === 204) return null;
  return res.json();
}

// Global State
let cart = [];
let products = [];
let employees = [];
let customers = [];
let currentCustomer = null;
let currentUser = null;
let barcodeReader = null;
let barcodeLib = null;
let barcodeLibLoadingPromise = null;
let posScannerControls = null;
let productScannerControls = null;
let existingProduct = null; // Barkod yoxlayanda tapılan məhsul

// Currency rates
let usdRate = 1.7;
let eurRate = 1.85;

// ═══════════════════════════════════════════════════════════
// AUTHENTICATION
// ═══════════════════════════════════════════════════════════

document.addEventListener("DOMContentLoaded", () => {
  checkAuth();
});

function checkAuth() {
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  const user = localStorage.getItem("user_info");

  if (!token || !user) {
    showLoginPage();
  } else {
    currentUser = JSON.parse(user);
    initApp();
  }
}

function showLoginPage() {
  document.body.style.display = "block";
  document.body.style.margin = "0";
  document.body.style.overflow = "hidden";
  document.body.style.background = "#0f172a";

  document.body.innerHTML = `
        <style>
          @import url('https://fonts.googleapis.com/css2?family=Manrope:wght@400;600;700;800&display=swap');

          html,
          body {
            width: 100%;
            height: 100%;
          }

          .auth-shell {
            position: fixed;
            inset: 0;
            display: grid;
            place-items: center;
            padding: 24px;
            background:
              radial-gradient(circle at 15% 20%, #ffd5b4 0%, transparent 32%),
              radial-gradient(circle at 85% 10%, #b9f2df 0%, transparent 28%),
              linear-gradient(160deg, #0f172a 0%, #1f2f4c 48%, #2e4d7d 100%);
            font-family: 'Manrope', sans-serif;
          }

          .auth-card {
            width: 100%;
            max-width: 430px;
            border-radius: 28px;
            padding: 30px;
            background: rgba(255, 255, 255, 0.94);
            border: 1px solid rgba(255, 255, 255, 0.45);
            box-shadow: 0 25px 70px rgba(15, 23, 42, 0.35);
            backdrop-filter: blur(10px);
          }

          .auth-brand {
            text-align: center;
            margin-bottom: 22px;
          }

          .auth-logo {
            width: 68px;
            height: 68px;
            margin: 0 auto 12px;
            border-radius: 18px;
            background: linear-gradient(145deg, #2d6cdf 0%, #1c3d8b 100%);
            color: #fff;
            display: grid;
            place-items: center;
            font-size: 32px;
            font-weight: 800;
          }

          .auth-title {
            margin: 0;
            font-size: 34px;
            font-weight: 800;
            color: #0f172a;
            letter-spacing: -0.02em;
          }

          .auth-subtitle {
            margin: 6px 0 0;
            color: #50607f;
            font-weight: 600;
          }

          .auth-form {
            display: grid;
            gap: 14px;
          }

          .auth-input {
            border: 1px solid #d7e2f7;
            border-radius: 12px;
            padding: 13px 14px;
            font-size: 15px;
            background: #f8fbff;
            transition: border-color 0.2s, box-shadow 0.2s;
          }

          .auth-input:focus {
            outline: none;
            border-color: #2d6cdf;
            box-shadow: 0 0 0 3px rgba(45, 108, 223, 0.18);
          }

          .auth-button {
            margin-top: 6px;
            border: none;
            border-radius: 12px;
            padding: 13px;
            font-size: 17px;
            font-weight: 800;
            color: #fff;
            cursor: pointer;
            background: linear-gradient(135deg, #2d6cdf 0%, #1e4fb3 100%);
            box-shadow: 0 14px 28px rgba(30, 79, 179, 0.32);
            transition: transform 0.2s, box-shadow 0.2s;
          }

          .auth-button:hover {
            transform: translateY(-1px);
            box-shadow: 0 18px 34px rgba(30, 79, 179, 0.36);
          }

          .auth-error {
            margin-top: 14px;
            min-height: 20px;
            color: #dc2626;
            text-align: center;
            font-weight: 600;
            font-size: 14px;
          }

          @media (max-width: 480px) {
            .auth-shell {
              padding: 14px;
            }
            .auth-card {
              padding: 24px 18px;
              border-radius: 22px;
            }
            .auth-title {
              font-size: 30px;
            }
          }
        </style>

        <div class="auth-shell">
          <div class="auth-card">
            <div class="auth-brand">
              <div class="auth-logo">L</div>
              <h2 class="auth-title">Lirios Beauty</h2>
              <p class="auth-subtitle">CRM Panel</p>
            </div>

            <form onsubmit="handleLogin(event)" class="auth-form">
              <input type="text" id="login-username" class="auth-input" placeholder="İstifadəçi adı" required>
              <input type="password" id="login-password" class="auth-input" placeholder="Şifrə" required>
              <button type="submit" class="auth-button">Daxil ol</button>
            </form>

            <div id="login-error" class="auth-error"></div>
          </div>
        </div>
    `;
}

async function handleLogin(e) {
  e.preventDefault();
  const username = document.getElementById("login-username").value;
  const password = document.getElementById("login-password").value;

  try {
    const res = await fetch(`${API}/api/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    });

    if (!res.ok) {
      throw new Error(await readApiError(res, "Giriş uğursuz oldu"));
    }

    const data = await res.json();
    localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken || "");
    localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken || "");
    localStorage.setItem("user_info", JSON.stringify(data.user || {}));
    currentUser = data.user || null;
    location.reload();
  } catch (err) {
    document.getElementById("login-error").textContent =
      err.message || "Giriş uğursuz oldu!";
  }
}

function logout() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem("user_info");
  location.reload();
}

function handleAuthFailure() {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem("user_info");
  if (!document.getElementById("login-username")) {
    location.reload();
  }
}

function initApp() {
  document.body.style.display = "";
  document.body.style.margin = "";
  document.body.style.overflow = "";
  document.body.style.background = "";

  initNav();
  loadCurrency();
  const initialPage = getInitialPage();
  activatePage(initialPage, false);
  displayUserInfo();
}

function displayUserInfo() {
  const sidebar = document.querySelector(".sidebar-footer");
  if (sidebar && currentUser) {
    const currencyHTML = sidebar.innerHTML;
    sidebar.innerHTML = `
            <div style="padding:16px 0;border-bottom:1px solid rgba(255,255,255,0.1);margin-bottom:12px">
                <div style="display:flex;align-items:center;gap:12px;padding:0 8px">
                    <div style="width:40px;height:40px;background:#2563eb;border-radius:50%;display:flex;align-items:center;justify-content:center;color:white;font-weight:700;font-size:16px">${currentUser.fullName.charAt(0)}</div>
                    <div style="flex:1">
                        <div style="color:white;font-weight:600;font-size:14px">${currentUser.fullName}</div>
                        <div style="color:rgba(255,255,255,0.6);font-size:12px">${currentUser.role}</div>
                    </div>
                    <button onclick="logout()" style="background:transparent;border:none;color:rgba(255,255,255,0.7);cursor:pointer;padding:8px" title="Çıxış">
                        <svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1"/>
                        </svg>
                    </button>
                </div>
            </div>
            ${currencyHTML}
        `;
  }
}

// ═══════════════════════════════════════════════════════════
// NAVIGATION
// ═══════════════════════════════════════════════════════════

function initNav() {
  document.querySelectorAll(".nav-item").forEach((item) => {
    item.addEventListener("click", (e) => {
      const externalHref = item.getAttribute("href");
      if (item.classList.contains("nav-external") && externalHref) {
        return;
      }

      e.preventDefault();
      const page = item.dataset.page;

      if (!page) return;
      activatePage(page, true);
    });
  });

  window.addEventListener("hashchange", () => {
    activatePage(getInitialPage(), false);
  });
}

function getInitialPage() {
  const page = (window.location.hash || "").replace("#", "").trim();
  const allowedPages = [
    "dashboard",
    "pos",
    "products",
    "orders",
    "customers",
    "employees",
    "debts",
  ];
  return allowedPages.includes(page) ? page : "dashboard";
}

function activatePage(page, updateHash = true) {
  const targetPage = document.getElementById(page);
  if (!targetPage) return;

  document
    .querySelectorAll(".nav-item")
    .forEach((n) => n.classList.remove("active"));
  document
    .querySelector(`.nav-item[data-page="${page}"]`)
    ?.classList.add("active");

  document
    .querySelectorAll(".page")
    .forEach((p) => p.classList.remove("active"));
  targetPage.classList.add("active");

  const loaders = {
    dashboard: loadDashboard,
    pos: loadPOS,
    products: loadProducts,
    orders: loadOrders,
    customers: loadCustomers,
    employees: loadEmployees,
    debts: loadDebts,
  };

  if (loaders[page]) loaders[page]();

  if (updateHash) {
    window.location.hash = page;
  }

  if (window.matchMedia("(max-width: 768px)").matches) {
    closeSidebar();
  }
}

function toggleSidebar() {
  const sidebar = document.querySelector(".sidebar");
  const overlay = document.getElementById("sidebar-overlay");
  if (!sidebar || !overlay) return;
  sidebar.classList.toggle("active");
  overlay.classList.toggle("active");
}

function closeSidebar() {
  const sidebar = document.querySelector(".sidebar");
  const overlay = document.getElementById("sidebar-overlay");
  sidebar?.classList.remove("active");
  overlay?.classList.remove("active");
}

// ═══════════════════════════════════════════════════════════
// CURRENCY
// ═══════════════════════════════════════════════════════════

async function loadCurrency() {
  try {
    const res = await fetch("https://api.exchangerate-api.com/v4/latest/AZN");
    const data = await res.json();
    usdRate = (1 / data.rates.USD).toFixed(2);
    eurRate = (1 / data.rates.EUR).toFixed(2);
    document.getElementById("usd-rate").textContent = usdRate;
    document.getElementById("eur-rate").textContent = eurRate;
  } catch (e) {}
}

// ═══════════════════════════════════════════════════════════
// DASHBOARD
// ═══════════════════════════════════════════════════════════

async function loadDashboard() {
  try {
    const [products, orders, totalDebt, summary] = await Promise.all([
      fetchJson(`${API}/api/products`, {}, "Məhsullar yüklənmədi"),
      fetchJson(`${API}/api/orders`, {}, "Sifarişlər yüklənmədi"),
      fetchJson(`${API}/api/orders/total-debt`, {}, "Borc məlumatı yüklənmədi"),
      fetchJson(
        `${API}/api/export/summary`,
        {},
        "Hesabat məlumatı yüklənmədi",
      ).catch(() => null),
    ]);

    const activeOrders = orders.filter((o) => o.status !== "CANCELLED");

    // Ümumi dövriyyə: stokda olan məhsulların satış dəyəri cəmi
    const inventoryTurnover = products.reduce((sum, p) => {
      const qty = parseFloat(p.stockQuantity ?? p.stockQty ?? 0) || 0;
      const price = parseFloat(p.sellingPrice ?? p.price ?? 0) || 0;
      return sum + qty * price;
    }, 0);

    const realizedRevenue =
      summary?.totalRevenue != null
        ? parseFloat(summary.totalRevenue)
        : activeOrders.reduce((sum, o) => sum + parseFloat(o.totalAmount || 0), 0);

    // Ümumi dövriyyə: cari stok dəyəri + satılmış malların dəyəri
    // Bu yanaşma satış zamanı stok azalanda dövriyyənin düşməsinin qarşısını alır.
    const turnover = inventoryTurnover + realizedRevenue;
    const expenses =
      summary?.totalExpenses != null ? parseFloat(summary.totalExpenses) : 0;
    const purchaseCosts =
      summary?.totalCost != null ? parseFloat(summary.totalCost) : 0;
    const netProfitRaw =
      summary?.netProfit != null
        ? parseFloat(summary.netProfit)
        : turnover - purchaseCosts - expenses;
    const netProfit = Math.max(0, netProfitRaw);
    const debtValue =
      summary?.totalDebt != null
        ? parseFloat(summary.totalDebt)
        : parseFloat(totalDebt || 0);

    document.getElementById("total-turnover").textContent =
      `${formatNumber(turnover)} ₼`;
    document.getElementById("net-profit").textContent =
      `${formatNumber(netProfit)} ₼`;
    document.getElementById("total-expenses").textContent =
      `${formatNumber(expenses)} ₼`;
    document.getElementById("total-debts").textContent =
      `${formatNumber(debtValue)} ₼`;
    document.getElementById("total-products").textContent = products.length;
    document.getElementById("total-orders").textContent = activeOrders.length;

    renderRecentOrders(activeOrders.slice(0, 10));
  } catch (e) {
    console.error("Dashboard error:", e);
    showToast("Dashboard yüklənmədi: " + e.message, "error");
  }
}

function renderRecentOrders(orders) {
  const tbody = document.getElementById("recent-orders");
  if (!orders || !orders.length) {
    tbody.innerHTML =
      '<tr><td colspan="5" style="text-align:center;padding:32px;color:#94a3b8;">Məlumat yoxdur</td></tr>';
    return;
  }

  tbody.innerHTML = orders
    .map(
      (o) => `
        <tr>
            <td>#${o.id}</td>
            <td>${o.customer?.fullName || "Anonim"}</td>
            <td><strong>${o.totalAmount} ₼</strong></td>
            <td><span class="badge badge-${getStatusClass(o.paymentStatus)}">${getStatusText(o.paymentStatus)}</span></td>
            <td>${formatDate(o.orderedAt)}</td>
        </tr>
    `,
    )
    .join("");
}

// ═══════════════════════════════════════════════════════════
// POS
// ═══════════════════════════════════════════════════════════

async function loadPOS() {
  try {
    const [productsData, employeesData, customersData] = await Promise.all([
      fetchJson(`${API}/api/products`, {}, "Məhsullar yüklənmədi"),
      fetchJson(`${API}/api/employees/active`, {}, "İşçilər yüklənmədi"),
      fetchJson(`${API}/api/customers`, {}, "Müştərilər yüklənmədi"),
    ]);

    products = productsData;
    employees = employeesData;
    customers = customersData || [];

    renderProducts();
    renderCart();
    renderEmployeeDropdown();
    renderCustomerSelect(customers);
    bindPosPaymentEvents();

    // Search listener
    const searchInput = document.getElementById("product-search");
    if (searchInput) {
      searchInput.replaceWith(searchInput.cloneNode(true));
      document
        .getElementById("product-search")
        .addEventListener("input", (e) => {
          const query = e.target.value.toLowerCase();
          const filtered = products.filter(
            (p) =>
              p.productName.toLowerCase().includes(query) ||
              (p.barcode && p.barcode.includes(query)),
          );
          renderProducts(filtered);
        });
    }

    // Paid amount listener
    const paidInput = document.getElementById("paid-amount");
    if (paidInput) {
      paidInput.replaceWith(paidInput.cloneNode(true));
      document
        .getElementById("paid-amount")
        .addEventListener("input", calculateDebt);
    }
  } catch (e) {
    console.error("POS error:", e);
    showToast("Məhsullar yüklənmədi: " + e.message, "error");
  }
}

function bindPosPaymentEvents() {
  const methodSelect = document.getElementById("payment-method");
  if (methodSelect) {
    methodSelect.replaceWith(methodSelect.cloneNode(true));
    document
      .getElementById("payment-method")
      .addEventListener("change", onPaymentMethodChange);
  }

  const searchInput = document.getElementById("customer-search-input");
  if (searchInput) {
    searchInput.replaceWith(searchInput.cloneNode(true));
    document
      .getElementById("customer-search-input")
      .addEventListener("input", filterDebtCustomers);
  }

  const customerSelect = document.getElementById("customer-select");
  if (customerSelect) {
    customerSelect.replaceWith(customerSelect.cloneNode(true));
    document
      .getElementById("customer-select")
      .addEventListener("change", onDebtCustomerSelect);
  }

  onPaymentMethodChange();
}

function onPaymentMethodChange() {
  const method = document.getElementById("payment-method")?.value || "CASH";
  const debtSection = document.getElementById("debt-customer-section");
  const paidInput = document.getElementById("paid-amount");
  if (!debtSection || !paidInput) return;

  if (method === "DEBT") {
    debtSection.style.display = "block";
  } else {
    debtSection.style.display = "none";
    currentCustomer = null;
    const info = document.getElementById("customer-info");
    if (info) info.innerHTML = "";
  }
}

function renderCustomerSelect(list) {
  const select = document.getElementById("customer-select");
  if (!select) return;
  if (!list || !list.length) {
    select.innerHTML = "<option value=''>Müştəri yoxdur</option>";
    return;
  }
  select.innerHTML = list
    .map(
      (c) =>
        `<option value="${c.id}">${c.fullName} - ${c.phone || "—"}</option>`,
    )
    .join("");
}

function filterDebtCustomers() {
  const query = (
    document.getElementById("customer-search-input")?.value || ""
  ).toLowerCase();

  const filtered = customers.filter(
    (c) =>
      (c.fullName || "").toLowerCase().includes(query) ||
      (c.phone || "").toLowerCase().includes(query),
  );
  renderCustomerSelect(filtered);
}

function onDebtCustomerSelect() {
  const selectedId = document.getElementById("customer-select")?.value;
  const info = document.getElementById("customer-info");
  if (!selectedId) {
    currentCustomer = null;
    if (info) info.innerHTML = "";
    return;
  }

  currentCustomer =
    customers.find((c) => String(c.id) === String(selectedId)) || null;
  if (!currentCustomer || !info) return;

  const debt = parseFloat(currentCustomer.currentDebt || 0);
  info.innerHTML = `Müştəri: <strong>${currentCustomer.fullName}</strong><br/>Mövcud borc: <strong>${formatNumber(debt)} ₼</strong>`;
  info.style.color = debt > 0 ? "#ef4444" : "#2563eb";
}

function renderEmployeeDropdown() {
  const select = document.getElementById("sale-employee");
  if (!select) return;

  select.innerHTML =
    '<option value="">İşçi seç...</option>' +
    employees
      .map((e) => `<option value="${e.id}">${e.fullName}</option>`)
      .join("");
}

function renderProducts(list = products) {
  const container = document.getElementById("products-list");
  const availableProducts = list.filter((p) => p.stockQuantity > 0);

  if (!availableProducts || !availableProducts.length) {
    container.innerHTML =
      '<div style="grid-column:1/-1;text-align:center;padding:60px;color:#94a3b8;">Stokda məhsul yoxdur</div>';
    return;
  }

  container.innerHTML = availableProducts
    .map(
      (p) => `
        <div class="product-card" onclick="addToCart(${p.id})">
            <div class="name">${p.productName}</div>
            <div class="price">${p.sellingPrice} ₼</div>
            <div class="stock">Stok: ${p.stockQuantity}</div>
        </div>
    `,
    )
    .join("");
}

function addToCart(id) {
  const product = products.find((p) => p.id === id);
  if (!product) return;

  const existing = cart.find((c) => c.id === id);
  if (existing) {
    if (existing.qty < product.stockQuantity) {
      existing.qty++;
    } else {
      showToast("Stokda kifayət qədər məhsul yoxdur!", "warning");
      return;
    }
  } else {
    cart.push({
      id: product.id,
      name: product.productName,
      price: product.sellingPrice,
      qty: 1,
      maxStock: product.stockQuantity,
    });
  }
  renderCart();
  showToast(`${product.productName} əlavə edildi`);
}

function renderCart() {
  const container = document.getElementById("cart-items");
  if (!cart.length) {
    container.innerHTML = '<div class="empty-cart">Səbət boşdur</div>';
    document.getElementById("cart-total").textContent = "0.00 ₼";
    return;
  }

  container.innerHTML = cart
    .map(
      (item) => `
        <div class="cart-item">
            <div class="cart-item-info">
                <div class="cart-item-name">${item.name}</div>
                <div class="cart-item-price">${item.price} ₼ × ${item.qty} = ${(item.price * item.qty).toFixed(2)} ₼</div>
            </div>
            <div class="qty-controls">
                <button class="qty-btn" onclick="updateQty(${item.id}, -1)">−</button>
                <span class="qty">${item.qty}</span>
                <button class="qty-btn" onclick="updateQty(${item.id}, 1)">+</button>
            </div>
        </div>
    `,
    )
    .join("");

  const total = cart.reduce((sum, item) => sum + item.price * item.qty, 0);
  document.getElementById("cart-total").textContent = `${total.toFixed(2)} ₼`;
  calculateDebt();
}

function updateQty(id, delta) {
  const item = cart.find((c) => c.id === id);
  if (!item) return;

  item.qty += delta;
  if (item.qty <= 0) {
    cart = cart.filter((c) => c.id !== id);
  } else if (item.qty > item.maxStock) {
    item.qty = item.maxStock;
    showToast("Stok limiti!", "warning");
  }
  renderCart();
}

function clearCart() {
  if (cart.length && confirm("Səbəti təmizləmək istəyirsiniz?")) {
    cart = [];
    currentCustomer = null;
    const customerSearch = document.getElementById("customer-search-input");
    if (customerSearch) customerSearch.value = "";
    const customerSelect = document.getElementById("customer-select");
    if (customerSelect) customerSelect.selectedIndex = 0;
    document.getElementById("customer-info").innerHTML = "";
    document.getElementById("paid-amount").value = "";
    document.getElementById("sale-employee").value = "";
    const paymentMethod = document.getElementById("payment-method");
    if (paymentMethod) paymentMethod.value = "CASH";
    onPaymentMethodChange();
    renderCart();
  }
}

function calculateDebt() {
  const total =
    parseFloat(document.getElementById("cart-total").textContent) || 0;
  const paid = parseFloat(document.getElementById("paid-amount").value) || 0;
  const debt = total - paid;

  const display = document.getElementById("debt-display");
  if (debt > 0) {
    display.textContent = `Borc: ${debt.toFixed(2)} ₼`;
    display.style.display = "block";
  } else {
    display.style.display = "none";
  }
}

async function completeSale() {
  if (!cart.length) {
    showToast("Səbət boşdur!", "error");
    return;
  }

  const employeeId = document.getElementById("sale-employee").value;
  const paymentMethod =
    document.getElementById("payment-method")?.value || "CASH";

  const total =
    parseFloat(document.getElementById("cart-total").textContent) || 0;
  const paid = parseFloat(document.getElementById("paid-amount").value) || 0;

  if (paymentMethod === "DEBT" && !currentCustomer) {
    showToast("Borclu satış üçün müştəri seçin!", "error");
    return;
  }

  if (total - paid > 0 && !currentCustomer) {
    showToast("Borca satış üçün müştəri lazımdır!", "error");
    return;
  }

  if (paymentMethod === "DEBT" && total - paid <= 0) {
    showToast("Borclu satış üçün qalıq borc olmalıdır", "warning");
    return;
  }

  try {
    const endpoint =
      paymentMethod === "DEBT" ? `${API}/api/sales/debt` : `${API}/api/orders`;

    await fetchJson(
      endpoint,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body:
          paymentMethod === "DEBT"
            ? JSON.stringify({
                employeeId: employeeId ? parseInt(employeeId) : null,
                customerId: currentCustomer?.id,
                amount: paid,
                products: cart.map((i) => ({
                  productId: i.id,
                  quantity: i.qty,
                })),
              })
            : JSON.stringify({
                employeeId: employeeId ? parseInt(employeeId) : null,
                customerId: currentCustomer?.id || null,
                paidAmount: paid,
                items: cart.map((i) => ({ productId: i.id, quantity: i.qty })),
              }),
      },
      "Satış tamamlanmadı",
    );

    showToast("Satış tamamlandı!");
    clearCart();
    loadPOS();
    loadDashboard();
    loadOrders();
    loadDebts();
  } catch (e) {
    showToast(e.message || "Əlaqə xətası!", "error");
  }
}

// ═══════════════════════════════════════════════════════════
// PRODUCTS - YENİ WORKFLOW
// ═══════════════════════════════════════════════════════════

async function loadProducts() {
  try {
    const productList = await fetchJson(
      `${API}/api/products`,
      {},
      "Məhsullar yüklənmədi",
    );

    const tbody = document.getElementById("products-table");

    if (!productList || !productList.length) {
      tbody.innerHTML =
        '<tr><td colspan="7" style="text-align:center;padding:32px;color:#94a3b8;">Məhsul yoxdur</td></tr>';
      return;
    }

    tbody.innerHTML = productList
      .map((p) => {
        let statusBadge = "";
        if (p.stockQuantity === 0) {
          statusBadge = '<span class="badge badge-danger">Bitib</span>';
        } else if (p.stockQuantity <= (p.minStockLevel || 10)) {
          statusBadge = '<span class="badge badge-warning">Az qalıb</span>';
        } else {
          statusBadge = '<span class="badge badge-success">Stokda</span>';
        }

        return `
                <tr>
                    <td>#${p.id}</td>
                    <td>${p.barcode || "—"}</td>
                    <td><strong>${p.productName}</strong></td>
                    <td>${p.sellingPrice} ₼</td>
                    <td>${p.stockQuantity}</td>
                    <td>${statusBadge}</td>
                    <td>
                        <div class="stock-controls">
                            <input type="number" id="stock-step-${p.id}" class="stock-step-input" value="1" min="1">
                            <button class="btn-text" onclick="adjustProductStock(${p.id}, -1)">−</button>
                            <button class="btn-text" onclick="adjustProductStock(${p.id}, 1)">+</button>
                        </div>
                    </td>
                </tr>
            `;
      })
      .join("");
  } catch (e) {
    console.error("Products error:", e);
    showToast(e.message || "Məhsullar yüklənmədi", "error");
  }
}

function getStockStep(productId) {
  const stepInput = document.getElementById(`stock-step-${productId}`);
  const step = parseInt(stepInput?.value, 10);
  if (!step || step <= 0) {
    showToast("Miqdar 1-dən böyük olmalıdır", "warning");
    return null;
  }
  return step;
}

async function adjustProductStock(productId, direction) {
  const step = getStockStep(productId);
  if (step === null) return;

  const quantity = direction > 0 ? step : -step;

  try {
    await fetchJson(
      `${API}/api/products/${productId}/stock?quantity=${quantity}`,
      {
        method: "PATCH",
      },
      "Stok yenilənmədi",
    );

    showToast("Stok uğurla yeniləndi");
    loadProducts();
    if (document.getElementById("pos")?.classList.contains("active")) {
      loadPOS();
    }
  } catch (e) {
    showToast(e.message || "Stok yenilənmədi", "error");
  }
}

function openProductModal() {
  resetProductModal();
  document.getElementById("product-modal").classList.add("active");
}

function resetProductModal() {
  existingProduct = null;
  document.getElementById("step-barcode").style.display = "block";
  document.getElementById("step-stock-update").style.display = "none";
  document.getElementById("step-new-product").style.display = "none";
  document.getElementById("scanner-container").style.display = "none";
  document.getElementById("manual-barcode-input").style.display = "none";

  // Köhnə input dəyərlərini təmizlə
  document.getElementById("manual-barcode").value = "";
  document.getElementById("stock-add-qty").value = "1";
  document.getElementById("new-barcode").value = "";
  document.getElementById("new-name").value = "";
  document.getElementById("new-price").value = "";
  document.getElementById("new-foreign").value = "";
  document.getElementById("new-currency").value = "USD";
  document.getElementById("new-cost").value = "";
  document.getElementById("new-category").value = "";
  document.getElementById("new-stock").value = "";
  document.getElementById("existing-product-info").textContent = "";

  stopProductScanner();
}

function startProductScanner() {
  document.getElementById("scanner-container").style.display = "block";

  startBarcodeScanner("product-scanner", "product", (decodedText) => {
    showToast("Barkod oxundu: " + decodedText);
    stopProductScanner();
    checkBarcodeApi(decodedText);
  });
}

function stopProductScanner() {
  if (productScannerControls) {
    productScannerControls.stop();
    productScannerControls = null;
  }
  const container = document.getElementById("product-scanner");
  if (container) container.innerHTML = "";
}

function showManualBarcode() {
  document.getElementById("manual-barcode-input").style.display = "block";
}

function checkBarcode() {
  const barcode = document.getElementById("manual-barcode").value.trim();
  if (!barcode) {
    showToast("Barkod daxil et!", "error");
    return;
  }
  checkBarcodeApi(barcode);
}

async function checkBarcodeApi(barcode) {
  try {
    const res = await fetch(
      `${API}/api/products/barcode/${encodeURIComponent(barcode)}`,
    );

    if (res.ok) {
      // Məhsul mövcuddur - stok artırma
      existingProduct = await res.json();
      document.getElementById("step-barcode").style.display = "none";
      document.getElementById("step-stock-update").style.display = "block";
      document.getElementById("existing-product-info").textContent =
        `${existingProduct.productName} - Cari stok: ${existingProduct.stockQuantity}`;
    } else {
      // Yeni məhsul
      document.getElementById("step-barcode").style.display = "none";
      document.getElementById("step-new-product").style.display = "block";
      document.getElementById("new-barcode").value = barcode;
    }
  } catch (e) {
    showToast("Xəta baş verdi!", "error");
  }
}

async function addStock() {
  const qty = parseInt(document.getElementById("stock-add-qty").value);
  if (!qty || qty <= 0) {
    showToast("Miqdar düzgün deyil!", "error");
    return;
  }

  try {
    const res = await fetch(
      `${API}/api/products/barcode/${encodeURIComponent(existingProduct.barcode)}/stock?qty=${qty}`,
      {
        method: "PATCH",
      },
    );

    if (res.ok) {
      showToast(`${qty} ədəd stok əlavə edildi!`);
      closeModal("product-modal");
      loadProducts();
    } else {
      showToast("Xəta baş verdi!", "error");
    }
  } catch (e) {
    showToast("Əlaqə xətası!", "error");
  }
}

async function convertCurrency() {
  const foreign = parseFloat(document.getElementById("new-foreign").value);
  const currency = document.getElementById("new-currency").value;

  if (!foreign) {
    showToast("Məbləğ daxil et!", "error");
    return;
  }

  let azn = foreign;
  if (currency === "USD") azn = foreign * usdRate;
  if (currency === "EUR") azn = foreign * eurRate;

  document.getElementById("new-cost").value = azn.toFixed(2);
  showToast(`${foreign} ${currency} = ${azn.toFixed(2)} AZN`);
}

async function saveNewProduct() {
  const product = {
    barcode: document.getElementById("new-barcode").value,
    name: document.getElementById("new-name").value.trim(),
    price: parseFloat(document.getElementById("new-price").value),
    costPrice: parseFloat(document.getElementById("new-cost").value) || 0,
    category: document.getElementById("new-category").value.trim() || "Ümumi",
    stockQty: parseInt(document.getElementById("new-stock").value),
  };

  if (!product.name || !product.price || !product.stockQty) {
    showToast("Məcburi sahələri doldurun!", "error");
    return;
  }

  try {
    const res = await fetch(`${API}/api/products`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(product),
    });

    if (res.ok) {
      showToast("Məhsul əlavə edildi!");
      closeModal("product-modal");
      loadProducts();
    } else {
      const error = await res.text();
      showToast("Xəta: " + error, "error");
    }
  } catch (e) {
    showToast("Əlaqə xətası!", "error");
  }
}

// ═══════════════════════════════════════════════════════════
// ORDERS
// ═══════════════════════════════════════════════════════════

async function loadOrders() {
  try {
    const orders = await fetchJson(
      `${API}/api/orders`,
      {},
      "Sifarişlər yüklənmədi",
    );
    const tbody = document.getElementById("orders-table");

    if (!orders || !orders.length) {
      tbody.innerHTML =
        '<tr><td colspan="9" style="text-align:center;padding:32px;color:#94a3b8;">Sifariş yoxdur</td></tr>';
      return;
    }

    tbody.innerHTML = orders
      .map((o) => {
        const total = parseFloat(o.totalAmount || 0);
        const paid = parseFloat(o.paidAmount || 0);
        const balance = total - paid;
        const debt = Math.max(balance, 0).toFixed(2);
        const change = Math.max(-balance, 0).toFixed(2);
        const isCancelled = o.status === "CANCELLED";
        const statusText = isCancelled
          ? "Ləğv edilib"
          : getStatusText(o.paymentStatus);
        const statusClass = isCancelled
          ? "secondary"
          : getStatusClass(o.paymentStatus);
        const balanceText =
          change !== "0.00" ? `Üstü: ${change} ₼` : `${debt} ₼`;

        return `
                <tr>
                    <td>#${o.id}</td>
                    <td>${o.customer?.fullName || "Anonim"}</td>
                    <td>${o.employee?.fullName || "—"}</td>
                    <td>${o.totalAmount} ₼</td>
                    <td>${o.paidAmount || 0} ₼</td>
                    <td>${balanceText}</td>
                    <td><span class="badge badge-${statusClass}">${statusText}</span></td>
                    <td>${formatDate(o.orderedAt)}</td>
                    <td>
                        <button class="btn-text" onclick="deleteOrder(${o.id})" style="color:#ef4444">Sil</button>
                    </td>
                </tr>
            `;
      })
      .join("");
  } catch (e) {
    showToast(e.message || "Sifarişlər yüklənmədi", "error");
  }
}

async function deleteOrder(orderId) {
  if (
    !confirm(
      "Bu sifarişi silmək istədiyinizə əminsiniz? Stok geri qaytarılacaq.",
    )
  )
    return;

  try {
    const res = await fetch(`${API}/api/orders/${orderId}`, {
      method: "DELETE",
    });
    if (!res.ok) {
      throw new Error(await readApiError(res, "Sifariş silinmədi"));
    }

    showToast("Sifariş silindi və stok geri qaytarıldı");
    loadOrders();
    loadDashboard();
    loadProducts();
    loadPOS();
    loadDebts();
  } catch (e) {
    showToast(e.message || "Sifariş silinmədi", "error");
  }
}

// ═══════════════════════════════════════════════════════════
// CUSTOMERS
// ═══════════════════════════════════════════════════════════

async function loadCustomers() {
  try {
    const customers = await fetchJson(
      `${API}/api/customers`,
      {},
      "Müştərilər yüklənmədi",
    );
    const tbody = document.getElementById("customers-table");

    if (!customers || !customers.length) {
      tbody.innerHTML =
        '<tr><td colspan="4" style="text-align:center;padding:32px;color:#94a3b8;">Müştəri yoxdur</td></tr>';
      return;
    }

    tbody.innerHTML = customers
      .map(
        (c) => `
            <tr>
                <td>#${c.id}</td>
                <td><strong>${c.fullName}</strong></td>
                <td>${c.phone || "—"}</td>
                <td>${formatDate(c.registeredAt)}</td>
            </tr>
        `,
      )
      .join("");
  } catch (e) {
    showToast(e.message || "Müştərilər yüklənmədi", "error");
  }
}

function openCustomerModal() {
  document.getElementById("customer-modal").classList.add("active");
}

async function saveCustomer() {
  const customer = {
    fullName: document.getElementById("c-name").value.trim(),
    phone: document.getElementById("c-phone").value.trim(),
  };

  if (!customer.fullName || !customer.phone) {
    showToast("Ad və telefon məcburidir!", "error");
    return;
  }

  try {
    const res = await fetch(`${API}/api/customers`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(customer),
    });

    if (res.ok) {
      showToast("Müştəri əlavə edildi!");
      closeModal("customer-modal");
      loadCustomers();
      document.getElementById("c-name").value = "";
      document.getElementById("c-phone").value = "";
    } else {
      const message = await readApiError(res, "Müştəri əlavə edilmədi");
      showToast(message, "error");
    }
  } catch (e) {
    showToast("Əlaqə xətası!", "error");
  }
}

// ═══════════════════════════════════════════════════════════
// EMPLOYEES
// ═══════════════════════════════════════════════════════════

async function loadEmployees() {
  try {
    const employees = await fetchJson(
      `${API}/api/employees`,
      {},
      "İşçilər yüklənmədi",
    );
    const tbody = document.getElementById("employees-table");

    if (!employees || !employees.length) {
      tbody.innerHTML =
        '<tr><td colspan="7" style="text-align:center;padding:32px;color:#94a3b8;">İşçi yoxdur</td></tr>';
      return;
    }

    tbody.innerHTML = employees
      .map(
        (e) => `
            <tr>
                <td>#${e.id}</td>
                <td><strong>${e.fullName}</strong></td>
                <td>${e.phone || "—"}</td>
                <td>${e.baseSalary} ₼</td>
                <td>${formatDate(e.hiredAt)}</td>
                <td><span class="badge badge-${e.active ? "success" : "danger"}">${e.active ? "Aktiv" : "Deaktiv"}</span></td>
                <td>
                    ${e.active ? `<button class="btn-text" style="color:#ef4444;" onclick="deactivateEmployee(${e.id})">Deaktiv et</button>` : "—"}
                </td>
            </tr>
        `,
      )
      .join("");
  } catch (e) {
    showToast(e.message || "İşçilər yüklənmədi", "error");
  }
}

function openEmployeeModal() {
  document.getElementById("employee-modal").classList.add("active");
}

async function saveEmployee() {
  const employee = {
    fullName: document.getElementById("emp-name").value.trim(),
    phone: document.getElementById("emp-phone").value.trim() || null,
    baseSalary: parseFloat(document.getElementById("emp-salary").value),
    hiredAt:
      document.getElementById("emp-hired").value ||
      new Date().toISOString().split("T")[0],
  };

  if (!employee.fullName || !employee.baseSalary) {
    showToast("Ad və maaş məcburidir!", "error");
    return;
  }

  try {
    const res = await fetch(`${API}/api/employees`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(employee),
    });

    if (res.ok) {
      showToast("İşçi əlavə edildi!");
      closeModal("employee-modal");
      loadEmployees();
      document.getElementById("emp-name").value = "";
      document.getElementById("emp-phone").value = "";
      document.getElementById("emp-salary").value = "";
      document.getElementById("emp-hired").value = "";
    } else {
      showToast("Xəta baş verdi!", "error");
    }
  } catch (e) {
    showToast("Əlaqə xətası!", "error");
  }
}

async function deactivateEmployee(id) {
  if (!confirm("Bu işçini deaktiv etmək istəyirsiniz?")) return;

  try {
    const res = await fetch(`${API}/api/employees/${id}/deactivate`, {
      method: "PATCH",
    });

    if (res.ok) {
      showToast("İşçi deaktiv edildi!");
      loadEmployees();
    } else {
      showToast("Xəta baş verdi!", "error");
    }
  } catch (e) {
    showToast("Əlaqə xətası!", "error");
  }
}

// ═══════════════════════════════════════════════════════════
// DEBTS
// ═══════════════════════════════════════════════════════════

async function loadDebts() {
  try {
    const [debts, partials] = await Promise.all([
      fetchJson(`${API}/api/orders/debts`, {}, "Borclar yüklənmədi"),
      fetchJson(
        `${API}/api/orders/partials`,
        {},
        "Qismən ödənişlər yüklənmədi",
      ),
    ]);
    const all = [...debts, ...partials];
    const tbody = document.getElementById("debts-table");

    if (!all.length) {
      tbody.innerHTML =
        '<tr><td colspan="7" style="text-align:center;padding:32px;color:#94a3b8;">Borc yoxdur</td></tr>';
      return;
    }

    tbody.innerHTML = all
      .map((o) => {
        const debt = Math.max(
          parseFloat(o.totalAmount || 0) - parseFloat(o.paidAmount || 0),
          0,
        ).toFixed(2);
        return `
                <tr>
                    <td>#${o.id}</td>
                    <td><strong>${o.customer?.fullName || "Anonim"}</strong></td>
                    <td>${o.totalAmount} ₼</td>
                    <td>${o.paidAmount || 0} ₼</td>
                    <td><strong style="color:#ef4444">${debt} ₼</strong></td>
                    <td>${formatDate(o.orderedAt)}</td>
                    <td><button class="btn-primary" style="padding:6px 12px;font-size:13px" onclick="payDebt(${o.id}, ${debt})">Ödə</button></td>
                </tr>
            `;
      })
      .join("");
  } catch (e) {
    showToast(e.message || "Borc məlumatı yüklənmədi", "error");
  }
}

async function payDebt(orderId, debt) {
  const amount = prompt(`Ödəniləcək məbləğ (Borc: ${debt} ₼):`, debt);
  if (!amount || parseFloat(amount) <= 0) return;

  try {
    const res = await fetch(
      `${API}/api/orders/${orderId}/pay?amount=${encodeURIComponent(amount)}`,
      { method: "PATCH" },
    );
    if (res.ok) {
      showToast("Ödəniş edildi!");
      loadDebts();
      loadDashboard();
    } else {
      showToast(await readApiError(res, "Ödəniş alınmadı"), "error");
    }
  } catch (e) {
    showToast("Əlaqə xətası!", "error");
  }
}

// ═══════════════════════════════════════════════════════════
// EXCEL EXPORT
// ═══════════════════════════════════════════════════════════

async function exportMonthly() {
  const year = prompt("İl:", new Date().getFullYear());
  const month = prompt("Ay (1-12):", new Date().getMonth() + 1);

  if (!year || !month) return;

  try {
    showToast("Excel hazırlanır...");
    const response = await fetch(
      `${API}/api/export/monthly?year=${year}&month=${month}`,
    );

    if (response.ok && isDownloadResponse(response)) {
      await saveResponseAsFile(response, `hesabat-${year}-${month}.xlsx`);
      showToast("Excel yükləndi! ✓");
      return;
    }

    const details = response.ok
      ? "Yüklənən fayl formatı düzgün deyil"
      : await readApiError(response, "Export xətası");

    const fallbackData = await fetchJson(
      `${API}/api/export/monthly-data?year=${year}&month=${month}`,
      {},
      "Aylıq fallback məlumatı alınmadı",
    );
    downloadCsv(
      ["Göstərici", "Məbləğ (AZN)"],
      [
        ["Satış gəliri", safeNum(fallbackData?.revenue)],
        ["Mal xərci", safeNum(fallbackData?.costOfGoods)],
        ["Brut mənfəət", safeNum(fallbackData?.grossProfit)],
        ["Əməliyyat xərcləri", safeNum(fallbackData?.totalExpense)],
        ["Xalis mənfəət", safeNum(fallbackData?.netProfit)],
      ],
      `hesabat-${year}-${month}.csv`,
    );
    showToast(`Backend xətası (${details}) - CSV fallback yükləndi`, "warning");
  } catch (e) {
    showToast("Xəta: " + e.message, "error");
  }
}

async function exportBonus() {
  const year = prompt("İl:", new Date().getFullYear());
  const quarter = prompt("Rüb (1-4):", 1);

  if (!year || !quarter) return;

  try {
    showToast("Excel hazırlanır...");
    const response = await fetch(
      `${API}/api/export/bonus?year=${year}&quarter=${quarter}`,
    );

    if (response.ok && isDownloadResponse(response)) {
      await saveResponseAsFile(response, `bonus-${year}-Q${quarter}.xlsx`);
      showToast("Excel yükləndi! ✓");
      return;
    }

    const details = response.ok
      ? "Yüklənən fayl formatı düzgün deyil"
      : await readApiError(response, "Export xətası");

    const fallbackData = await fetchJson(
      `${API}/api/bonus/quarterly?year=${year}&quarter=${quarter}`,
      {},
      "Bonus fallback məlumatı alınmadı",
    );

    downloadCsv(
      ["İşçi", "Satış (AZN)", "Bonus %", "Bonus (AZN)"],
      (fallbackData || []).map((b) => [
        b?.employeeName || "—",
        safeNum(b?.totalSales),
        safeNum(b?.bonusPercent),
        safeNum(b?.bonusAmount),
      ]),
      `bonus-${year}-Q${quarter}.csv`,
    );
    showToast(`Backend xətası (${details}) - CSV fallback yükləndi`, "warning");
  } catch (e) {
    showToast("Xəta: " + e.message, "error");
  }
}

async function exportProducts() {
  try {
    showToast("Excel hazırlanır...");
    const response = await fetch(`${API}/api/export/products`);

    if (response.ok && isDownloadResponse(response)) {
      await saveResponseAsFile(response, "mehsullar.xlsx");
      showToast("Excel yükləndi! ✓");
      return;
    }

    const details = response.ok
      ? "Yüklənən fayl formatı düzgün deyil"
      : await readApiError(response, "Export xətası");

    const fallbackRows = (products || []).map((p) => [
      p?.id ?? "",
      p?.barcode || "",
      p?.name || "",
      safeNum(p?.price),
      safeNum(p?.costPrice),
      p?.stockQty ?? 0,
      p?.category || "",
      p?.status || "",
    ]);

    downloadCsv(
      ["ID", "Barcode", "Ad", "Satış qiyməti", "Alış qiyməti", "Stok", "Kateqoriya", "Status"],
      fallbackRows,
      "mehsullar.csv",
    );
    showToast(`Backend xətası (${details}) - CSV fallback yükləndi`, "warning");
  } catch (e) {
    showToast("Xəta: " + e.message, "error");
  }
}

function isDownloadResponse(response) {
  const ct = (response.headers.get("content-type") || "").toLowerCase();
  return (
    ct.includes("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet") ||
    ct.includes("application/octet-stream")
  );
}

async function saveResponseAsFile(response, fileName) {
  const blob = await response.blob();
  const downloadUrl = window.URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = downloadUrl;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(downloadUrl);
}

function downloadCsv(header, rows, fileName) {
  const allRows = [header, ...(rows || [])];
  const csv = allRows
    .map((r) => r.map((cell) => `"${sanitizeCsv(cell)}"`).join(","))
    .join("\n");

  const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = fileName;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

function sanitizeCsv(value) {
  return String(value ?? "")
    .replaceAll('"', '""')
    .replaceAll("\n", " ")
    .replaceAll("\r", " ")
    .trim();
}

function safeNum(value) {
  return Number(value || 0).toFixed(2);
}

// ═══════════════════════════════════════════════════════════
// POS BARCODE SCANNER
// ═══════════════════════════════════════════════════════════

function openScanner() {
  document.getElementById("scanner-modal").classList.add("active");

  startBarcodeScanner("pos-scanner-container", "pos", (decodedText) => {
    document.getElementById("scanner-result").textContent = "✓ " + decodedText;

    // Məhsulu tap və səbətə at
    const product = products.find((p) => p.barcode === decodedText);
    if (product) {
      addToCart(product.id);
    } else {
      showToast("Məhsul tapılmadı!", "error");
    }

    setTimeout(() => closeScanner(), 900);
  });
}

function closeScanner() {
  if (posScannerControls) {
    posScannerControls.stop();
    posScannerControls = null;
  }
  document.getElementById("scanner-modal").classList.remove("active");
  document.getElementById("scanner-result").textContent = "";
  const container = document.getElementById("pos-scanner-container");
  if (container) container.innerHTML = "";
}

function getBarcodeReader() {
  if (!barcodeLib) {
    throw new Error("Barkod kitabxanası hazır deyil");
  }

  if (!barcodeReader) {
    const hints = new Map();
    hints.set(barcodeLib.DecodeHintType.POSSIBLE_FORMATS, [
      barcodeLib.BarcodeFormat.EAN_13,
      barcodeLib.BarcodeFormat.UPC_A,
      barcodeLib.BarcodeFormat.CODE_128,
    ]);
    barcodeReader = new barcodeLib.BrowserMultiFormatReader(hints, 250);
  }

  return barcodeReader;
}

function loadScript(src) {
  return new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[data-src="${src}"]`);
    if (existing) {
      if (existing.dataset.loaded === "true") resolve();
      else existing.addEventListener("load", () => resolve(), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    script.dataset.src = src;
    script.onload = () => {
      script.dataset.loaded = "true";
      resolve();
    };
    script.onerror = () => reject(new Error(`Script yüklənmədi: ${src}`));
    document.head.appendChild(script);
  });
}

async function ensureBarcodeLibrary() {
  if (window.ZXingBrowser?.BrowserMultiFormatReader) {
    barcodeLib = window.ZXingBrowser;
    return;
  }

  if (window.ZXing?.BrowserMultiFormatReader) {
    barcodeLib = window.ZXing;
    return;
  }

  if (!barcodeLibLoadingPromise) {
    barcodeLibLoadingPromise = (async () => {
      const sources = [
        "https://cdn.jsdelivr.net/npm/@zxing/browser@0.1.5/umd/index.min.js",
        "https://unpkg.com/@zxing/browser@0.1.5/umd/index.min.js",
      ];

      for (const src of sources) {
        try {
          await loadScript(src);
          if (window.ZXingBrowser?.BrowserMultiFormatReader) {
            barcodeLib = window.ZXingBrowser;
            return;
          }
          if (window.ZXing?.BrowserMultiFormatReader) {
            barcodeLib = window.ZXing;
            return;
          }
        } catch (_) {}
      }

      throw new Error("Barkod kitabxanası yüklənmədi");
    })();
  }

  await barcodeLibLoadingPromise;
}

async function startNativeBarcodeScanner(containerId, mode, onDetected) {
  if (!navigator.mediaDevices?.getUserMedia) {
    throw new Error("Brauzerdə kamera API dəstəklənmir");
  }

  if (!window.BarcodeDetector) {
    throw new Error("Brauzerdə barkod detektoru dəstəklənmir");
  }

  const supportedFormats =
    typeof window.BarcodeDetector.getSupportedFormats === "function"
      ? await window.BarcodeDetector.getSupportedFormats()
      : [];

  const preferredFormats = ["ean_13", "upc_a", "code_128"];
  const formats = supportedFormats.length
    ? preferredFormats.filter((f) => supportedFormats.includes(f))
    : preferredFormats;

  const detector = formats.length
    ? new window.BarcodeDetector({ formats })
    : new window.BarcodeDetector();

  const videoEl = ensureScannerVideo(containerId);
  const stream = await navigator.mediaDevices.getUserMedia({
    video: {
      facingMode: { ideal: "environment" },
    },
    audio: false,
  });

  videoEl.srcObject = stream;
  await videoEl.play();

  let stopped = false;
  let frameRequestId = null;

  const stop = () => {
    if (stopped) return;
    stopped = true;
    if (frameRequestId != null) {
      cancelAnimationFrame(frameRequestId);
      frameRequestId = null;
    }
    stream.getTracks().forEach((track) => track.stop());
    if (videoEl.srcObject) {
      videoEl.srcObject = null;
    }
  };

  const scan = async () => {
    if (stopped) return;
    try {
      const codes = await detector.detect(videoEl);
      if (codes.length) {
        const value = codes[0].rawValue;
        if (value) {
          stop();
          if (mode === "pos") {
            posScannerControls = null;
          } else {
            productScannerControls = null;
          }
          onDetected(value);
          return;
        }
      }
    } catch (_) {}

    frameRequestId = requestAnimationFrame(scan);
  };

  frameRequestId = requestAnimationFrame(scan);
  return { stop };
}

function ensureScannerVideo(containerId) {
  const container = document.getElementById(containerId);
  if (!container) {
    throw new Error("Skan konteyneri tapılmadı");
  }

  container.innerHTML = "";

  const video = document.createElement("video");
  video.setAttribute("autoplay", "true");
  video.setAttribute("playsinline", "true");
  video.setAttribute("muted", "true");
  video.style.width = "100%";
  video.style.borderRadius = "12px";

  container.appendChild(video);
  return video;
}

async function startBarcodeScanner(containerId, mode, onDetected) {
  try {
    await ensureBarcodeLibrary();
    const reader = getBarcodeReader();
    const videoEl = ensureScannerVideo(containerId);

    if (mode === "pos" && posScannerControls) {
      posScannerControls.stop();
    }
    if (mode === "product" && productScannerControls) {
      productScannerControls.stop();
    }

    const controls = await reader.decodeFromVideoDevice(
      undefined,
      videoEl,
      (result) => {
        if (!result) return;

        controls.stop();
        if (mode === "pos") {
          posScannerControls = null;
        } else {
          productScannerControls = null;
        }

        onDetected(result.getText());
      },
    );

    if (mode === "pos") {
      posScannerControls = controls;
    } else {
      productScannerControls = controls;
    }
  } catch (e) {
    try {
      const fallbackControls = await startNativeBarcodeScanner(
        containerId,
        mode,
        onDetected,
      );
      if (mode === "pos") {
        posScannerControls = fallbackControls;
      } else {
        productScannerControls = fallbackControls;
      }
      showToast("Kamera fallback rejimində açıldı", "warning");
    } catch (fallbackError) {
      showToast(
        "Kamera açılmadı: " +
          (fallbackError.message || e.message || "naməlum xəta"),
        "error",
      );
    }
  }
}

// ═══════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════

async function apiRequest(path, options = {}, fallback = "Sorğu uğursuz oldu") {
  try {
    const res = await fetch(`${API}${path}`, {
      headers: {
        "Content-Type": "application/json",
        ...(options.headers || {}),
      },
      ...options,
    });

    if (!res.ok) {
      throw new Error(await readApiError(res, fallback));
    }

    if (res.status === 204) return null;
    const ct = res.headers.get("content-type") || "";
    return ct.includes("application/json") ? await res.json() : null;
  } catch (e) {
    showToast(e.message || "Naməlum xəta", "error");
    throw e;
  }
}
function closeModal(id) {
  if (id === "product-modal") {
    resetProductModal();
    stopProductScanner();
  }
  document.getElementById(id).classList.remove("active");
}

// ═══════════════════════════════════════════════════════════
// RETURN / CANCEL ORDER
// ═══════════════════════════════════════════════════════════

let returnOrderId = null;

function openReturnModal(orderId) {
  returnOrderId = orderId;
  document.getElementById("return-modal").classList.add("active");
}

async function confirmReturn() {
  if (!returnOrderId) return;

  try {
    const res = await fetch(`${API}/api/orders/${returnOrderId}/cancel`, {
      method: "PATCH",
    });

    if (res.ok) {
      showToast("Sifariş ləğv edildi!");
      closeModal("return-modal");
      returnOrderId = null;
      loadOrders();
      loadDashboard();
      loadDebts();
      loadPOS();
      loadProducts();
    } else {
      showToast(await readApiError(res, "Sifariş ləğv edilmədi"), "error");
    }
  } catch (e) {
    showToast("Əlaqə xətası!", "error");
  }
}

// ═══════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════

function showToast(message, type = "success") {
  const toast = document.getElementById("toast");
  if (!toast) return;
  toast.textContent = message;
  toast.className = `toast ${type} active`;
  setTimeout(() => toast.classList.remove("active"), 3000);
}

function formatNumber(num) {
  return new Intl.NumberFormat("az-AZ", {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  }).format(num);
}

function formatDate(dateString) {
  if (!dateString) return "—";
  return new Date(dateString).toLocaleDateString("az-AZ");
}

function getStatusClass(status) {
  const map = { PAID: "success", DEBT: "danger", PARTIAL: "warning" };
  return map[status] || "secondary";
}

function getStatusText(status) {
  const map = { PAID: "Ödənildi", DEBT: "Borclu", PARTIAL: "Qismən" };
  return map[status] || status;
}

document.addEventListener("click", (e) => {
  if (e.target.classList.contains("modal")) {
    const modalId = e.target.id;
    if (modalId === "product-modal") {
      stopProductScanner();
    }
    e.target.classList.remove("active");
  }
  // ...existing code...

  /* ===== HOTFIX: Products stock +/- və Orders delete (UI auto-attach) ===== */
  (function () {
    function toast(msg, type = "info") {
      if (typeof showToast === "function") return showToast(msg, type);
      alert(msg);
    }

    async function apiCall(
      path,
      options = {},
      fallback = "Sorğu uğursuz oldu",
    ) {
      try {
        const res = await fetch(`${API}${path}`, {
          headers: {
            "Content-Type": "application/json",
            ...(options.headers || {}),
          },
          ...options,
        });
        if (!res.ok) {
          const err =
            typeof readApiError === "function"
              ? await readApiError(res, fallback)
              : fallback;
          throw new Error(err || fallback);
        }
        if (res.status === 204) return null;
        const ct = res.headers.get("content-type") || "";
        return ct.includes("application/json") ? await res.json() : null;
      } catch (e) {
        toast(e.message || "Xəta baş verdi", "error");
        throw e;
      }
    }

    function parseRowId(tr) {
      const firstTd = tr?.querySelector("td");
      if (!firstTd) return null;
      const raw = (firstTd.textContent || "").trim().replace("#", "");
      const id = Number(raw);
      return Number.isFinite(id) ? id : null;
    }

    function ensureHeaderAction(pageId, title) {
      const page = document.getElementById(pageId);
      if (!page) return;
      const table = page.querySelector("table");
      if (!table) return;
      const headRow = table.querySelector("thead tr");
      if (!headRow) return;
      const lastTh = headRow.lastElementChild;
      if (lastTh && (lastTh.textContent || "").trim() === title) return;
      const th = document.createElement("th");
      th.textContent = title;
      headRow.appendChild(th);
    }

    function enhanceProductsTable() {
      const page = document.getElementById("products");
      if (!page) return;

      ensureHeaderAction("products", "Stok İdarəsi");

      const rows = page.querySelectorAll("tbody tr");
      rows.forEach((tr) => {
        if (tr.dataset.stockEnhanced === "1") return;
        const id = parseRowId(tr);
        if (!id) return;

        const td = document.createElement("td");
        td.innerHTML = `
        <button class="btn-secondary stock-btn" data-stock-id="${id}" data-delta="1">+1</button>
        <button class="btn-secondary stock-btn" data-stock-id="${id}" data-delta="-1">-1</button>
      `;
        tr.appendChild(td);
        tr.dataset.stockEnhanced = "1";
      });
    }

    async function onStockClick(e) {
      const btn = e.target.closest(".stock-btn");
      if (!btn) return;

      const id = Number(btn.dataset.stockId);
      const delta = Number(btn.dataset.delta);
      if (!id || !delta) return;

      btn.disabled = true;
      try {
        await apiCall(
          `/api/products/${id}/stock?quantity=${delta}`,
          { method: "PATCH" },
          "Stok yenilənmədi",
        );
        toast("Stok yeniləndi", "success");
        if (typeof loadProducts === "function") await loadProducts();
        if (typeof loadPOS === "function") await loadPOS();
        if (typeof loadDashboard === "function") await loadDashboard();
        setTimeout(enhanceProductsTable, 80);
      } finally {
        btn.disabled = false;
      }
    }

    function enhanceOrdersTable() {
      const page = document.getElementById("orders");
      if (!page) return;

      ensureHeaderAction("orders", "Əməliyyat");

      const rows = page.querySelectorAll("tbody tr");
      rows.forEach((tr) => {
        if (tr.dataset.deleteEnhanced === "1") return;
        const id = parseRowId(tr);
        if (!id) return;

        const td = document.createElement("td");
        td.innerHTML = `<button class="btn-danger order-delete-btn" data-order-id="${id}">Sil</button>`;
        tr.appendChild(td);
        tr.dataset.deleteEnhanced = "1";
      });
    }

    async function onDeleteClick(e) {
      const btn = e.target.closest(".order-delete-btn");
      if (!btn) return;

      const id = Number(btn.dataset.orderId);
      if (!id) return;
      if (!confirm("Bu sifarişi silmək istədiyinizə əminsiniz?")) return;

      btn.disabled = true;
      try {
        await apiCall(
          `/api/orders/${id}`,
          { method: "DELETE" },
          "Sifariş silinmədi",
        );
        toast("Sifariş silindi, stok geri qaytarıldı", "success");
        if (typeof loadOrders === "function") await loadOrders();
        if (typeof loadProducts === "function") await loadProducts();
        if (typeof loadDashboard === "function") await loadDashboard();
        setTimeout(enhanceOrdersTable, 80);
      } finally {
        btn.disabled = false;
      }
    }

    function wireDelegation() {
      document.addEventListener("click", onStockClick);
      document.addEventListener("click", onDeleteClick);
    }

    function hookLoaders() {
      if (typeof loadProducts === "function" && !loadProducts.__patched) {
        const oldLoadProducts = loadProducts;
        window.loadProducts = async function (...args) {
          const r = await oldLoadProducts.apply(this, args);
          setTimeout(enhanceProductsTable, 60);
          return r;
        };
        window.loadProducts.__patched = true;
      }

      if (typeof loadOrders === "function" && !loadOrders.__patched) {
        const oldLoadOrders = loadOrders;
        window.loadOrders = async function (...args) {
          const r = await oldLoadOrders.apply(this, args);
          setTimeout(enhanceOrdersTable, 60);
          return r;
        };
        window.loadOrders.__patched = true;
      }
    }

    function bootEnhancements() {
      hookLoaders();
      wireDelegation();
      setTimeout(() => {
        enhanceProductsTable();
        enhanceOrdersTable();
      }, 300);
    }

    if (document.readyState === "loading") {
      document.addEventListener("DOMContentLoaded", bootEnhancements);
    } else {
      bootEnhancements();
    }
  })();
});
