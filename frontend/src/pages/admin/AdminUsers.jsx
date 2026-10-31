import { useCallback, useEffect, useState } from 'react';
import { api } from '../../api';
import { useAuth } from '../../auth';
import { Alert } from '../../components/AuthLayout';
import Pagination from '../../components/Pagination';

const EMPTY = { fullName: '', email: '', password: '', role: 'USER' };

function UserForm({ user, onClose, onSaved }) {
  const [form, setForm] = useState(user ? { ...user, password: '' } : EMPTY);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    const body = { fullName: form.fullName, email: form.email, role: form.role, password: form.password || null };
    try {
      if (user) await api(`/users/${user.id}`, { method: 'PUT', body });
      else await api('/users/create-user', { method: 'POST', body });
      onSaved(user ? 'Benutzer gespeichert.' : 'Benutzer angelegt. Eine Bestätigungs-E-Mail wurde gesendet.');
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form className="modal form-modal small" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <button type="button" className="modal-close" onClick={onClose} aria-label="Schließen">×</button>
        <h2>{user ? 'Benutzer bearbeiten' : 'Neuer Benutzer'}</h2>
        <Alert>{error}</Alert>
        <div className="form">
          <label>Vollständiger Name<input required value={form.fullName} onChange={set('fullName')} autoFocus /></label>
          <label>E-Mail<input type="email" required value={form.email} onChange={set('email')} /></label>
          <label>{user ? 'Neues Passwort (leer = unverändert)' : 'Passwort'}
            <input type="password" required={!user} minLength={6} value={form.password} onChange={set('password')} />
          </label>
          <label>Rolle
            <select value={form.role} onChange={set('role')}>
              <option value="USER">Benutzer</option>
              <option value="ADMIN">Administrator</option>
            </select>
          </label>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-grey" onClick={onClose}>Abbrechen</button>
          <button className="btn btn-red" disabled={busy}>Speichern</button>
        </div>
      </form>
    </div>
  );
}

export default function AdminUsers() {
  const { user: me } = useAuth();
  const [data, setData] = useState({ content: [], totalPages: 0 });
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState(undefined);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api(`/users?page=${page}&size=10${query ? `&search=${encodeURIComponent(query)}` : ''}`)
      .then(setData)
      .catch((err) => setError(err.message));
  }, [page, query]);

  useEffect(load, [load]);
  useEffect(() => {
    const t = setTimeout(() => { setQuery(search.trim()); setPage(0); }, 300);
    return () => clearTimeout(t);
  }, [search]);

  const flash = (text) => { setMessage(text); setError(''); setTimeout(() => setMessage(''), 3000); };

  const toggleStatus = async (u) => {
    try {
      await api(`/users/${u.id}/toggle-status`, { method: 'PUT' });
      flash(u.active ? `${u.fullName} wurde deaktiviert.` : `${u.fullName} wurde aktiviert.`);
      load();
    } catch (err) { setError(err.message); }
  };

  const remove = async (u) => {
    if (!window.confirm(`Benutzer „${u.fullName}“ (${u.email}) wirklich löschen?`)) return;
    try {
      await api(`/users/${u.id}`, { method: 'DELETE' });
      flash(`${u.fullName} wurde gelöscht.`);
      load();
    } catch (err) { setError(err.message); }
  };

  return (
    <main className="page admin">
      <div className="page-header">
        <h1>Benutzer verwalten</h1>
        <button className="btn btn-red" onClick={() => setEditing(null)}>+ Neuer Benutzer</button>
      </div>
      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}
      <input type="search" className="search" placeholder="Name oder E-Mail suchen …" value={search} onChange={(e) => setSearch(e.target.value)} aria-label="Benutzer suchen" />

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Name</th><th>E-Mail</th><th>Rolle</th><th>E-Mail bestätigt</th><th>Status</th><th className="right">Aktionen</th></tr>
          </thead>
          <tbody>
            {data.content.map((u) => {
              const self = u.email === me.email;
              return (
                <tr key={u.id}>
                  <td><b>{u.fullName}</b>{self && <span className="muted small"> (Sie)</span>}</td>
                  <td>{u.email}</td>
                  <td>{u.role === 'ADMIN' ? <span className="pill pill-red">Admin</span> : <span className="pill">Benutzer</span>}</td>
                  <td>{u.emailVerified ? '✓' : <span className="muted">ausstehend</span>}</td>
                  <td>{u.active ? <span className="pill pill-green">Aktiv</span> : <span className="pill">Inaktiv</span>}</td>
                  <td className="right nowrap">
                    <button className="btn btn-grey btn-sm" onClick={() => setEditing(u)}>Bearbeiten</button>{' '}
                    <button className="btn btn-grey btn-sm" disabled={self} onClick={() => toggleStatus(u)}>
                      {u.active ? 'Deaktivieren' : 'Aktivieren'}
                    </button>{' '}
                    <button className="btn btn-danger btn-sm" disabled={self} onClick={() => remove(u)}>Löschen</button>
                  </td>
                </tr>
              );
            })}
            {data.content.length === 0 && <tr><td colSpan={6} className="muted center">Keine Benutzer gefunden.</td></tr>}
          </tbody>
        </table>
      </div>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />

      {editing !== undefined && (
        <UserForm user={editing} onClose={() => setEditing(undefined)}
                  onSaved={(text) => { setEditing(undefined); flash(text); load(); }} />
      )}
    </main>
  );
}
