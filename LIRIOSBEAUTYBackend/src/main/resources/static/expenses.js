const API =
  window.__APP_CONFIG__?.apiBaseUrl ||
  (["localhost:5500", "127.0.0.1:5500"].includes(window.location.host)
    ? "http://localhost:8080"
    : "");
const PAGE_SIZE = 10;
const ACCESS_TOKEN_KEY = "auth_token";
const REFRESH_TOKEN_KEY = "refresh_token";

let expenses = [];
let filteredExpenses = [];
let currentPage = 1;

const CATEGORY_LABELS = {
  COURIER: "İcarə/Logistika",
  MARKETING: "Kommunal/Reklam",
  SALARY: "Maaş",
  PACKAGING: "Alış/Paket",
  OTHER: "Digər",
};

document.addEventListener("DOMContentLoaded", () => {
  if (!localStorage.getItem(ACCESS_TOKEN_KEY)) {
    window.location.href = "index.html";
    return;
  }
  bindEvents();
  loadCurrency();
  loadExpenses();
});

function bindEvents() {
  document
    .getElementById("btn-new-expense")
    .addEventListener("click", () => openModal("expense-modal"));

  document
    .getElementById("close-expense-modal")
    .addEventListener("click", () => closeModal("expense-modal"));

  document
    .getElementById("save-expense")
    .addEventListener("click", createExpense);

  document
    .getElementById("btn-export-expenses")
    .addEventListener("click", exportExpensesToCsv);

  document.getElementById("expense-search").addEventListener("input", () => {
    currentPage = 1;
    applyFilters();
  });

  document
    .getElementById("expense-date-filter")
    .addEventListener("change", () => {
      currentPage = 1;
      applyFilters();
    });

  document.addEventListener("click", (e) => {
    if (e.target.classList.contains("modal")) {
      e.target.classList.remove("active");
    }

    if (e.target.matches("[data-page]")) {
      const page = Number(e.target.getAttribute("data-page"));
      if (!Number.isNaN(page)) {
        currentPage = page;
        renderTable();
      }
    }

    if (e.target.matches("[data-delete-id]")) {
      const id = Number(e.target.getAttribute("data-delete-id"));
      if (!Number.isNaN(id)) deleteExpense(id);
    }
  });
}

async function loadCurrency() {
  try {
    const res = await fetch("https://api.exchangerate-api.com/v4/latest/AZN");
    const data = await res.json();
    const usd = (1 / data.rates.USD).toFixed(2);
    const eur = (1 / data.rates.EUR).toFixed(2);
    document.getElementById("usd-rate").textContent = usd;
    document.getElementById("eur-rate").textContent = eur;
  } catch (_) {}
}

async function loadExpenses() {
  try {
    const res = await authFetch(`${API}/api/expenses`);
    if (!res.ok) throw new Error("Xərclər yüklənmədi");

    expenses = await res.json();
    expenses.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));

    applyFilters();
    renderStats();
  } catch (e) {
    showToast(e.message || "Xəta baş verdi", "error");
  }
}

function applyFilters() {
  const search = document
    .getElementById("expense-search")
    .value.trim()
    .toLowerCase();
  const dateFilter = document.getElementById("expense-date-filter").value;

  filteredExpenses = expenses.filter((item) => {
    const categoryText = getCategoryLabel(item.category).toLowerCase();
    const noteText = (item.notes || "").toLowerCase();
    const descText = (item.description || "").toLowerCase();

    const matchesSearch =
      !search ||
      categoryText.includes(search) ||
      noteText.includes(search) ||
      descText.includes(search);

    const matchesDate = matchDateFilter(item.createdAt, dateFilter);
    return matchesSearch && matchesDate;
  });

  const totalPages = Math.max(
    1,
    Math.ceil(filteredExpenses.length / PAGE_SIZE),
  );
  if (currentPage > totalPages) currentPage = totalPages;

  renderTable();
  renderPagination();
}

function matchDateFilter(dateValue, filter) {
  if (filter === "all") return true;

  const now = new Date();
  const d = new Date(dateValue);

  if (filter === "today") {
    return (
      d.getFullYear() === now.getFullYear() &&
      d.getMonth() === now.getMonth() &&
      d.getDate() === now.getDate()
    );
  }

  if (filter === "week") {
    const start = new Date(now);
    const day = start.getDay() || 7;
    start.setDate(now.getDate() - day + 1);
    start.setHours(0, 0, 0, 0);

    const end = new Date(start);
    end.setDate(start.getDate() + 6);
    end.setHours(23, 59, 59, 999);

    return d >= start && d <= end;
  }

  if (filter === "month") {
    return (
      d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth()
    );
  }

  return true;
}

function renderStats() {
  const now = new Date();

  const monthTotal = expenses
    .filter((e) => {
      const d = new Date(e.createdAt);
      return (
        d.getFullYear() === now.getFullYear() && d.getMonth() === now.getMonth()
      );
    })
    .reduce((sum, e) => sum + Number(e.amount || 0), 0);

  const allTotal = expenses.reduce((sum, e) => sum + Number(e.amount || 0), 0);

  const categorySums = {};
  expenses.forEach((e) => {
    const key = e.category || "OTHER";
    categorySums[key] = (categorySums[key] || 0) + Number(e.amount || 0);
  });

  let topCategory = "—";
  let topValue = -1;
  Object.keys(categorySums).forEach((key) => {
    if (categorySums[key] > topValue) {
      topValue = categorySums[key];
      topCategory = getCategoryLabel(key);
    }
  });

  document.getElementById("expense-monthly-total").textContent =
    `${formatNumber(monthTotal)} ₼`;
  document.getElementById("expense-all-total").textContent =
    `${formatNumber(allTotal)} ₼`;
  document.getElementById("expense-top-category").textContent = topCategory;
}

function renderTable() {
  const body = document.getElementById("expenses-table-body");
  if (!filteredExpenses.length) {
    body.innerHTML =
      '<tr><td colspan="6" style="text-align:center;padding:32px;color:#94a3b8;">Məlumat yoxdur</td></tr>';
    return;
  }

  const start = (currentPage - 1) * PAGE_SIZE;
  const pageItems = filteredExpenses.slice(start, start + PAGE_SIZE);

  body.innerHTML = pageItems
    .map(
      (e) => `
      <tr>
        <td>#${e.id}</td>
        <td>${formatDateTime(e.createdAt)}</td>
        <td>${getCategoryLabel(e.category)}</td>
        <td><strong>${formatNumber(e.amount)} ₼</strong></td>
        <td>${escapeHtml(e.notes || e.description || "—")}</td>
        <td>
          <button class="btn-danger" data-delete-id="${e.id}">Sil</button>
        </td>
      </tr>
    `,
    )
    .join("");
}

function renderPagination() {
  const container = document.getElementById("expenses-pagination");
  const totalPages = Math.max(
    1,
    Math.ceil(filteredExpenses.length / PAGE_SIZE),
  );

  if (totalPages <= 1) {
    container.innerHTML = "";
    return;
  }

  let html = "";
  for (let i = 1; i <= totalPages; i++) {
    const active = i === currentPage ? "active" : "";
    html += `<button class="page-btn ${active}" data-page="${i}">${i}</button>`;
  }

  container.innerHTML = html;
}

async function createExpense() {
  const category = document.getElementById("expense-category").value;
  const amount = Number(document.getElementById("expense-amount").value);
  const description = document
    .getElementById("expense-description")
    .value.trim();
  const notes = document.getElementById("expense-notes").value.trim();

  if (!category || !amount || amount <= 0 || !description) {
    showToast("Kateqoriya, məbləğ və təsvir mütləqdir", "error");
    return;
  }

  try {
    const res = await authFetch(`${API}/api/expenses`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        category,
        amount,
        description,
        notes,
      }),
    });

    if (!res.ok)
      throw new Error(await readApiError(res, "Xərc əlavə olunmadı"));

    showToast("Xərc əlavə edildi");
    closeModal("expense-modal");
    resetExpenseForm();
    await loadExpenses();
  } catch (e) {
    showToast(e.message || "Xəta baş verdi", "error");
  }
}

async function deleteExpense(id) {
  if (!confirm("Bu xərci silmək istədiyinizə əminsiniz?")) return;

  try {
    const res = await authFetch(`${API}/api/expenses/${id}`, {
      method: "DELETE",
    });

    if (!res.ok) throw new Error(await readApiError(res, "Xərc silinmədi"));

    showToast("Xərc silindi");
    await loadExpenses();
  } catch (e) {
    showToast(e.message || "Xəta baş verdi", "error");
  }
}

function exportExpensesToCsv() {
  if (!filteredExpenses.length) {
    showToast("Export üçün məlumat yoxdur", "warning");
    return;
  }

  const header = ["ID", "Tarix", "Kateqoriya", "Məbləğ", "Təsvir", "Qeyd"];
  const rows = filteredExpenses.map((e) => [
    e.id,
    formatDateTime(e.createdAt),
    getCategoryLabel(e.category),
    e.amount,
    sanitizeCsv(e.description || ""),
    sanitizeCsv(e.notes || ""),
  ]);

  const csv = [header, ...rows]
    .map((r) =>
      r.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(","),
    )
    .join("\n");

  const blob = new Blob(["\uFEFF" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `expenses-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);

  showToast("Excel export hazırdır");
}

function resetExpenseForm() {
  document.getElementById("expense-category").value = "";
  document.getElementById("expense-amount").value = "";
  document.getElementById("expense-description").value = "";
  document.getElementById("expense-notes").value = "";
}

function getCategoryLabel(category) {
  return CATEGORY_LABELS[category] || category || "Digər";
}

function openModal(id) {
  document.getElementById(id)?.classList.add("active");
}

function closeModal(id) {
  document.getElementById(id)?.classList.remove("active");
}

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
  }).format(Number(num || 0));
}

function formatDateTime(value) {
  if (!value) return "—";
  const d = new Date(value);
  return d.toLocaleString("az-AZ", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function sanitizeCsv(value) {
  return String(value).replaceAll("\n", " ").trim();
}

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

async function authFetch(url, options = {}) {
  const headers = new Headers(options.headers || {});
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const res = await fetch(url, { ...options, headers });
  if (res.status === 401 || res.status === 403) {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem("user_info");
    window.location.href = "index.html";
  }
  return res;
}
