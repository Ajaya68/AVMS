// API client for the Jakarta Servlet backend.
// - attaches the CSRF token (read from the AV-XSRF cookie) to mutating requests
// - sends the active business-unit context header
// - normalizes all failures into `ApiError` so the UI can render the standard
//   states: loading, success, empty, validation, forbidden, server, network,
//   timeout.

const CSRF_COOKIE = 'AV-XSRF';
const CSRF_HEADER = 'X-XSRF-Token';
const BU_HEADER = 'X-Business-Unit-Id';

function readCookie(name) {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'));
  return match ? decodeURIComponent(match[1]) : null;
}

export class ApiError extends Error {
  constructor(message, { status, code, fieldErrors = [], network = false, timeout = false } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.network = network;
    this.timeout = timeout;
  }

  get state() {
    if (this.network) return 'network';
    if (this.timeout) return 'timeout';
    if (!this.status) return 'server';
    if (this.status === 422) return 'validation';
    if (this.status === 401) return 'unauthorized';
    if (this.status === 403) return 'forbidden';
    if (this.status !== 200 && this.status !== 201) return 'server';
    return 'success';
  }
}

async function request(path, {
  method = 'GET',
  body,
  params,
  businessUnitId,
  timeoutMs = 30000,
} = {}) {
  const url = new URL(path, window.location.origin);
  if (params) {
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined && value !== null && value !== '') {
        url.searchParams.set(key, value);
      }
    }
  }

  const headers = {};
  const isMutating = !['GET', 'HEAD', 'OPTIONS'].includes(method.toUpperCase());
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (businessUnitId) {
    headers[BU_HEADER] = String(businessUnitId);
  }
  const csrf = readCookie(CSRF_COOKIE);
  if (isMutating && csrf) {
    headers[CSRF_HEADER] = csrf;
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);

  let response;
  try {
    response = await fetch(url.toString(), {
      method,
      headers,
      credentials: 'same-origin',
      body: body === undefined ? undefined : JSON.stringify(body),
      signal: controller.signal,
    });
  } catch (error) {
    clearTimeout(timer);
    if (error.name === 'AbortError') {
      throw new ApiError(`Request timed out after ${timeoutMs}ms`, { timeout: true });
    }
    throw new ApiError('Network error: backend unreachable', { network: true });
  }
  clearTimeout(timer);

  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }

  const data = payload ? payload.data : null;
  if (!response.ok || payload === null || payload.success === false) {
    const err = payload && payload.error ? payload.error : {};
    throw new ApiError(err.message || `HTTP ${response.status}`, {
      status: response.status,
      code: err.code,
      fieldErrors: err.fieldErrors || [],
    });
  }
  return data;
}

const api = {
  get: (path, opts) => request(path, { ...opts, method: 'GET' }),
  post: (path, body, opts) => request(path, { ...opts, method: 'POST', body }),
  put: (path, body, opts) => request(path, { ...opts, method: 'PUT', body }),
  patch: (path, body, opts) => request(path, { ...opts, method: 'PATCH', body }),
  del: (path, opts) => request(path, { ...opts, method: 'DELETE' }),
};

export default api;