import type { ReactNode } from 'react';

export function Spinner({ label = 'Loading…' }: { label?: string }) {
  return (
    <div className="status" role="status">
      <span className="spinner" aria-hidden />
      <span>{label}</span>
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const message = error instanceof Error ? error.message : 'Something went wrong.';
  return (
    <div className="status status--error" role="alert">
      <p>{message}</p>
      {onRetry && (
        <button type="button" className="button button--small" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}

export function EmptyState({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="status status--empty">
      <h2>{title}</h2>
      {children}
    </div>
  );
}

export function SkeletonGrid({ count }: { count: number }) {
  return (
    <ul className="grid" aria-hidden>
      {Array.from({ length: count }, (_, index) => (
        <li key={index} className="card card--skeleton" />
      ))}
    </ul>
  );
}
