// Public catalog — AB Mushroom Farming + Ajaya Fish Farming.
// Prices in NPR. Stock managed by admin dashboards (farmStore) and merged here.

export const PRODUCTS = [
  { id: 'mus-oyster', unit: 'fish-mushroom', kind: 'mushroom', name: 'Fresh Oyster Mushroom', emoji: '🍄', price: 350, per: 'kg', desc: 'Harvested daily, chemical-free, farm fresh.', tag: 'Bestseller', stock: 120 },
  { id: 'mus-button', unit: 'fish-mushroom', kind: 'mushroom', name: 'Button Mushroom', emoji: '🤍', price: 450, per: 'kg', desc: 'Premium white buttons for hotels & kitchens.', tag: 'Premium', stock: 60 },
  { id: 'mus-shiitake', unit: 'fish-mushroom', kind: 'mushroom', name: 'Shiitake Mushroom', emoji: '🍂', price: 900, per: 'kg', desc: 'Meaty, aromatic — limited seasonal batches.', tag: 'Limited', stock: 25 },
  { id: 'mus-spawn', unit: 'fish-mushroom', kind: 'mushroom', name: 'Mushroom Spawn (seed)', emoji: '🌱', price: 250, per: 'packet', desc: 'High-yield oyster spawn for home growers.', tag: 'Growers', stock: 200 },
  { id: 'mus-substrate', unit: 'fish-mushroom', kind: 'mushroom', name: 'Ready-to-fruit Bags', emoji: '🛍️', price: 180, per: 'bag', desc: 'Colonised substrate bags — fruits in 7–10 days.', tag: 'DIY', stock: 300 },
  { id: 'mus-dry', unit: 'fish-mushroom', kind: 'mushroom', name: 'Dried Oyster Mushroom', emoji: '🥓', price: 1200, per: 'kg', desc: 'Sun-dried, long shelf life, export grade.', tag: 'Export', stock: 40 },
  { id: 'fish-rohu', unit: 'fish', kind: 'fish', name: 'Rohu Fish (fresh)', emoji: '🐟', price: 550, per: 'kg', desc: 'Pond-raised, same-day harvest & delivery.', tag: 'Bestseller', stock: 150 },
  { id: 'fish-katla', unit: 'fish', kind: 'fish', name: 'Katla Fish', emoji: '🐠', price: 520, per: 'kg', desc: 'Sweet-fleshed carp, family favourite.', tag: '', stock: 100 },
  { id: 'fish-tilapia', unit: 'fish', kind: 'fish', name: 'Tilapia (Nile)', emoji: '🐡', price: 480, per: 'kg', desc: 'Boneless-friendly, fast-cooking fillets.', tag: 'Value', stock: 130 },
  { id: 'fish-pangas', unit: 'fish', kind: 'fish', name: 'Pangasius', emoji: '🦈', price: 420, per: 'kg', desc: 'Soft white flesh — ideal for fry & curry.', tag: '', stock: 90 },
  { id: 'fish-finger', unit: 'fish', kind: 'fish', name: 'Fingerlings (seed)', emoji: '🐣', price: 15, per: 'piece', desc: 'Healthy rohu/katla/tilapia seed for ponds.', tag: 'Farmers', stock: 5000 },
  { id: 'fish-smoked', unit: 'fish', kind: 'fish', name: 'Smoked Fish', emoji: '🔥', price: 950, per: 'kg', desc: 'Traditionally smoked, ready to cook.', tag: 'Special', stock: 30 },
];

export const SERVICES = [
  { icon: '🏗️', title: 'Mushroom Farm Setup', desc: 'Shed design, racks, humidification & spawn-to-harvest commissioning for AB-style units.', price: 'From NPR 85,000' },
  { icon: '🐟', title: 'Fish Pond Setup & Stocking', desc: 'Pond preparation, liming, water testing, fingerling stocking & aeration guidance.', price: 'From NPR 60,000' },
  { icon: '🎓', title: 'Grower Training', desc: '1-day hands-on training: substrate prep, inoculation, pest control, harvesting.', price: 'NPR 3,500 / person' },
  { icon: '🧪', title: 'Spawn & Seed Supply', desc: 'Lab-grade mushroom spawn and disease-free fingerlings with replacement guarantee.', price: 'Bulk rates' },
  { icon: '🚚', title: 'Farm-to-Door Delivery', desc: 'Same-day fresh delivery inside the valley for orders before 2 PM.', price: 'NPR 100 flat' },
  { icon: '📋', title: 'Contract Growing / Buy-back', desc: 'We train you, supply inputs, and buy back your harvest at fixed rates.', price: 'Partner plan' },
];

export const fmtNPR = (n) => `NPR ${Number(n || 0).toLocaleString('en-IN')}`;
