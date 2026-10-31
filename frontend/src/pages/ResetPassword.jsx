import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { api } from '../api';
import AuthLayout, { Alert } from '../components/AuthLayout';

export default function ResetPassword() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (password.length < 6) return setError('Das Passwort muss mindestens 6 Zeichen lang sein.');
    if (password !== confirm) return setError('Die Passwörter stimmen nicht überein.');
    setBusy(true);
    try {
      const res = await api('/auth/reset-password', { method: 'POST', body: { token: params.get('token'), newPassword: password } });
      navigate('/login', { state: { info: res.message } });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="Neues Passwort">
      <Alert>{error}</Alert>
      <form onSubmit={handleSubmit} className="form">
        <input type="password" required placeholder="Neues Passwort" value={password} onChange={(e) => setPassword(e.target.value)} autoFocus />
        <input type="password" required placeholder="Neues Passwort bestätigen" value={confirm} onChange={(e) => setConfirm(e.target.value)} />
        <button className="btn btn-red btn-block" disabled={busy}>Passwort speichern</button>
      </form>
      <div className="auth-links"><Link to="/login">Zurück zur Anmeldung</Link></div>
    </AuthLayout>
  );
}
