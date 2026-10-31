import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { Alert } from '../components/AuthLayout';

export default function Account() {
  const { user } = useAuth();
  const [form, setForm] = useState({ currentPassword: '', newPassword: '', confirm: '' });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');
    if (form.newPassword.length < 6) return setError('Das neue Passwort muss mindestens 6 Zeichen lang sein.');
    if (form.newPassword !== form.confirm) return setError('Die Passwörter stimmen nicht überein.');
    setBusy(true);
    try {
      const res = await api('/auth/change-password', {
        method: 'POST',
        body: { currentPassword: form.currentPassword, newPassword: form.newPassword },
      });
      setSuccess(res.message);
      setForm({ currentPassword: '', newPassword: '', confirm: '' });
    } catch (err) {
      setError(err.message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <main className="page narrow">
      <h1>Konto</h1>
      <div className="card">
        <dl className="details">
          <dt>Name</dt><dd>{user.fullName}</dd>
          <dt>E-Mail</dt><dd>{user.email}</dd>
          <dt>Rolle</dt><dd>{user.role === 'ADMIN' ? 'Administrator' : 'Benutzer'}</dd>
        </dl>
      </div>
      <div className="card">
        <h2>Passwort ändern</h2>
        <Alert>{error}</Alert>
        <Alert type="success">{success}</Alert>
        <form onSubmit={handleSubmit} className="form">
          <input type="password" required placeholder="Aktuelles Passwort" value={form.currentPassword} onChange={set('currentPassword')} />
          <input type="password" required placeholder="Neues Passwort" value={form.newPassword} onChange={set('newPassword')} />
          <input type="password" required placeholder="Neues Passwort bestätigen" value={form.confirm} onChange={set('confirm')} />
          <button className="btn btn-red" disabled={busy}>Passwort ändern</button>
        </form>
      </div>
    </main>
  );
}
