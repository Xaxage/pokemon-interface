import { useCallback, useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';

export const PAGE_SIZES = [12, 24, 48, 96] as const;

/** Keeps the page and page size in the URL, so they survive reloads and can be shared or bookmarked. */
export function usePagination(defaultSize: number = PAGE_SIZES[1]) {
  const [params, setParams] = useSearchParams();
  const page = clamp(Number(params.get('page') ?? 0), 0, Number.MAX_SAFE_INTEGER);
  const size = clamp(Number(params.get('size') ?? defaultSize), 1, 100);

  const update = useCallback(
    (next: { page?: number; size?: number }) => {
      setParams((current) => {
        const updated = new URLSearchParams(current);
        if (next.page !== undefined) updated.set('page', String(next.page));
        if (next.size !== undefined) updated.set('size', String(next.size));
        return updated;
      });
      // A new page starts at the top. Done here, on the reader's action, rather than in an effect on mount.
      window.scrollTo({ top: 0 });
    },
    [setParams],
  );

  // Stable identities, so effects that depend on them don't re-run on every render.
  const setPage = useCallback((value: number) => update({ page: value }), [update]);
  const setSize = useCallback((value: number) => update({ page: 0, size: value }), [update]);

  return useMemo(() => ({ page, size, setPage, setSize }), [page, size, setPage, setSize]);
}

function clamp(value: number, min: number, max: number) {
  return Number.isFinite(value) ? Math.min(Math.max(Math.trunc(value), min), max) : min;
}
