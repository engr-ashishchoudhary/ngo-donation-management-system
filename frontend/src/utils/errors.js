// Turns any Axios/network error into a safe, user-facing message.
// The backend error body is { status, error, message, timestamp, path }.
export const getErrorMessage = (error, fallback = 'Something went wrong. Please try again.') => {
  if (!error) return fallback;

  if (error.response) {
    const { status, data } = error.response;
    const serverMessage = typeof data?.message === 'string' ? data.message : null;

    switch (status) {
      case 400:
        return serverMessage || 'Please check the form — some values are invalid.';
      case 401:
        return serverMessage || 'Your session is invalid or has expired. Please sign in again.';
      case 403:
        return 'Permission denied. You do not have access to this action.';
      case 404:
        return serverMessage || 'The requested resource was not found.';
      case 409:
        return serverMessage || 'This record already exists or conflicts with existing data.';
      case 422:
        return serverMessage || 'The request could not be processed.';
      default:
        if (status >= 500) return 'A server error occurred. Please try again later.';
        return serverMessage || fallback;
    }
  }

  if (error.request) {
    return 'Unable to reach the server. Check that the backend is running and try again.';
  }

  return error.message || fallback;
};

export const getStatusCode = (error) => error?.response?.status ?? null;
