import { useEffect, useState } from 'react';
import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';

export default function Navbar() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const handleLogout = () => {
    logout();
    setMenuOpen(false);
    navigate('/login');
  };

  return (
    <header className={`navbar ${scrolled || isAdmin ? 'navbar-solid' : ''}`}>
      <Link to={user ? (isAdmin ? '/admin/videos' : '/browse') : '/'} className="logo">NETFLIX<span>CLONE</span></Link>
      {user && (
        <nav className="nav-links">
          {isAdmin ? (
            <>
              <NavLink to="/admin/videos">Videos</NavLink>
              <NavLink to="/admin/users">Benutzer</NavLink>
            </>
          ) : (
            <>
              <NavLink to="/browse">Startseite</NavLink>
              <NavLink to="/watchlist">Meine Liste</NavLink>
            </>
          )}
        </nav>
      )}
      <div className="nav-right">
        {user ? (
          <div className="profile">
            <button className="avatar" onClick={() => setMenuOpen((o) => !o)} aria-label="Profilmenü">
              {(user.fullName || user.email).charAt(0).toUpperCase()}
            </button>
            {menuOpen && (
              <div className="profile-menu" onMouseLeave={() => setMenuOpen(false)}>
                <div className="profile-name">{user.fullName}<small>{user.email}</small></div>
                <Link to="/account" onClick={() => setMenuOpen(false)}>Konto &amp; Passwort</Link>
                <button onClick={handleLogout}>Abmelden</button>
              </div>
            )}
          </div>
        ) : (
          <Link to="/login" className="btn btn-red btn-sm">Anmelden</Link>
        )}
      </div>
    </header>
  );
}
