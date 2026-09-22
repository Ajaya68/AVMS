import AsyncData from '../../components/AsyncData';
import api from '../../services/api';
import { useReveal } from '../../hooks/useUi';

export default function OrganizationPage() {
  const ref = useReveal();
  return (
    <AsyncData load={() => api.get('/api/v1/organizations')} deps={[]}>
      {({ data }) => {
        const orgs = data.organizations || [];
        if (orgs.length === 0) {
          return <p className="text-muted">No organization found.</p>;
        }
        const org = orgs[0];
        const rows = [
          ['🔖', 'Code', org.organization_code],
          ['🏢', 'Name', org.organization_name],
          ['✉️', 'Email', org.email || '—'],
          ['📞', 'Phone', org.phone || '—'],
          ['📍', 'Address', org.address_line1 || '—'],
          ['💱', 'Currency', org.currency_code || '—'],
          ['📅', 'Fiscal Year', org.fiscal_year_start_month ? `Month ${org.fiscal_year_start_month}` : '—'],
        ];
        return (
          <div ref={ref} className="av-reveal">
            <span className="av-chip up mb-2">● Active organization</span>
            <h1 className="fw-extrabold mb-1" style={{ letterSpacing: '-0.02em' }}>
              {org.organization_name}
            </h1>
            <p className="text-muted mb-4">Code <code>{org.organization_code}</code> · everything rolls up here</p>
            <div className="row g-3">
              {rows.map(([icon, k, v]) => (
                <div className="col-12 col-sm-6 col-lg-4" key={k}>
                  <div className="av-glass p-3 av-card-hover d-flex gap-3 align-items-center">
                    <span style={{ fontSize: 24 }}>{icon}</span>
                    <span>
                      <span className="d-block text-muted small fw-semibold text-uppercase" style={{ letterSpacing: '.06em', fontSize: '.7rem' }}>{k}</span>
                      <span className="d-block fw-bold">{v}</span>
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        );
      }}
    </AsyncData>
  );
}
