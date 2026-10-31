import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';

export default function Landing() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [busy, setBusy] = useState(false);

  // Wie im Original: E-Mail eingeben -> vorhandenes Konto: Login, sonst Registrierung
  const handleSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      const res = await api(`/auth/validate-email?email=${encodeURIComponent(email)}`);
      navigate(res.exists ? '/login' : '/signup', { state: { email } });
    } catch {
      navigate('/signup', { state: { email } });
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="landing">
      <div className="landing-content">
        <h1>Unbegrenzt Filme, Serien und mehr</h1>
        <p className="landing-sub">Jederzeit ansehen. Überall. Jetzt kostenlos registrieren.</p>
        <p>Bereit? Geben Sie Ihre E-Mail-Adresse ein, um Ihr Konto zu erstellen oder sich anzumelden.</p>
        <form className="landing-form" onSubmit={handleSubmit}>
          <input type="email" required placeholder="E-Mail-Adresse" value={email} onChange={(e) => setEmail(e.target.value)} />
          <button className="btn btn-red btn-lg" disabled={busy}>Loslegen ›</button>
        </form>
      </div>
    </div>
  );
}
