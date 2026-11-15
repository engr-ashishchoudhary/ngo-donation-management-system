import { useCallback, useEffect, useRef, useState } from 'react';
import { getErrorMessage } from '../utils/errors';

// Runs an async loader on mount (and whenever `deps` change) and exposes
// { data, loading, error, reload }.
export default function useAsync(loader, deps = [], { initialData = null, enabled = true } = {}) {
  const [data, setData] = useState(initialData);
  const [loading, setLoading] = useState(enabled);
  const [error, setError] = useState(null);
  const requestId = useRef(0);
  const loaderRef = useRef(loader);
  loaderRef.current = loader;

  const run = useCallback(async () => {
    const id = ++requestId.current;
    setLoading(true);
    setError(null);
    try {
      const result = await loaderRef.current();
      if (id === requestId.current) setData(result);
    } catch (err) {
      if (id === requestId.current) setError(getErrorMessage(err));
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (enabled) run();
    else setLoading(false);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled, ...deps]);

  return { data, loading, error, reload: run, setData };
}
