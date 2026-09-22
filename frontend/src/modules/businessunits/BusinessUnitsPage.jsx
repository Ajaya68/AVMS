import AsyncData from '../../components/AsyncData';
import api from '../../services/api';
import { useAuth } from '../auth/AuthContext';
import { useReveal } from '../../hooks/useUi';

const BU_ART = { MUSHROOM: '🍄', FISH: '🐟' };

export default function BusinessUnitsPage() {
  const { activeBusinessUnitId, selectBusinessUnit } = useAuth();
  const ref = useReveal();

  return (
    <AsyncData load={() => api.get('/api/v1/business-units', { params: { page: 1, page_size: 100 } })} deps={[]}>
      {({ data }) => {
        const units = data.business_units || [];
        return (
          <div ref={ref} className="av-reveal">
            <h1 className="fw-extrabold mb-1" style={{ letterSpacing: '-0.02em' }}>
              Business <span className="av-gradient-text">Units</span>
            </h1>
            <p className="text-muted mb-4">Tap a card to switch your working scope — permissions re-check server-side.</p>
            <div className="row row-cols-1 row-cols-md-2 row-cols-xl-3 g-3 av-stagger is-visible">
              {units.map((bu) => {
                const isActive = activeBusinessUnitId === bu.business_unit_id;
                const art = BU_ART[(bu.business_unit_type || '').toUpperCase()] || '🏭';
                return (
                  <div className="col" key={bu.business_unit_id}>
                    <div
                      className="av-glass p-4 h-100 av-card-hover position-relative overflow-hidden"
                      style={isActive ? { outline: '2px solid #6366f1' } : undefined}
                    >
                      {isActive && (
                        <span className="av-chip up position-absolute" style={{ top: 14, right: 14 }}>● Active</span>
                      )}
                      <div className="av-float" style={{ fontSize: '3rem' }}>{art}</div>
                      <h2 className="h6 fw-bold mt-2 mb-1">{bu.business_unit_name}</h2>
                      <div className="d-flex gap-2 mb-3">
                        <span className="badge text-bg-secondary">{bu.business_unit_code}</span>
                        <span className="badge rounded-pill" style={{ background: 'rgba(99,102,241,.14)', color: 'var(--av-ink)' }}>
                          {bu.business_unit_type}
                        </span>
                      </div>
                      <button
                        className={`btn btn-sm w-100 av-btn-press ${isActive ? 'btn-success' : 'av-gradient-btn'}`}
                        disabled={isActive}
                        onClick={() => selectBusinessUnit(bu.business_unit_id)}
                      >
                        {isActive ? '✓ Active scope' : 'Use as business scope →'}
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        );
      }}
    </AsyncData>
  );
}
