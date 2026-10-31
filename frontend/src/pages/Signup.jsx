import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { api } from '../api';
import AuthLayout, { Alert } from '../components/AuthLayout';

export default function Signup() {
  const location = useLocation();
  const [form, setForm] = useState({ fullName: '', email: location.state?.email || '', password: '', confirm: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    if (form.password.length < 6) return setError('Das Passwort muss mindestens 6 Zeichen lang sein.');
    if (form.password !== form.confirm) return setError('Die Passwörter stimmen nicht überein.');
    setBusy(true);
    try {
      const res = await api('/auth/signup', {
        method: 'POST',
        body: { fullName: form.fullName, email: form.email, password: form.password },
      });
      setSuccess(res.message);
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  if (success) {
    return (
      <AuthLayout title="Fast geschafft!">
        <Alert type="success">{success}</Alert>
        <p className="muted">Wir haben eine E-Mail an <b>{form.email}</b> gesendet. Lokal finden Sie sie in Mailpit unter{' '}
          <a href="http://localhost:8027" target="_blank" rel="noreferrer">localhost:8027</a>.</p>
        <Link to="/login" state={{ email: form.email }} className="btn btn-red btn-block">Zur Anmeldung</Link>
      </AuthLayout>
    );
  }

  return (
    <AuthLayout title="Registrieren">
      <Alert>{error}</Alert>
      <form onSubmit={handleSubmit} className="form">
        <input required placeholder="Vollständiger Name" value={form.fullName} onChange={set('fullName')} autoFocus />
        <input type="email" required placeholder="E-Mail-Adresse" value={form.email} onChange={set('email')} />
        <input type="password" required placeholder="Passwort (mind. 6 Zeichen)" value={form.password} onChange={set('password')} />
        <input type="password" required placeholder="Passwort bestätigen" value={form.confirm} onChange={set('confirm')} />
        <button className="btn btn-red btn-block" disabled={busy}>{busy ? 'Wird registriert …' : 'Konto erstellen'}</button>
      </form>
      <div className="auth-links"><span>Schon registriert? <Link to="/login">Anmelden</Link></span></div>
    </AuthLayout>
  );
}
