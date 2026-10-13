import { PAGE_SIZES } from '../hooks/usePagination';

interface PaginationProps {
  page: number;
  size: number;
  totalPages: number;
  count: number;
  busy?: boolean;
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
}

export function Pagination({ page, size, totalPages, count, busy, onPageChange, onSizeChange }: PaginationProps) {
  const last = Math.max(totalPages - 1, 0);
  const from = count === 0 ? 0 : page * size + 1;
  const to = Math.min((page + 1) * size, count);

  return (
    <div className="pagination" aria-busy={busy}>
      <p className="pagination__summary">
        {count === 0 ? 'No results' : `${from}–${to} of ${count.toLocaleString()}`}
      </p>

      <div className="pagination__controls">
        <button type="button" className="button button--ghost button--small" disabled={page === 0} onClick={() => onPageChange(0)} aria-label="First page">
          «
        </button>
        <button type="button" className="button button--ghost button--small" disabled={page === 0} onClick={() => onPageChange(page - 1)}>
          ‹ Prev
        </button>
        <span className="pagination__position">
          Page {Math.min(page + 1, Math.max(totalPages, 1))} / {Math.max(totalPages, 1)}
        </span>
        <button type="button" className="button button--ghost button--small" disabled={page >= last} onClick={() => onPageChange(page + 1)}>
          Next ›
        </button>
        <button type="button" className="button button--ghost button--small" disabled={page >= last} onClick={() => onPageChange(last)} aria-label="Last page">
          »
        </button>
      </div>

      <label className="pagination__size">
        Per page
        <select value={size} onChange={(event) => onSizeChange(Number(event.target.value))}>
          {PAGE_SIZES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>
      </label>
    </div>
  );
}
