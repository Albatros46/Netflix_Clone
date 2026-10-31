import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import AuthLayout, { Alert } from '../components/AuthLayout';

export default function VerifyEmail() {
  const [params] = useSearchParams();
  const token = params.get('token');
  const [state, setState] = useState({ loading: true, ok: false, message: '' });
  const called = useRef(false);

  useEffect(() => {
    // StrictMode ruft Effekte doppelt auf - der Token ist nach dem ersten Aufruf verbraucht
    if (called.current) return;
    called.current = true;
    if (!token) {
      setState({ loading: false, ok: false, message: 'Der Bestätigungslink ist unvollständig.' });
      return;
    }
    api(`/auth/verify-email?token=${encodeURIComponent(token)}`)
      .then((res) => setState({ loading: false, ok: true, message: res.message }))
      .catch((err) => setState({ loading: false, ok: false, message: err.message }));
  }, [token]);

  return (
    <AuthLayout title="E-Mail bestätigen">
      {state.loading ? <div className="spinner" /> : <Alert type={state.ok ? 'success' : 'error'}>{state.message}</Alert>}
      {!state.loading && <Link to="/login" className="btn btn-red btn-block">Zur Anmeldung</Link>}
    </AuthLayout>
  );
}
