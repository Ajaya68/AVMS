import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { PRODUCTS } from '../data/catalog';

const CART_KEY = 'av-cart-v1';
const ORDERS_KEY = 'av-orders-v1';

const ShopCtx = createContext(null);

function read(key, fallback) {
  try {
    const raw = localStorage.getItem(key);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

export function ShopProvider({ children }) {
  const [cart, setCart] = useState(() => read(CART_KEY, []));
  const [orders, setOrders] = useState(() => read(ORDERS_KEY, []));
  const [cartOpen, setCartOpen] = useState(false);

  useEffect(() => localStorage.setItem(CART_KEY, JSON.stringify(cart)), [cart]);
  useEffect(() => localStorage.setItem(ORDERS_KEY, JSON.stringify(orders)), [orders]);

  const add = (id, qty = 1) =>
    setCart((c) => {
      const found = c.find((i) => i.id === id);
      if (found) return c.map((i) => (i.id === id ? { ...i, qty: i.qty + qty } : i));
      return [...c, { id, qty }];
    });
  const setQty = (id, qty) =>
    setCart((c) => (qty <= 0 ? c.filter((i) => i.id !== id) : c.map((i) => (i.id === id ? { ...i, qty } : i))));
  const clear = () => setCart([]);

  const lines = useMemo(
    () =>
      cart
        .map((i) => ({ ...i, product: PRODUCTS.find((p) => p.id === i.id) }))
        .filter((i) => i.product),
    [cart],
  );
  const subtotal = lines.reduce((s, l) => s + l.product.price * l.qty, 0);
  const delivery = lines.length === 0 ? 0 : subtotal >= 2000 ? 0 : 100;
  const total = subtotal + delivery;

  const placeOrder = ({ name, phone, address, note }) => {
    const order = {
      id: 'AV-' + Math.floor(100000 + Math.random() * 900000),
      date: new Date().toISOString(),
      name,
      phone,
      address,
      note: note || '',
      items: lines.map((l) => ({ id: l.id, name: l.product.name, price: l.product.price, per: l.product.per, qty: l.qty })),
      subtotal,
      delivery,
      total,
      status: 'Received',
    };
    setOrders((o) => [order, ...o]);
    clear();
    return order;
  };

  const findOrders = (query) => {
    const q = (query || '').trim().toLowerCase();
    if (!q) return [];
    return orders.filter(
      (o) => o.id.toLowerCase() === q || o.phone.replace(/\D/g, '').includes(q.replace(/\D/g, '')),
    );
  };

  return (
    <ShopCtx.Provider value={{ cart, lines, subtotal, delivery, total, add, setQty, clear, cartOpen, setCartOpen, orders, placeOrder, findOrders, count: lines.reduce((s, l) => s + l.qty, 0) }}>
      {children}
    </ShopCtx.Provider>
  );
}

export function useShop() {
  const ctx = useContext(ShopCtx);
  if (!ctx) throw new Error('useShop must be used within ShopProvider');
  return ctx;
}
