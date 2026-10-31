import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import AuthLayout, { Alert } from '../components/AuthLayout';

export default function Login() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [email, setEmail] = useState(location.state?.email || '');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [info, setInfo] = useState(location.state?.info || '');
  const [busy, setBusy] = useState(false);
  const [unverified, setUnverified] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    setInfo('');
    setUnverified(false);
    try {
      const user = await login(email, password);
      const target = location.state?.from;
      navigate(target && target !== '/login' ? target : user.role === 'ADMIN' ? '/admin/videos' : '/browse', { replace: true });
    } catch (err) {
      setError(err.message);
      setUnverified(err.status === 403 && err.message.includes('bestätigen'));
    } finally {
      setBusy(false);
    }
  };

  const resend = async () => {
    try {
      const res = await api('/auth/resend-verification', { method: 'POST', body: { email } });
      setError('');
      setUnverified(false);
      setInfo(res.message);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <AuthLayout title="Anmelden">
      <Alert type="success">{info}</Alert>
      <Alert>{error}</Alert>
      {unverified && (
        <button type="button" className="link-button" onClick={resend}>Bestätigungs-E-Mail erneut senden</button>
      )}
      <form onSubmit={handleSubmit} className="form">
        <input type="email" required placeholder="E-Mail-Adresse" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus />
        <input type="password" required placeholder="Passwort" value={password} onChange={(e) => setPassword(e.target.value)} />
        <button className="btn btn-red btn-block" disabled={busy}>{busy ? 'Anmelden …' : 'Anmelden'}</button>
      </form>
      <div className="auth-links">
        <Link to="/forgot-password">Passwort vergessen?</Link>
        <span>Neu hier? <Link to="/signup">Jetzt registrieren</Link></span>
      </div>
      <p className="demo-hint">Demo: <b>admin@netflix.local</b> / admin123 · <b>user@netflix.local</b> / user123</p>
    </AuthLayout>
  );
}
