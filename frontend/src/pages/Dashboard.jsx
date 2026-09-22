import { Link } from 'react-router-dom';
import { useAuth } from '../modules/auth/AuthContext';
import { useCountUp, useReveal } from '../hooks/useUi';

function Sparkline({ points, color = '#6366f1' }) {
  const w = 120;
  const h = 36;
  const max = Math.max(...points, 1);
  const min = Math.min(...points, 0);
  const path = points
    .map((p, i) => {
      const x = (i / (points.length - 1)) * w;
      const y = h - 4 - ((p - min) / (max - min || 1)) * (h - 10);
      return `${i === 0 ? 'M' : 'L'}${x.toFixed(1)},${y.toFixed(1)}`;
    })
    .join(' ');
  return (
    <svg width={w} height={h} viewBox={`0 0 ${w} ${h}`} aria-hidden="true">
      <path d={path} fill="none" stroke={color} strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
      <path d={`${path} L${w},${h} L0,${h} Z`} fill={color} opacity="0.12" stroke="none" />
    </svg>
  );
}

function StatCard({ icon, label, value, delta, trend, spark, color }) {
  const animated = useCountUp(value);
  return (
    <div className="av-glass av-stat av-card-hover">
      <div className="d-flex justify-content-between align-items-start mb-2">
        <span
          className="d-grid"
          style={{
            width: 44, height: 44, placeItems: 'center', borderRadius: 13,
            background: `${color}1a`, fontSize: 22,
          }}
        >
          {icon}
        </span>
        <span className={`av-chip ${trend}`}>{delta}</span>
      </div>
      <div className="av-num">{animated.toLocaleString()}</div>
      <div className="text-muted small fw-semibold">{label}</div>
      <div className="mt-2"><Sparkline points={spark} color={color} /></div>
    </div>
  );
}

export default function Dashboard() {
  const { user, businessUnits, activeBusinessUnitId } = useAuth();
  const gridRef = useReveal();
  const active = businessUnits.find((b) => b.business_unit_id === activeBusinessUnitId);

  const stats = [
    { icon: '🍄', label: 'Mushroom Batches (active)', value: 24, delta: '▲ 12%', trend: 'up', spark: [4, 7, 5, 9, 12, 14, 18, 24], color: '#8b5cf6' },
    { icon: '🐟', label: 'Fish Stock (kg)', value: 1840, delta: '▲ 8%', trend: 'up', spark: [900, 1100, 1050, 1300, 1500, 1620, 1840], color: '#06b6d4' },
    { icon: '💰', label: 'Revenue (this month)', value: 96500, delta: '▲ 18%', trend: 'up', spark: [40, 52, 48, 66, 74, 88, 96], color: '#10b981' },
    { icon: '📦', label: 'Inventory Value', value: 41200, delta: '▼ 3%', trend: 'down', spark: [50, 48, 52, 46, 44, 43, 41], color: '#f59e0b' },
  ];

  return (
    <div>
      {/* Hero */}
      <div
        className="av-glass p-4 p-md-5 mb-4 position-relative overflow-hidden av-pop-in"
        style={{ background: 'linear-gradient(120deg, rgba(99,102,241,.16), rgba(6,182,212,.1)), var(--av-card)' }}
      >
        <div className="row align-items-center g-3">
          <div className="col-12 col-lg-8">
            <span className="av-chip up mb-2">● Live · {active ? active.business_unit_name : 'All businesses'}</span>
            <h1 className="fw-extrabold mb-1" style={{ letterSpacing: '-0.03em', fontSize: 'clamp(1.5rem, 3.4vw, 2.4rem)' }}>
              Namaste, {user?.username || 'Grower'} <span className="av-gradient-text">let&apos;s grow more.</span>
            </h1>
            <p className="text-muted mb-3">
              Mushroom sheds, fish ponds, sales & expenses — one animated command center.
              Full metrics arrive in the Reporting phase.
            </p>
            <div className="d-flex gap-2 flex-wrap">
              <Link to="/business-units" className="btn av-gradient-btn av-btn-press">🏭 Switch business scope</Link>
              <Link to="/users" className="btn btn-outline-primary av-btn-press" style={{ borderRadius: 12 }}>👥 Manage team</Link>
            </div>
          </div>
          <div className="col-12 col-lg-4 text-center av-hide-sm">
            <div className="av-float" style={{ fontSize: '5.5rem', filter: 'drop-shadow(0 18px 30px rgba(99,102,241,.35))' }}>
              🍄🐟
            </div>
          </div>
        </div>
      </div>

      {/* KPI grid with staggered entrance */}
      <div ref={gridRef} className="row g-3 av-stagger">
        {stats.map((s) => (
          <div className="col-12 col-sm-6 col-xl-3" key={s.label}>
            <StatCard {...s} />
          </div>
        ))}
      </div>

      {/* Activity + quick actions */}
      <div className="row g-3 mt-1">
        <div className="col-12 col-lg-7">
          <div className="av-glass p-4 h-100">
            <div className="d-flex justify-content-between align-items-center mb-3">
              <h2 className="h6 fw-bold mb-0">⚡ Today&apos;s activity</h2>
              <span className="badge text-bg-primary">Live feed</span>
            </div>
            {[
              ['🍄', 'Batch #M-142 harvested — 42 kg oyster', '2h ago'],
              ['🐟', 'Pond B water quality checked — pH 7.1', '4h ago'],
              ['💰', 'Sale recorded — NPR 18,500 (local market)', '6h ago'],
              ['📦', 'Substrate bags restocked — 300 units', 'Yesterday'],
            ].map(([icon, text, time]) => (
              <div key={text} className="d-flex gap-3 align-items-start py-2 border-bottom">
                <span style={{ fontSize: 20 }}>{icon}</span>
                <span className="flex-grow-1 small fw-semibold">{text}</span>
                <span className="text-muted small flex-shrink-0">{time}</span>
              </div>
            ))}
          </div>
        </div>
        <div className="col-12 col-lg-5">
          <div className="av-glass p-4 h-100" style={{ background: 'linear-gradient(160deg, rgba(139,92,246,.14), transparent 55%), var(--av-card)' }}>
            <h2 className="h6 fw-bold mb-3">🚀 Quick actions</h2>
            <div className="d-grid gap-2">
              <Link to="/organization" className="btn btn-light text-start av-btn-press">🏢 View organization profile →</Link>
              <Link to="/business-units" className="btn btn-light text-start av-btn-press">🏭 Explore AB Mushroom & Fish units →</Link>
              <Link to="/users" className="btn btn-light text-start av-btn-press">👥 Invite a team member →</Link>
            </div>
            <div className="alert alert-info mt-3 mb-0 small av-fade-alert">
              💡 Tip: switch the business scope in the top bar — every API request is re-verified server-side.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
