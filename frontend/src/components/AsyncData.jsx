import { useState, useEffect } from 'react';
import api, { ApiError } from '../services/api';

/**
 * Shared async-render states for every screen. Drives the mandated states:
 * loading, success, empty, validation, forbidden, server, network, timeout.
 */
export default function AsyncData({ load, render }) {
  const [state, setState] = useState({ phase: 'loading' });

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
  }, []);

  if (state.phase === 'loading') {
    return <p className="text-center text-muted py-4">Loading…</p>;
  }
  if (state.phase === 'empty') {
    return <p className="text-center text-muted py-4">No records found.</p>;
  }
  if (state.phase === 'network') {
    return <p className="alert alert-danger">Network error: backend unreachable.</p>;
  }
  if (state.phase === 'timeout') {
    return <p className="alert alert-warning">The request timed out. Try again.</p>;
  }
  if (state.phase === 'forbidden') {
    return <p className="alert alert-warning">Access denied for this business unit.</p>;
  }
  if (state.phase === 'validation') {
    return <p className="alert alert-danger">Validation failed.</p>;
  }
  if (state.phase === 'server') {
    return (
      <p className="alert alert-danger">
        {state.error && state.error.message ? state.error.message : 'Server error.'}
      </p>
    );
  }
  return render(state.data);
}

export { api };