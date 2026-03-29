// ═══════════════════════════════════════════════════════════
// LIRIOS BEAUTY CRM - FULL PRODUCTION VERSION v2
// Backend: http://localhost:8080
// ═══════════════════════════════════════════════════════════

const API = 'http://localhost:8080';

// Global State
let cart = [];
let products = [];
let employees = [];
let currentCustomer = null;
let currentUser = null;
let html5QrcodeScanner = null;
let productScanner = null;
let existingProduct = null; // Barkod yoxlayanda tapılan məhsul

// Currency rates
let usdRate = 1.7;
let eurRate = 1.85;

// ═══════════════════════════════════════════════════════════
// AUTHENTICATION
// ═══════════════════════════════════════════════════════════

document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
});

function checkAuth() {
    const user = localStorage.getItem('user_info');

    if (!user) {
        showLoginPage();
    } else {
        currentUser = JSON.parse(user);
        initApp();
    }
}

function showLoginPage() {
    document.body.innerHTML = `
        <div style="min-height:100vh;display:flex;align-items:center;justify-content:center;background:linear-gradient(135deg,#667eea 0%,#764ba2 100%)">
            <div style="background:white;padding:48px;border-radius:20px;box-shadow:0 20px 60px rgba(0,0,0,0.3);width:100%;max-width:400px">
                <div style="text-align:center;margin-bottom:32px">
                    <div style="width:60px;height:60px;background:linear-gradient(135deg,#667eea,#764ba2);border-radius:16px;display:inline-flex;align-items:center;justify-content:center;color:white;font-size:32px;font-weight:700;margin-bottom:16px">L</div>
                    <h2 style="font-size:28px;font-weight:700;margin:0 0 4px">Lirios Beauty</h2>
                    <p style="color:#64748b;margin:0">CRM Panel</p>
                </div>
                <form onsubmit="handleLogin(event)" style="display:flex;flex-direction:column;gap:16px">
                    <input type="text" id="login-username" placeholder="İstifadəçi adı" style="padding:12px 16px;border:2px solid #e2e8f0;border-radius:8px;font-size:15px" required>
                    <input type="password" id="login-password" placeholder="Şifrə" style="padding:12px 16px;border:2px solid #e2e8f0;border-radius:8px;font-size:15px" required>
                    <button type="submit" style="background:#2563eb;color:white;border:none;padding:14px;border-radius:8px;font-weight:600;cursor:pointer;font-size:16px">Daxil ol</button>
                </form>
                <div id="login-error" style="color:#ef4444;text-align:center;margin-top:16px;font-size:14px"></div>
            </div>
        </div>
    `;
}

async function handleLogin(e) {
    e.preventDefault();
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;

    // Mock login
    if (username && password) {
        const mockUser = {
            id: 1,
            fullName: username,
            role: 'ADMIN'
        };

        localStorage.setItem('user_info', JSON.stringify(mockUser));
        currentUser = mockUser;
        location.reload();
    } else {
        document.getElementById('login-error').textContent = 'Giriş uğursuz oldu!';
    }
}

function logout() {
    localStorage.removeItem('user_info');
    location.reload();
}

function initApp() {
    initNav();
    loadCurrency();
    loadDashboard();
    displayUserInfo();
}

function displayUserInfo() {
    const sidebar = document.querySelector('.sidebar-footer');
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
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const page = item.dataset.page;

            document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
            item.classList.add('active');
            document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
            document.getElementById(page).classList.add('active');

            const loaders = {
                'dashboard': loadDashboard,
                'pos': loadPOS,
                'products': loadProducts,
                'orders': loadOrders,
                'customers': loadCustomers,
                'employees': loadEmployees,
                'debts': loadDebts
            };

            if (loaders[page]) loaders[page]();
        });
    });
}

// ═══════════════════════════════════════════════════════════
// CURRENCY
// ═══════════════════════════════════════════════════════════

async function loadCurrency() {
    try {
        const res = await fetch('https://api.exchangerate-api.com/v4/latest/AZN');
        const data = await res.json();
        usdRate = (1 / data.rates.USD).toFixed(2);
        eurRate = (1 / data.rates.EUR).toFixed(2);
        document.getElementById('usd-rate').textContent = usdRate;
        document.getElementById('eur-rate').textContent = eurRate;
    } catch(e) {}
}

// ═══════════════════════════════════════════════════════════
// DASHBOARD
// ═══════════════════════════════════════════════════════════

async function loadDashboard() {
    try {
        const year = new Date().getFullYear();

        const [products, orders, totalDebt] = await Promise.all([
            fetch(`${API}/api/products`).then(r => r.json()),
            fetch(`${API}/api/orders`).then(r => r.json()),
            fetch(`${API}/api/orders/total-debt`).then(r => r.json())
        ]);

        const yearlyRevenue = orders
            .filter(o => new Date(o.orderedAt).getFullYear() === year)
            .reduce((sum, o) => sum + parseFloat(o.totalAmount || 0), 0);

        const yearlyProfit = orders
            .filter(o => new Date(o.orderedAt).getFullYear() === year)
            .reduce((sum, o) => {
                const items = o.items || [];
                const cost = items.reduce((s, i) => {
                    const product = products.find(p => p.id === i.productId);
                    return s + ((product?.costPrice || 0) * i.quantity);
                }, 0);
                return sum + (parseFloat(o.totalAmount) - cost);
            }, 0);

        const totalProducts = products.length;
        const yearlyOrders = orders.filter(o => new Date(o.orderedAt).getFullYear() === year).length;

        document.getElementById('daily-sales').textContent = `${formatNumber(yearlyRevenue)} ₼`;
        document.getElementById('total-products').textContent = formatNumber(yearlyProfit) + ' ₼';
        document.getElementById('total-customers').textContent = totalProducts;
        document.getElementById('active-employees').textContent = yearlyOrders;

        renderRecentOrders(orders.slice(0, 10));
    } catch(e) {
        console.error('Dashboard error:', e);
        showToast('Dashboard yüklənmədi: ' + e.message, 'error');
    }
}

function renderRecentOrders(orders) {
    const tbody = document.getElementById('recent-orders');
    if (!orders || !orders.length) {
        tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;padding:32px;color:#94a3b8;">Məlumat yoxdur</td></tr>';
        return;
    }

    tbody.innerHTML = orders.map(o => `
        <tr>
            <td>#${o.id}</td>
            <td>${o.customer?.fullName || 'Anonim'}</td>
            <td><strong>${o.totalAmount} ₼</strong></td>
            <td><span class="badge badge-${getStatusClass(o.paymentStatus)}">${getStatusText(o.paymentStatus)}</span></td>
            <td>${formatDate(o.orderedAt)}</td>
        </tr>
    `).join('');
}

// ═══════════════════════════════════════════════════════════
// POS
// ═══════════════════════════════════════════════════════════

async function loadPOS() {
    try {
        const [productsRes, employeesRes] = await Promise.all([
            fetch(`${API}/api/products`),
            fetch(`${API}/api/employees/active`)
        ]);

        products = await productsRes.json();
        employees = await employeesRes.json();

        renderProducts();
        renderCart();
        renderEmployeeDropdown();

        // Search listener
        const searchInput = document.getElementById('product-search');
        if (searchInput) {
            searchInput.replaceWith(searchInput.cloneNode(true));
            document.getElementById('product-search').addEventListener('input', (e) => {
                const query = e.target.value.toLowerCase();
                const filtered = products.filter(p =>
                    p.productName.toLowerCase().includes(query) ||
                    (p.barcode && p.barcode.includes(query))
                );
                renderProducts(filtered);
            });
        }

        // Phone listener
        const phoneInput = document.getElementById('customer-phone');
        if (phoneInput) {
            phoneInput.replaceWith(phoneInput.cloneNode(true));
            document.getElementById('customer-phone').addEventListener('blur', searchCustomer);
        }

        // Paid amount listener
        const paidInput = document.getElementById('paid-amount');
        if (paidInput) {
            paidInput.replaceWith(paidInput.cloneNode(true));
            document.getElementById('paid-amount').addEventListener('input', calculateDebt);
        }

    } catch(e) {
        console.error('POS error:', e);
        showToast('Məhsullar yüklənmədi: ' + e.message, 'error');
    }
}

function renderEmployeeDropdown() {
    const select = document.getElementById('sale-employee');
    if (!select) return;

    select.innerHTML = '<option value="">İşçi seç...</option>' +
        employees.map(e => `<option value="${e.id}">${e.fullName}</option>`).join('');
}

function renderProducts(list = products) {
    const container = document.getElementById('products-list');
    const availableProducts = list.filter(p => p.stockQuantity > 0);

    if (!availableProducts || !availableProducts.length) {
        container.innerHTML = '<div style="grid-column:1/-1;text-align:center;padding:60px;color:#94a3b8;">Stokda məhsul yoxdur</div>';
        return;
    }

    container.innerHTML = availableProducts.map(p => `
        <div class="product-card" onclick="addToCart(${p.id})">
            <div class="name">${p.productName}</div>
            <div class="price">${p.sellingPrice} ₼</div>
            <div class="stock">Stok: ${p.stockQuantity}</div>
        </div>
    `).join('');
}

function addToCart(id) {
    const product = products.find(p => p.id === id);
    if (!product) return;

    const existing = cart.find(c => c.id === id);
    if (existing) {
        if (existing.qty < product.stockQuantity) {
            existing.qty++;
        } else {
            showToast('Stokda kifayət qədər məhsul yoxdur!', 'warning');
            return;
        }
    } else {
        cart.push({
            id: product.id,
            name: product.productName,
            price: product.sellingPrice,
            qty: 1,
            maxStock: product.stockQuantity
        });
    }
    renderCart();
    showToast(`${product.productName} əlavə edildi`);
}

function renderCart() {
    const container = document.getElementById('cart-items');
    if (!cart.length) {
        container.innerHTML = '<div class="empty-cart">Səbət boşdur</div>';
        document.getElementById('cart-total').textContent = '0.00 ₼';
        return;
    }

    container.innerHTML = cart.map(item => `
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
    `).join('');

    const total = cart.reduce((sum, item) => sum + (item.price * item.qty), 0);
    document.getElementById('cart-total').textContent = `${total.toFixed(2)} ₼`;
    calculateDebt();
}

function updateQty(id, delta) {
    const item = cart.find(c => c.id === id);
    if (!item) return;

    item.qty += delta;
    if (item.qty <= 0) {
        cart = cart.filter(c => c.id !== id);
    } else if (item.qty > item.maxStock) {
        item.qty = item.maxStock;
        showToast('Stok limiti!', 'warning');
    }
    renderCart();
}

function clearCart() {
    if (cart.length && confirm('Səbəti təmizləmək istəyirsiniz?')) {
        cart = [];
        currentCustomer = null;
        document.getElementById('customer-phone').value = '';
        document.getElementById('customer-info').innerHTML = '';
        document.getElementById('paid-amount').value = '';
        document.getElementById('sale-employee').value = '';
        renderCart();
    }
}

async function searchCustomer() {
    const phone = document.getElementById('customer-phone').value.trim();
    const info = document.getElementById('customer-info');

    if (!phone) {
        currentCustomer = null;
        info.innerHTML = '';
        return;
    }

    try {
        const res = await fetch(`${API}/api/customers/phone/${encodeURIComponent(phone)}`);
        if (res.ok) {
            const customer = await res.json();
            currentCustomer = customer;
            info.innerHTML = `✓ ${customer.fullName}`;
            info.style.color = '#10b981';
        } else {
            currentCustomer = null;
            info.innerHTML = 'Müştəri tapılmadı';
            info.style.color = '#f59e0b';
        }
    } catch(e) {}
}

function calculateDebt() {
    const total = parseFloat(document.getElementById('cart-total').textContent) || 0;
    const paid = parseFloat(document.getElementById('paid-amount').value) || 0;
    const debt = total - paid;

    const display = document.getElementById('debt-display');
    if (debt > 0) {
        display.textContent = `Borc: ${debt.toFixed(2)} ₼`;
        display.style.display = 'block';
    } else {
        display.style.display = 'none';
    }
}

async function completeSale() {
    if (!cart.length) {
        showToast('Səbət boşdur!', 'error');
        return;
    }

    const employeeId = document.getElementById('sale-employee').value;
    if (!employeeId) {
        showToast('İşçi seçməlisən! (Bonus üçün lazımdır)', 'error');
        return;
    }

    const total = parseFloat(document.getElementById('cart-total').textContent) || 0;
    const paid = parseFloat(document.getElementById('paid-amount').value) || 0;

    if (total - paid > 0 && !currentCustomer) {
        showToast('Borca satış üçün müştəri lazımdır!', 'error');
        return;
    }

    try {
        const res = await fetch(`${API}/api/orders`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                employeeId: parseInt(employeeId),
                customerId: currentCustomer?.id || null,
                paidAmount: paid,
                items: cart.map(i => ({productId: i.id, quantity: i.qty}))
            })
        });

        if (res.ok) {
            showToast('Satış tamamlandı!');
            clearCart();
            loadDashboard();
        } else {
            showToast('Xəta baş verdi!', 'error');
        }
    } catch(e) {
        showToast('Əlaqə xətası!', 'error');
    }
}

// ═══════════════════════════════════════════════════════════
// PRODUCTS - YENİ WORKFLOW
// ═══════════════════════════════════════════════════════════

async function loadProducts() {
    try {
        const res = await fetch(`${API}/api/products`);
        const products = await res.json();

        const tbody = document.getElementById('products-table');

        if (!products || !products.length) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center;padding:32px;color:#94a3b8;">Məhsul yoxdur</td></tr>';
            return;
        }

        tbody.innerHTML = products.map(p => {
            let statusBadge = '';
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
                    <td>${p.barcode || '—'}</td>
                    <td><strong>${p.productName}</strong></td>
                    <td>${p.sellingPrice} ₼</td>
                    <td>${p.stockQuantity}</td>
                    <td>${statusBadge}</td>
                </tr>
            `;
        }).join('');
    } catch(e) {
        console.error('Products error:', e);
    }
}

function openProductModal() {
    resetProductModal();
    document.getElementById('product-modal').classList.add('active');
}

function resetProductModal() {
    existingProduct = null;
    document.getElementById('step-barcode').style.display = 'block';
    document.getElementById('step-stock-update').style.display = 'none';
    document.getElementById('step-new-product').style.display = 'none';
    document.getElementById('scanner-container').style.display = 'none';
    document.getElementById('manual-barcode-input').style.display = 'none';
    stopProductScanner();
}

function startProductScanner() {
    document.getElementById('scanner-container').style.display = 'block';

    productScanner = new Html5QrcodeScanner("product-scanner", {
        fps: 10,
        qrbox: {width: 250, height: 250}
    });

    productScanner.render(
        (decodedText) => {
            showToast('Barkod oxundu: ' + decodedText);
            stopProductScanner();
            checkBarcodeApi(decodedText);
        },
        (err) => {}
    );
}

function stopProductScanner() {
    if (productScanner) {
        productScanner.clear().catch(e => {});
        productScanner = null;
    }
}

function showManualBarcode() {
    document.getElementById('manual-barcode-input').style.display = 'block';
}

function checkBarcode() {
    const barcode = document.getElementById('manual-barcode').value.trim();
    if (!barcode) {
        showToast('Barkod daxil et!', 'error');
        return;
    }
    checkBarcodeApi(barcode);
}

async function checkBarcodeApi(barcode) {
    try {
        const res = await fetch(`${API}/api/products/barcode/${encodeURIComponent(barcode)}`);

        if (res.ok) {
            // Məhsul mövcuddur - stok artırma
            existingProduct = await res.json();
            document.getElementById('step-barcode').style.display = 'none';
            document.getElementById('step-stock-update').style.display = 'block';
            document.getElementById('existing-product-info').textContent =
                `${existingProduct.productName} - Cari stok: ${existingProduct.stockQuantity}`;
        } else {
            // Yeni məhsul
            document.getElementById('step-barcode').style.display = 'none';
            document.getElementById('step-new-product').style.display = 'block';
            document.getElementById('new-barcode').value = barcode;
        }
    } catch(e) {
        showToast('Xəta baş verdi!', 'error');
    }
}

async function addStock() {
    const qty = parseInt(document.getElementById('stock-add-qty').value);
    if (!qty || qty <= 0) {
        showToast('Miqdar düzgün deyil!', 'error');
        return;
    }

    try {
        const newStock = existingProduct.stockQuantity + qty;
        const res = await fetch(`${API}/api/products/barcode/${encodeURIComponent(existingProduct.barcode)}/stock?qty=${newStock}`, {
            method: 'PATCH'
        });

        if (res.ok) {
            showToast(`${qty} ədəd stok əlavə edildi!`);
            closeModal('product-modal');
            loadProducts();
        } else {
            showToast('Xəta baş verdi!', 'error');
        }
    } catch(e) {
        showToast('Əlaqə xətası!', 'error');
    }
}

async function convertCurrency() {
    const foreign = parseFloat(document.getElementById('new-foreign').value);
    const currency = document.getElementById('new-currency').value;

    if (!foreign) {
        showToast('Məbləğ daxil et!', 'error');
        return;
    }

    let azn = foreign;
    if (currency === 'USD') azn = foreign * usdRate;
    if (currency === 'EUR') azn = foreign * eurRate;

    document.getElementById('new-cost').value = azn.toFixed(2);
    showToast(`${foreign} ${currency} = ${azn.toFixed(2)} AZN`);
}

async function saveNewProduct() {
    const product = {
        barcode: document.getElementById('new-barcode').value,
        productName: document.getElementById('new-name').value.trim(),
        sellingPrice: parseFloat(document.getElementById('new-price').value),
        costPrice: parseFloat(document.getElementById('new-cost').value) || 0,
        category: document.getElementById('new-category').value.trim() || 'Ümumi',
        stockQuantity: parseInt(document.getElementById('new-stock').value)
    };

    if (!product.productName || !product.sellingPrice || !product.stockQuantity) {
        showToast('Məcburi sahələri doldurun!', 'error');
        return;
    }

    try {
        const res = await fetch(`${API}/api/products`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(product)
        });

        if (res.ok) {
            showToast('Məhsul əlavə edildi!');
            closeModal('product-modal');
            loadProducts();
        } else {
            const error = await res.text();
            showToast('Xəta: ' + error, 'error');
        }
    } catch(e) {
        showToast('Əlaqə xətası!', 'error');
    }
}

// ═══════════════════════════════════════════════════════════
// ORDERS
// ═══════════════════════════════════════════════════════════

async function loadOrders() {
    try {
        const orders = await fetch(`${API}/api/orders`).then(r => r.json());
        const tbody = document.getElementById('orders-table');

        if (!orders || !orders.length) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;padding:32px;color:#94a3b8;">Sifariş yoxdur</td></tr>';
            return;
        }

        tbody.innerHTML = orders.map(o => {
            const debt = (parseFloat(o.totalAmount) - parseFloat(o.paidAmount || 0)).toFixed(2);
            const canReturn = o.paymentStatus === 'PAID' || o.paymentStatus === 'PARTIAL';

            return `
                <tr>
                    <td>#${o.id}</td>
                    <td>${o.customer?.fullName || 'Anonim'}</td>
                    <td>${o.employee?.fullName || '—'}</td>
                    <td>${o.totalAmount} ₼</td>
                    <td>${o.paidAmount || 0} ₼</td>
                    <td>${debt} ₼</td>
                    <td><span class="badge badge-${getStatusClass(o.paymentStatus)}">${getStatusText(o.paymentStatus)}</span></td>
                    <td>${formatDate(o.orderedAt)}</td>
                    <td>
                        ${canReturn ? `<button class="btn-text" onclick="openReturnModal(${o.id})" style="color:#ef4444">İadə/Ləğv</button>` : '—'}
                    </td>
                </tr>
            `;
        }).join('');
    } catch(e) {}
}

// ═══════════════════════════════════════════════════════════
// CUSTOMERS
// ═══════════════════════════════════════════════════════════

async function loadCustomers() {
    try {
        const customers = await fetch(`${API}/api/customers`).then(r => r.json());
        const tbody = document.getElementById('customers-table');

        if (!customers || !customers.length) {
            tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;padding:32px;color:#94a3b8;">Müştəri yoxdur</td></tr>';
            return;
        }

        tbody.innerHTML = customers.map(c => `
            <tr>
                <td>#${c.id}</td>
                <td><strong>${c.fullName}</strong></td>
                <td>${c.phone || '—'}</td>
                <td>${formatDate(c.registeredAt)}</td>
            </tr>
        `).join('');
    } catch(e) {}
}

function openCustomerModal() {
    document.getElementById('customer-modal').classList.add('active');
}

async function saveCustomer() {
    const customer = {
        fullName: document.getElementById('c-name').value.trim(),
        phone: document.getElementById('c-phone').value.trim()
    };

    if (!customer.fullName || !customer.phone) {
        showToast('Ad və telefon məcburidir!', 'error');
        return;
    }

    try {
        const res = await fetch(`${API}/api/customers`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(customer)
        });

        if (res.ok) {
            showToast('Müştəri əlavə edildi!');
            closeModal('customer-modal');
            loadCustomers();
            document.getElementById('c-name').value = '';
            document.getElementById('c-phone').value = '';
        } else {
            showToast('Bu nömrə artıq qeydiyyatdadır!', 'error');
        }
    } catch(e) {
        showToast('Əlaqə xətası!', 'error');
    }
}

// ═══════════════════════════════════════════════════════════
// EMPLOYEES
// ═══════════════════════════════════════════════════════════

async function loadEmployees() {
    try {
        const employees = await fetch(`${API}/api/employees`).then(r => r.json());
        const tbody = document.getElementById('employees-table');

        if (!employees || !employees.length) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:32px;color:#94a3b8;">İşçi yoxdur</td></tr>';
            return;
        }

        tbody.innerHTML = employees.map(e => `
            <tr>
                <td>#${e.id}</td>
                <td><strong>${e.fullName}</strong></td>
                <td>${e.phone || '—'}</td>
                <td>${e.baseSalary} ₼</td>
                <td>${formatDate(e.hiredAt)}</td>
                <td><span class="badge badge-${e.active ? 'success' : 'danger'}">${e.active ? 'Aktiv' : 'Deaktiv'}</span></td>
                <td>
                    ${e.active ? `<button class="btn-text" style="color:#ef4444;" onclick="deactivateEmployee(${e.id})">Deaktiv et</button>` : '—'}
                </td>
            </tr>
        `).join('');
    } catch(e) {}
}

function openEmployeeModal() {
    document.getElementById('employee-modal').classList.add('active');
}

async function saveEmployee() {
    const employee = {
        fullName: document.getElementById('emp-name').value.trim(),
        phone: document.getElementById('emp-phone').value.trim() || null,
        baseSalary: parseFloat(document.getElementById('emp-salary').value),
        hiredAt: document.getElementById('emp-hired').value || new Date().toISOString().split('T')[0]
    };

    if (!employee.fullName || !employee.baseSalary) {
        showToast('Ad və maaş məcburidir!', 'error');
        return;
    }

    try {
        const res = await fetch(`${API}/api/employees`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(employee)
        });

        if (res.ok) {
            showToast('İşçi əlavə edildi!');
            closeModal('employee-modal');
            loadEmployees();
            document.getElementById('emp-name').value = '';
            document.getElementById('emp-phone').value = '';
            document.getElementById('emp-salary').value = '';
            document.getElementById('emp-hired').value = '';
        } else {
            showToast('Xəta baş verdi!', 'error');
        }
    } catch(e) {
        showToast('Əlaqə xətası!', 'error');
    }
}

async function deactivateEmployee(id) {
    if (!confirm('Bu işçini deaktiv etmək istəyirsiniz?')) return;

    try {
        const res = await fetch(`${API}/api/employees/${id}/deactivate`, {
            method: 'PATCH'
        });

        if (res.ok) {
            showToast('İşçi deaktiv edildi!');
            loadEmployees();
        } else {
            showToast('Xəta baş verdi!', 'error');
        }
    } catch(e) {
        showToast('Əlaqə xətası!', 'error');
    }
}

// ═══════════════════════════════════════════════════════════
// DEBTS
// ═══════════════════════════════════════════════════════════

async function loadDebts() {
    try {
        const [debts, partials] = await Promise.all([
            fetch(`${API}/api/orders/debts`).then(r => r.json()),
            fetch(`${API}/api/orders/partials`).then(r => r.json())
        ]);
        const all = [...debts, ...partials];
        const tbody = document.getElementById('debts-table');

        if (!all.length) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align:center;padding:32px;color:#94a3b8;">Borc yoxdur</td></tr>';
            return;
        }

        tbody.innerHTML = all.map(o => {
            const debt = (parseFloat(o.totalAmount) - parseFloat(o.paidAmount || 0)).toFixed(2);
            return `
                <tr>
                    <td>#${o.id}</td>
                    <td><strong>${o.customer?.fullName || 'Anonim'}</strong></td>
                    <td>${o.totalAmount} ₼</td>
                    <td>${o.paidAmount || 0} ₼</td>
                    <td><strong style="color:#ef4444">${debt} ₼</strong></td>
                    <td>${formatDate(o.orderedAt)}</td>
                    <td><button class="btn-primary" style="padding:6px 12px;font-size:13px" onclick="payDebt(${o.id}, ${debt})">Ödə</button></td>
                </tr>
            `;
        }).join('');
    } catch(e) {}
}

async function payDebt(orderId, debt) {
    const amount = prompt(`Ödəniləcək məbləğ (Borc: ${debt} ₼):`, debt);
    if (!amount || parseFloat(amount) <= 0) return;

    try {
        const res = await fetch(`${API}/api/orders/${orderId}/pay?amount=${amount}`, {method: 'PATCH'});
        if (res.ok) {
            showToast('Ödəniş edildi!');
            loadDebts();
            loadDashboard();
        } else {
            showToast('Xəta!', 'error');
        }
    } catch(e) {
        showToast('Əlaqə xətası!', 'error');
    }
}

// ═══════════════════════════════════════════════════════════
// EXCEL EXPORT
// ═══════════════════════════════════════════════════════════

async function exportMonthly() {
    const year = prompt('İl:', new Date().getFullYear());
    const month = prompt('Ay (1-12):', new Date().getMonth() + 1);

    if (!year || !month) return;

    try {
        showToast('Excel hazırlanır...');
        const response = await fetch(`${API}/api/export/monthly?year=${year}&month=${month}`);

        if (!response.ok) {
            showToast('Export xətası: ' + response.status, 'error');
            return;
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = `hesabat-${year}-${month}.xlsx`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);

        showToast('Excel yükləndi! ✓');
    } catch(e) {
        showToast('Xəta: ' + e.message, 'error');
    }
}

async function exportBonus() {
    const year = prompt('İl:', new Date().getFullYear());
    const quarter = prompt('Rüb (1-4):', 1);

    if (!year || !quarter) return;

    try {
        showToast('Excel hazırlanır...');
        const response = await fetch(`${API}/api/export/bonus?year=${year}&quarter=${quarter}`);

        if (!response.ok) {
            showToast('Export xətası: ' + response.status, 'error');
            return;
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = `bonus-${year}-Q${quarter}.xlsx`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);

        showToast('Excel yükləndi! ✓');
    } catch(e) {
        showToast('Xəta: ' + e.message, 'error');
    }
}

async function exportProducts() {
    try {
        showToast('Excel hazırlanır...');
        const response = await fetch(`${API}/api/export/products`);

        if (!response.ok) {
            showToast('Export xətası: ' + response.status, 'error');
            return;
        }

        const blob = await response.blob();
        const downloadUrl = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = downloadUrl;
        link.download = 'mehsullar.xlsx';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(downloadUrl);

        showToast('Excel yükləndi! ✓');
    } catch(e) {
        showToast('Xəta: ' + e.message, 'error');
    }
}

// ═══════════════════════════════════════════════════════════
// POS BARCODE SCANNER
// ═══════════════════════════════════════════════════════════

function openScanner() {
    document.getElementById('scanner-modal').classList.add('active');

    html5QrcodeScanner = new Html5QrcodeScanner("pos-scanner-container", {
        fps: 10,
        qrbox: {width: 250, height: 250}
    });

    html5QrcodeScanner.render(
        (decodedText) => {
            document.getElementById('scanner-result').textContent = '✓ ' + decodedText;

            // Məhsulu tap və səbətə at
            const product = products.find(p => p.barcode === decodedText);
            if (product) {
                addToCart(product.id);
            } else {
                showToast('Məhsul tapılmadı!', 'error');
            }

            setTimeout(() => closeScanner(), 1000);
        },
        (err) => {}
    );
}

function closeScanner() {
    if (html5QrcodeScanner) {
        html5QrcodeScanner.clear().catch(e => {});
        html5QrcodeScanner = null;
    }
    document.getElementById('scanner-modal').classList.remove('active');
    document.getElementById('scanner-result').textContent = '';
}

// ═══════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════

function closeModal(id) {
    if (id === 'product-modal') {
        stopProductScanner();
    }
    document.getElementById(id).classList.remove('active');
}

// ═══════════════════════════════════════════════════════════
// RETURN / CANCEL ORDER
// ═══════════════════════════════════════════════════════════

let returnOrderId = null;

function openReturnModal(orderId) {
    returnOrderId = orderId;
    document.getElementById('return-modal').classList.add('active');
}

async function confirmReturn() {
    if (!returnOrderId) return;

    try {
        const res = await fetch(`${API}/api/orders/${returnOrderId}/cancel`, {
            method: 'PATCH'
        });

        if (res.ok) {
            showToast('Sifariş ləğv edildi!');
            closeModal('return-modal');
            returnOrderId = null;
            loadOrders();
        } else {
            showToast('Xəta baş verdi!', 'error');
        }
    } catch(e) {
        showToast('Əlaqə xətası!', 'error');
    }
}

// ═══════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════

function showToast(message, type = 'success') {
    const toast = document.getElementById('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.className = `toast ${type} active`;
    setTimeout(() => toast.classList.remove('active'), 3000);
}

function formatNumber(num) {
    return new Intl.NumberFormat('az-AZ', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2
    }).format(num);
}

function formatDate(dateString) {
    if (!dateString) return '—';
    return new Date(dateString).toLocaleDateString('az-AZ');
}

function getStatusClass(status) {
    const map = {'PAID': 'success', 'DEBT': 'danger', 'PARTIAL': 'warning'};
    return map[status] || 'secondary';
}

function getStatusText(status) {
    const map = {'PAID': 'Ödənildi', 'DEBT': 'Borclu', 'PARTIAL': 'Qismən'};
    return map[status] || status;
}

document.addEventListener('click', (e) => {
    if (e.target.classList.contains('modal')) {
        const modalId = e.target.id;
        if (modalId === 'product-modal') {
            stopProductScanner();
        }
        e.target.classList.remove('active');
    }
});