import { useCallback, useEffect, useState } from 'react';
import { api } from '../../api';
import Pagination from '../../components/Pagination';
import VideoForm from './VideoForm';

export default function AdminVideos() {
  const [data, setData] = useState({ content: [], totalPages: 0, totalElements: 0 });
  const [stats, setStats] = useState(null);
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [editing, setEditing] = useState(undefined); // undefined = zu, null = neu, Objekt = bearbeiten
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const load = useCallback(() => {
    api(`/videos/admin?page=${page}&size=10${query ? `&search=${encodeURIComponent(query)}` : ''}`)
      .then(setData)
      .catch((err) => setError(err.message));
    api('/videos/admin/stats').then(setStats).catch(() => {});
  }, [page, query]);

  useEffect(load, [load]);

  useEffect(() => {
    const t = setTimeout(() => { setQuery(search.trim()); setPage(0); }, 300);
    return () => clearTimeout(t);
  }, [search]);

  const flash = (text) => { setMessage(text); setError(''); setTimeout(() => setMessage(''), 3000); };

  const togglePublish = async (video) => {
    try {
      await api(`/videos/admin/${video.id}/publish?value=${!video.published}`, { method: 'PATCH' });
      flash(video.published ? `„${video.title}“ ist nicht mehr veröffentlicht.` : `„${video.title}“ wurde veröffentlicht.`);
      load();
    } catch (err) { setError(err.message); }
  };

  const remove = async (video) => {
    if (!window.confirm(`„${video.title}“ wirklich löschen? Video- und Posterdatei werden ebenfalls gelöscht.`)) return;
    try {
      await api(`/videos/admin/${video.id}`, { method: 'DELETE' });
      flash(`„${video.title}“ wurde gelöscht.`);
      if (data.content.length === 1 && page > 0) setPage(page - 1); else load();
    } catch (err) { setError(err.message); }
  };

  const hours = stats ? Math.floor(stats.totalDuration / 60) : 0;

  return (
    <main className="page admin">
      <div className="page-header">
        <h1>Videos verwalten</h1>
        <button className="btn btn-red" onClick={() => setEditing(null)}>+ Neues Video</button>
      </div>

      {stats && (
        <div className="stats">
          <div className="stat"><span>{stats.totalVideos}</span>Videos gesamt</div>
          <div className="stat"><span>{stats.publishedVideos}</span>Veröffentlicht</div>
          <div className="stat"><span>{stats.totalVideos - stats.publishedVideos}</span>Entwürfe</div>
          <div className="stat"><span>{hours} Std. {stats.totalDuration % 60} Min.</span>Gesamtlaufzeit</div>
        </div>
      )}

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <input type="search" className="search" placeholder="Videos suchen …" value={search} onChange={(e) => setSearch(e.target.value)} aria-label="Videos suchen" />

      <div className="table-wrap">
        <table className="table">
          <thead>
            <tr><th>Poster</th><th>Titel</th><th>Jahr</th><th>Genres</th><th>Datei</th><th>Status</th><th className="right">Aktionen</th></tr>
          </thead>
          <tbody>
            {data.content.map((v) => (
              <tr key={v.id}>
                <td>{v.poster ? <img className="thumb" src={v.poster} alt="" /> : <div className="thumb thumb-empty" />}</td>
                <td><b>{v.title}</b><div className="muted small">{v.rating} · {v.duration ? `${v.duration} Min.` : '–'}</div></td>
                <td>{v.year}</td>
                <td>{v.categories?.join(', ')}</td>
                <td>{v.src ? <span className="pill pill-green">Video ✓</span> : <span className="pill">kein Video</span>}</td>
                <td>
                  <button className={`pill pill-button ${v.published ? 'pill-green' : ''}`} onClick={() => togglePublish(v)}
                          title="Klicken zum Umschalten">
                    {v.published ? 'Veröffentlicht' : 'Entwurf'}
                  </button>
                </td>
                <td className="right nowrap">
                  <button className="btn btn-grey btn-sm" onClick={() => setEditing(v)}>Bearbeiten</button>{' '}
                  <button className="btn btn-danger btn-sm" onClick={() => remove(v)}>Löschen</button>
                </td>
              </tr>
            ))}
            {data.content.length === 0 && <tr><td colSpan={7} className="muted center">Keine Videos gefunden.</td></tr>}
          </tbody>
        </table>
      </div>
      <Pagination page={page} totalPages={data.totalPages} onChange={setPage} />

      {editing !== undefined && (
        <VideoForm video={editing} onClose={() => setEditing(undefined)}
                   onSaved={(text) => { setEditing(undefined); flash(text); load(); }} />
      )}
    </main>
  );
}
