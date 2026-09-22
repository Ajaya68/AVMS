import { createContext, useContext, useEffect, useState } from 'react';

// Admin data-entry store (mushroom batches + fish ponds/activities).
// Persists to localStorage so admins can manage business data until the
// backend Reporting/Inventory phase lands. Seeded with demo rows.

const KEY = 'av-farm-data-v1';

const seed = {
  mushroomBatches: [
    { id: 'M-141', variety: 'Oyster', bags: 300, inoculated: '2026-08-28', status: 'Fruiting', expectedKg: 90, harvestedKg: 42 },
    { id: 'M-142', variety: 'Oyster', bags: 250, inoculated: '2026-09-05', status: 'Colonising', expectedKg: 75, harvestedKg: 0 },
    { id: 'M-143', variety: 'Button', bags: 200, inoculated: '2026-09-12', status: 'Preparing', expectedKg: 40, harvestedKg: 0 },
  ],
  fishPonds: [
    { id: 'Pond-A', species: 'Rohu + Katla', fingerlings: 2000, stockedOn: '2026-05-10', feedKg: 320, harvestedKg: 150, health: 'Good (pH 7.1)' },
    { id: 'Pond-B', species: 'Tilapia', fingerlings: 3000, stockedOn: '2026-06-02', feedKg: 410, harvestedKg: 60, health: 'Good (pH 7.3)' },
  ],
  activities: [
    { at: '2026-09-22 08:30', unit: 'mushroom', text: 'Batch M-141 harvested — 42 kg oyster' },
    { at: '2026-09-21 16:00', unit: 'fish', text: 'Pond B water quality checked — pH 7.1' },
  ],
};

function load() {
  try {
    const raw = localStorage.getItem(KEY);
    if (raw) return { ...seed, ...JSON.parse(raw) };
  } catch { /* ignore */ }
  return seed;
}

const FarmCtx = createContext(null);

export function FarmProvider({ children }) {
  const [data, setData] = useState(load);
  useEffect(() => {
    try { localStorage.setItem(KEY, JSON.stringify(data)); } catch { /* ignore */ }
  }, [data]);

  const log = (unit, text) =>
    setData((d) => ({
      ...d,
      activities: [{ at: new Date().toLocaleString(), unit, text }, ...d.activities].slice(0, 50),
    }));

  const addBatch = (b) => {
    setData((d) => ({ ...d, mushroomBatches: [b, ...d.mushroomBatches] }));
    log('mushroom', `Batch ${b.id} added — ${b.bags} bags ${b.variety}`);
  };
  const updateBatch = (id, patch) =>
    setData((d) => ({ ...d, mushroomBatches: d.mushroomBatches.map((b) => (b.id === id ? { ...b, ...patch } : b)) }));
  const removeBatch = (id) =>
    setData((d) => ({ ...d, mushroomBatches: d.mushroomBatches.filter((b) => b.id !== id) }));

  const addPond = (p) => {
    setData((d) => ({ ...d, fishPonds: [p, ...d.fishPonds] }));
    log('fish', `${p.id} stocked — ${p.fingerlings} ${p.species}`);
  };
  const updatePond = (id, patch) =>
    setData((d) => ({ ...d, fishPonds: d.fishPonds.map((p) => (p.id === id ? { ...p, ...patch } : p)) }));
  const removePond = (id) =>
    setData((d) => ({ ...d, fishPonds: d.fishPonds.filter((p) => p.id !== id) }));

  return (
    <FarmCtx.Provider value={{ ...data, addBatch, updateBatch, removeBatch, addPond, updatePond, removePond, log }}>
      {children}
    </FarmCtx.Provider>
  );
}

export function useFarm() {
  const ctx = useContext(FarmCtx);
  if (!ctx) throw new Error('useFarm must be used within FarmProvider');
  return ctx;
}
