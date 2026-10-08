// Firebase Config Placeholder (Gunakan Project Firebase yang sama dengan Android)
const firebaseConfig = {
    apiKey: "AIzaSyDummyKeyPesenHubJenggirat2026",
    authDomain: "pesenhub-jenggirat.firebaseapp.com",
    projectId: "pesenhub-jenggirat",
    storageBucket: "pesenhub-jenggirat.appspot.com",
    messagingSenderId: "123456789",
    appId: "1:123456789:web:abcdef123456"
};

// Inisialisasi Firebase
let db = null;
try {
    firebase.initializeApp(firebaseConfig);
    db = firebase.firestore();
    pantauPesananRealtime();
} catch (e) {
    console.warn("Firebase belum terkonfigurasi dengan project asli:", e);
}

// 1. Fungsi Kirim Pesanan dari Web Pelanggan
function kirimPesananWeb() {
    const nama = document.getElementById("custName").value.trim();
    const hp = document.getElementById("custPhone").value.trim();
    const select = document.getElementById("menuSelect");
    const menu = select.value;
    const harga = parseInt(select.selectedOptions[0].getAttribute("data-price") || "0");
    const notes = document.getElementById("custNotes").value.trim();
    const metode = document.querySelector('input[name="payMethod"]:checked').value;

    if (!nama || !hp) {
        alert("Nama dan nomor HP wajib diisi!");
        return;
    }

    const orderId = "ORD-WEB-" + Date.now().toString().slice(-6);

    const payload = {
        orderNumber: orderId,
        customerName: nama,
        customerPhone: hp,
        menuItem: menu,
        paymentMethod: metode,
        total: harga,
        notes: notes,
        status: "PENDING",
        source: "CUSTOMER_WEB",
        createdAt: firebase.firestore.FieldValue.serverTimestamp()
    };

    if (db) {
        db.collection("orders").doc(orderId).set(payload)
            .then(() => {
                alert(`Pesanan ${orderId} berhasil dikirim! Silakan tunggu konfirmasi kasir.`);
                document.getElementById("custName").value = "";
                document.getElementById("custPhone").value = "";
                document.getElementById("custNotes").value = "";
            })
            .catch(err => {
                alert("Gagal mengirim ke cloud Firestore: " + err.message);
            });
    } else {
        alert(`[Demo Mode] Pesanan ${orderId} dibuat untuk ${nama} (${menu}) senilai Rp ${harga.toLocaleString()}`);
    }
}

// 2. Fungsi Pantau Pesanan Realtime (Firestore Snapshot Listener)
function pantauPesananRealtime() {
    if (!db) return;

    db.collection("orders")
        .orderBy("createdAt", "desc")
        .limit(10)
        .onSnapshot(snapshot => {
            const container = document.getElementById("orderList");
            if (snapshot.empty) {
                container.innerHTML = `<p class="text-sm text-gray-400 italic">Belum ada pesanan aktif.</p>`;
                return;
            }

            container.innerHTML = "";
            snapshot.forEach(doc => {
                const data = doc.data();
                const statusColor = data.status === "COMPLETED" ? "bg-green-100 text-green-700" :
                                    data.status === "PREPARING" ? "bg-blue-100 text-blue-700" :
                                    "bg-amber-100 text-amber-700";

                const div = document.createElement("div");
                div.className = "flex justify-between items-center p-3 bg-gray-50 rounded-lg border border-gray-100";
                div.innerHTML = `
                    <div>
                        <div class="font-bold text-sm text-gray-800">#${data.orderNumber || doc.id} — ${data.customerName || 'Pelanggan'}</div>
                        <div class="text-xs text-gray-500">${data.menuItem || '-'} (${data.paymentMethod || 'Tunai'})</div>
                    </div>
                    <div class="text-right">
                        <span class="text-xs font-semibold px-2 py-1 rounded-full ${statusColor}">${data.status || 'PENDING'}</span>
                        <div class="text-xs font-bold text-amber-600 mt-1">Rp ${(data.total || 0).toLocaleString()}</div>
                    </div>
                `;
                container.appendChild(div);
            });
        }, err => {
            console.error("Listener error:", err);
        });
}
