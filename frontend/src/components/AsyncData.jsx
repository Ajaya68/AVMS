import { useState, useEffect, useCallback } from 'react';
import { ApiError } from '../services/api';

export default function AsyncData({ load, render, children, deps = [] }) {
  const [state, setState] = useState({ phase: 'loading' });
  const [runId, setRunId] = useState(0);

  const reload = useCallback(() => setRunId((n) => n + 1), []);

  useEffect(() => {
    let active = true;
    setState({ phase: 'loading' });
    (async () => {
      try {
        const data = await load();
        if (!active) return;
        const items = Array.isArray(data) ? data : data && data.items ? data.items : data;
        const isEmpty = Array.isArray(items) ? items.length === 0 : !data;
        setState({ phase: isEmpty ? 'empty' : 'success', data });
      } catch (error) {
        if (!active) return;
        if (error instanceof ApiError) {
          setState({ phase: error.state, error });
        } else {
          setState({ phase: 'server', error });
        }
      }
    })();
    return () => {
      active = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, deps.concat([runId]));

  if (state.phase === 'loading') {
    return (
      <div className="av-glass p-4" aria-busy="true" aria-label="Loading">
        <div className="d-flex align-items-center gap-3 mb-3">
          <div className="av-spinner" />
          <div>
            <div className="fw-bold">Loading fresh data…</div>
            <div className="text-muted small">Fetching from your farm workspace</div>
          </div>
        </div>
        {[100, 92, 96].map((w, i) => (
          <div key={i} className="av-skeleton mb-2" style={{ height: 18, width: `${w}%` }} />
        ))}
      </div>
    );
  }
  if (state.phase === 'empty') {
    return (
      <div className="av-glass p-5 text-center av-pop-in">
        <div style={{ fontSize: '3rem' }}>🪴</div>
        <h2 className="h6 fw-bold mt-2">Nothing here yet</h2>
        <p className="text-muted small mb-3">No records found. Try a different scope or add new data.</p>
        <button className="btn btn-sm av-gradient-btn av-btn-press" onClick={reload}>↻ Retry</button>
      </div>
    );
  }
  const alertBox = (kind, icon, title, hint) => (
    <div className={`av-glass p-4 av-fade-alert border-${kind}`} role="alert">
      <div className="d-flex gap-3 align-items-start">
        <span style={{ fontSize: 26 }}>{icon}</span>
        <div className="flex-grow-1">
          <div className="fw-bold">{title}</div>
          <div className="text-muted small">{hint}</div>
        </div>
        <button className="btn btn-sm btn-outline-secondary av-btn-press" onClick={reload}>↻ Retry</button>
      </div>
    </div>
  );
  if (state.phase === 'network') return alertBox('danger', '📡', 'Network error', 'Backend unreachable. Check connection and retry.');
  if (state.phase === 'timeout') return alertBox('warning', '⏳', 'Request timed out', 'The server took too long. Please try again.');
  if (state.phase === 'forbidden') return alertBox('warning', '🔒', 'Access denied', 'You lack permission for this business unit.');
  if (state.phase === 'validation') return alertBox('danger', '⚠️', 'Validation failed', state.error?.message || 'Check your input.');
  if (state.phase === 'server') {
    return alertBox('danger', '💥', 'Server error', state.error?.message || 'Something went wrong.');
  }
  if (typeof render === 'function') {
    return render(state.data);
  }
  return children(state.data, reload);
}

export { default as api } from '../services/api';
