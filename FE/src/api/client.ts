import type { InvalidField, ProblemDetail } from './types';

/** Thrown for every non-2xx answer, carrying the backend's problem detail. */
export class ApiError extends Error {
  readonly status: number;
  readonly invalidFields: InvalidField[];

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title ?? `Request failed with status ${problem.status}`);
    this.name = 'ApiError';
    this.status = problem.status;
    this.invalidFields = problem.invalidFields ?? [];
  }
}

type TokenProvider = () => string | null;
type UnauthorizedListener = () => void;

let tokenProvider: TokenProvider = () => null;
let onUnauthorized: UnauthorizedListener = () => {};

/** Lets the auth layer supply the bearer token and hear about rejected tokens, without the client depending on React. */
export function configureClient(options: { token: TokenProvider; onUnauthorized: UnauthorizedListener }) {
  tokenProvider = options.token;
  onUnauthorized = options.onUnauthorized;
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  body?: unknown;
  query?: Record<string, string | number | undefined>;
  signal?: AbortSignal;
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', body, query, signal } = options;

  const url = new URL(`/api${path}`, window.location.origin);
  Object.entries(query ?? {}).forEach(([key, value]) => {
    if (value !== undefined) url.searchParams.set(key, String(value));
  });

  const headers: Record<string, string> = { Accept: 'application/json, application/problem+json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  const token = tokenProvider();
  if (token) headers.Authorization = `Bearer ${token}`;

  let response: Response;
  try {
    response = await fetch(url, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    });
  } catch (error) {
    if (error instanceof DOMException && error.name === 'AbortError') throw error;
    throw new ApiError({ status: 0, detail: 'Cannot reach the server. Is the backend running?' });
  }

  if (!response.ok) {
    const problem = await readProblem(response);
    if (response.status === 401 && token) onUnauthorized();
    throw new ApiError(problem);
  }

  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

async function readProblem(response: Response): Promise<ProblemDetail> {
  try {
    const problem = (await response.json()) as ProblemDetail;
    return { ...problem, status: response.status };
  } catch {
    return { status: response.status, detail: response.statusText || undefined };
  }
}
