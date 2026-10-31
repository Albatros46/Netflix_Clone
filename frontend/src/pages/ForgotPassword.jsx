import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import AuthLayout, { Alert } from '../components/AuthLayout';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busy, setBusy] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      const res = await api('/auth/forgot-password', { method: 'POST', body: { email } });
      setSuccess(res.message);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <AuthLayout title="Passwort vergessen">
      <Alert>{error}</Alert>
      <Alert type="success">{success}</Alert>
      {!success && (
        <form onSubmit={handleSubmit} className="form">
          <p className="muted">Geben Sie Ihre E-Mail-Adresse ein. Wir senden Ihnen einen Link zum Zurücksetzen.</p>
          <input type="email" required placeholder="E-Mail-Adresse" value={email} onChange={(e) => setEmail(e.target.value)} autoFocus />
          <button className="btn btn-red btn-block" disabled={busy}>Link senden</button>
        </form>
      )}
      <div className="auth-links"><Link to="/login">Zurück zur Anmeldung</Link></div>
    </AuthLayout>
  );
}
