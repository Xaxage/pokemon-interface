import { Link } from 'react-router-dom';
import { EmptyState } from '../components/Status';

export function NotFoundPage() {
  return (
    <EmptyState title="Page not found">
      <p className="muted">
        <Link to="/">Back to browsing</Link>
      </p>
    </EmptyState>
  );
}
