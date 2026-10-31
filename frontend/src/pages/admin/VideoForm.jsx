import { useState } from 'react';
import { api, uploadFile } from '../../api';
import { Alert } from '../../components/AuthLayout';

const EMPTY = { title: '', description: '', year: new Date().getFullYear(), rating: 'PG-13', duration: '', categories: '', published: true };
const RATINGS = ['G', 'PG', 'PG-13', 'R', 'NC-17'];

// Upload-Kennungen stehen am Ende der relativen URL (/api/files/video/<uuid>)
const uuidOf = (url) => (url ? url.substring(url.lastIndexOf('/') + 1) : '');

export default function VideoForm({ video, onClose, onSaved }) {
  const [form, setForm] = useState(video
    ? { ...EMPTY, ...video, duration: video.duration ?? '', year: video.year ?? '', categories: (video.categories || []).join(', ') }
    : EMPTY);
  const [src, setSrc] = useState(uuidOf(video?.src));
  const [poster, setPoster] = useState(uuidOf(video?.poster));
  const [progress, setProgress] = useState({ video: null, image: null });
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.type === 'checkbox' ? e.target.checked : e.target.value });

  const handleUpload = (kind) => async (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setError('');
    setProgress((p) => ({ ...p, [kind]: 0 }));
    try {
      const uuid = await uploadFile(kind, file, (pct) => setProgress((p) => ({ ...p, [kind]: pct })));
      if (kind === 'video') {
        setSrc(uuid);
        // Laufzeit aus der Datei uebernehmen, falls noch keine eingetragen ist
        const probe = document.createElement('video');
        probe.preload = 'metadata';
        probe.onloadedmetadata = () => {
          if (Number.isFinite(probe.duration) && probe.duration > 0) {
            setForm((f) => (f.duration ? f : { ...f, duration: Math.max(1, Math.round(probe.duration / 60)) }));
          }
          URL.revokeObjectURL(probe.src);
        };
        probe.src = URL.createObjectURL(file);
      } else {
        setPoster(uuid);
      }
    } catch (err) {
      setError(err.message);
    } finally {
      setProgress((p) => ({ ...p, [kind]: null }));
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setBusy(true);
    setError('');
    const body = {
      title: form.title.trim(),
      description: form.description,
      year: form.year ? Number(form.year) : null,
      rating: form.rating,
      duration: form.duration ? Number(form.duration) : null,
      categories: form.categories.split(',').map((c) => c.trim()).filter(Boolean),
      published: form.published,
      src,
      poster,
    };
    try {
      if (video) await api(`/videos/admin/${video.id}`, { method: 'PUT', body });
      else await api('/videos/admin', { method: 'POST', body });
      onSaved(video ? 'Video gespeichert.' : 'Video angelegt.');
    } catch (err) {
      setError(err.message);
      setBusy(false);
    }
  };

  const uploading = progress.video !== null || progress.image !== null;

  return (
    <div className="modal-backdrop" onClick={onClose}>
      <form className="modal form-modal" onClick={(e) => e.stopPropagation()} onSubmit={handleSubmit}>
        <button type="button" className="modal-close" onClick={onClose} aria-label="Schließen">×</button>
        <h2>{video ? 'Video bearbeiten' : 'Neues Video'}</h2>
        <Alert>{error}</Alert>
        <div className="form">
          <label>Titel<input required value={form.title} onChange={set('title')} autoFocus /></label>
          <label>Beschreibung<textarea rows={3} maxLength={4000} value={form.description || ''} onChange={set('description')} /></label>
          <div className="form-row">
            <label>Jahr<input type="number" min="1900" max="2100" value={form.year} onChange={set('year')} /></label>
            <label>Altersfreigabe
              <select value={form.rating || ''} onChange={set('rating')}>
                {RATINGS.map((r) => <option key={r}>{r}</option>)}
              </select>
            </label>
            <label>Dauer (Min.)<input type="number" min="1" value={form.duration} onChange={set('duration')} /></label>
          </div>
          <label>Genres (durch Komma getrennt)<input value={form.categories} onChange={set('categories')} placeholder="Action, Drama" /></label>

          <div className="form-row">
            <label className="file-field">Videodatei (MP4/WebM)
              <input type="file" accept="video/*" onChange={handleUpload('video')} />
              {progress.video !== null && <progress max="100" value={progress.video} />}
              <small>{src ? `✓ ${src}` : 'Noch keine Datei'}</small>
            </label>
            <label className="file-field">Poster (JPG/PNG/WebP/SVG)
              <input type="file" accept="image/*" onChange={handleUpload('image')} />
              {progress.image !== null && <progress max="100" value={progress.image} />}
              {poster ? <img className="poster-preview" src={`/api/files/image/${poster}`} alt="Poster-Vorschau" /> : <small>Noch kein Poster</small>}
            </label>
          </div>

          <label className="checkbox"><input type="checkbox" checked={form.published} onChange={set('published')} /> Veröffentlicht (für Benutzer sichtbar)</label>
        </div>
        <div className="modal-actions">
          <button type="button" className="btn btn-grey" onClick={onClose}>Abbrechen</button>
          <button className="btn btn-red" disabled={busy || uploading}>{uploading ? 'Upload läuft …' : 'Speichern'}</button>
        </div>
      </form>
    </div>
  );
}
