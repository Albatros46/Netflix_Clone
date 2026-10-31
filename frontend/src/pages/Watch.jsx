import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { api } from '../api';

export default function Watch() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [video, setVideo] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api(`/videos/${id}`).then(setVideo).catch((err) => setError(err.message));
  }, [id]);

  return (
    <div className="watch">
      <button className="watch-back" onClick={() => navigate(-1)} aria-label="Zurück">← Zurück</button>
      {error && <div className="watch-message">{error}</div>}
      {video && (video.src ? (
        <>
          <video className="player" src={video.src} poster={video.poster || undefined} controls autoPlay />
          <div className="watch-title">{video.title}</div>
        </>
      ) : (
        <div className="watch-message">
          <h2>{video.title}</h2>
          <p>Für diesen Titel wurde noch keine Videodatei hochgeladen.</p>
          <p className="muted">Ein Administrator kann unter „Videos“ eine MP4/WebM-Datei hochladen.</p>
        </div>
      ))}
    </div>
  );
}
