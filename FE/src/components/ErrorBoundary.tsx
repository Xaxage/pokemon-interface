import { Component, type ErrorInfo, type ReactNode } from 'react';

interface ErrorBoundaryProps {
  children: ReactNode;
}

interface ErrorBoundaryState {
  error: Error | null;
}

/**
 * Catches a rendering error in the current page, so one broken view shows a message instead of blanking the
 * whole app. Layout keys it by route, so navigating elsewhere starts afresh.
 */
export class ErrorBoundary extends Component<ErrorBoundaryProps, ErrorBoundaryState> {
  state: ErrorBoundaryState = { error: null };

  static getDerivedStateFromError(error: Error): ErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Page failed to render', error, info.componentStack);
  }

  render() {
    if (!this.state.error) return this.props.children;
    return (
      <div className="status status--error" role="alert">
        <p>Something went wrong showing this page.</p>
        <button type="button" className="button button--small" onClick={() => this.setState({ error: null })}>
          Try again
        </button>
      </div>
    );
  }
}
