import { useState, useEffect } from 'react';
import AsyncData from '../../components/AsyncData';
import api, { ApiError } from '../../services/api';
import { useAuth } from '../auth/AuthContext';

export default function UsersPage() {
  const { hasPermission } = useAuth();
  const canManage = hasPermission('USER.MANAGE');
  const [showCreate, setShowCreate] = useState(false);
  const [flash, setFlash] = useState('');
  const [query, setQuery] = useState('');

  const notify = (msg) => {
    setFlash(msg);
    setTimeout(() => setFlash(''), 4000);
  };

  return (
    <AsyncData load={() => api.get('/api/v1/users', { params: { page: 1, page_size: 50 } })} deps={[]}>
      {({ data, reload }) => {
        const users = (data.users || []).filter((u) =>
          !query || u.username.toLowerCase().includes(query.toLowerCase())
          || (u.full_name || '').toLowerCase().includes(query.toLowerCase()),
        );
        return (
          <div className="av-pop-in">
            <div className="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
              <div>
                <h1 className="fw-extrabold mb-0" style={{ letterSpacing: '-0.02em' }}>
                  Team <span className="av-gradient-text">({(data.users || []).length})</span>
                </h1>
                <p className="text-muted small mb-0">Search, invite and manage access</p>
              </div>
              {canManage && (
                <button className="btn av-gradient-btn btn-sm av-btn-press" onClick={() => setShowCreate(true)}>
                  ＋ New user
                </button>
              )}
            </div>

            <div className="av-glass p-2 px-3 mb-3 d-flex align-items-center gap-2">
              <span>🔍</span>
              <input
                className="form-control border-0 bg-transparent"
                placeholder="Search username or name…"
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                style={{ boxShadow: 'none' }}
              />
              {query && <button className="btn btn-sm btn-light" onClick={() => setQuery('')}>✕</button>}
            </div>

            {flash && <div className="alert alert-success py-2 small av-fade-alert">{flash}</div>}
            {showCreate && canManage && (
              <CreateUserForm
                onClose={() => setShowCreate(false)}
                onCreated={() => {
                  setShowCreate(false);
                  notify('✅ User created.');
                  reload();
                }}
              />
            )}
            <div className="av-glass av-table-wrap">
              <div className="table-responsive">
                <table className="table table-hover align-middle mb-0">
                  <thead>
                    <tr>
                      <th>User</th>
                      <th className="av-hide-sm">Email</th>
                      <th>Status</th>
                      <th className="av-hide-sm">Last login</th>
                    </tr>
                  </thead>
                  <tbody>
                    {users.map((u) => (
                      <tr key={u.user_id}>
                        <td>
                          <div className="d-flex align-items-center gap-2">
                            <span
                              className="rounded-circle text-white fw-bold d-grid flex-shrink-0"
                              style={{ width: 36, height: 36, placeItems: 'center', display: 'grid', background: 'linear-gradient(135deg,#8b5cf6,#06b6d4)' }}
                            >
                              {u.username.slice(0, 1).toUpperCase()}
                            </span>
                            <span>
                              <span className="d-block fw-bold">{u.username}</span>
                              <span className="d-block text-muted small">{u.full_name || '—'}</span>
                            </span>
                          </div>
                        </td>
                        <td className="av-hide-sm small">{u.email || '—'}</td>
                        <td>
                          <span className={'badge rounded-pill ' + (u.status === 'ACTIVE' ? 'text-bg-success' : 'text-bg-secondary')}>
                            ● {u.status}
                          </span>
                        </td>
                        <td className="av-hide-sm small text-muted">{u.last_login_at || 'Never'}</td>
                      </tr>
                    ))}
                    {users.length === 0 && (
                      <tr><td colSpan={4} className="text-center text-muted py-4">No users match “{query}”.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        );
      }}
    </AsyncData>
  );
}

function CreateUserForm({ onClose, onCreated }) {
  const [form, setForm] = useState({
    username: '', fullName: '', email: '', password: '', roleId: '', businessUnitIds: [],
  });
  const [errorMessage, setErrorMessage] = useState('');
  const [processing, setProcessing] = useState(false);
  const [roles, setRoles] = useState([]);
  const [businessUnits, setBusinessUnits] = useState([]);

  useEffect(() => {
    (async () => {
      try {
        const [roleRes, buRes] = await Promise.all([
          api.get('/api/v1/roles', { params: { page: 1, page_size: 100 } }),
          api.get('/api/v1/business-units', { params: { page: 1, page_size: 100 } }),
        ]);
        setRoles(roleRes.data.roles || []);
        setBusinessUnits(buRes.data.business_units || []);
      } catch (e) {
        setErrorMessage(e && e.message ? e.message : 'Failed to load form options.');
      }
    })();
  }, []);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setProcessing(true);
    setErrorMessage('');
    try {
      await api.post('/api/v1/users', {
        username: form.username,
        full_name: form.fullName || null,
        email: form.email || null,
        password: form.password,
        role_id: Number(form.roleId),
        business_unit_ids: form.businessUnitIds.map(Number),
      });
      onCreated();
    } catch (error) {
      if (error instanceof ApiError) {
        const field = error.fieldErrors && error.fieldErrors[0];
        setErrorMessage(field ? field.message : error.message);
      } else {
        setErrorMessage('Unable to reach the server.');
      }
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="av-glass mb-3 overflow-hidden av-pop-in">
      <div className="d-flex justify-content-between align-items-center p-3 border-bottom">
        <strong>✨ New user</strong>
        <button className="btn-close" onClick={onClose} aria-label="Close" />
      </div>
      <div className="p-3">
        {errorMessage && <div className="alert alert-danger py-2 small av-fade-alert">{errorMessage}</div>}
        <form onSubmit={submit}>
          <div className="row g-3">
            <div className="col-12 col-md-4">
              <label className="form-label fw-semibold">Username</label>
              <input className="form-control av-input" value={form.username} onChange={set('username')} required />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label fw-semibold">Full name</label>
              <input className="form-control av-input" value={form.fullName} onChange={set('fullName')} />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label fw-semibold">Email</label>
              <input type="email" className="form-control av-input" value={form.email} onChange={set('email')} />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label fw-semibold">Temporary password</label>
              <input type="password" className="form-control av-input" value={form.password} onChange={set('password')} required />
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label fw-semibold">Role</label>
              <select className="form-select av-input" value={form.roleId} onChange={set('roleId')} required>
                <option value="">— Select —</option>
                {roles.map((r) => (
                  <option key={r.role_id} value={r.role_id}>{r.role_name}</option>
                ))}
              </select>
            </div>
            <div className="col-12 col-md-4">
              <label className="form-label fw-semibold">Business units</label>
              <select
                multiple
                className="form-select av-input"
                value={form.businessUnitIds}
                onChange={(e) => setForm({
                  ...form,
                  businessUnitIds: Array.from(e.target.selectedOptions, (o) => o.value),
                })}
              >
                {businessUnits.map((bu) => (
                  <option key={bu.business_unit_id} value={bu.business_unit_id}>
                    {bu.business_unit_name}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div className="mt-3 d-flex gap-2">
            <button className="btn av-gradient-btn av-btn-press" disabled={processing}>
              {processing ? 'Creating…' : 'Create user'}
            </button>
            <button type="button" className="btn btn-light" onClick={onClose}>Cancel</button>
          </div>
        </form>
      </div>
    </div>
  );
}
