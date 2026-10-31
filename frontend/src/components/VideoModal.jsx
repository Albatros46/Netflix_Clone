import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { formatDuration } from '../api';

export default function VideoModal({ video, onClose, onToggleWatchlist }) {
  const navigate = useNavigate();

  useEffect(() => {
    const onKey = (e) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  if (!video) return null;
  return (
    <div className="modal-backdrop" onClick={onClose}>
      <div className="modal video-modal" onClick={(e) => e.stopPropagation()} role="dialog" aria-label={video.title}>
        <button className="modal-close" onClick={onClose} aria-label="Schließen">×</button>
        <div className="video-modal-body">
          {video.poster && <img src={video.poster} alt="" className="video-modal-poster" />}
          <div>
            <h2>{video.title}</h2>
            <div className="card-meta">
              {video.rating && <span className="badge">{video.rating}</span>}
              <span>{video.year}</span>
              <span>{formatDuration(video.duration)}</span>
            </div>
            <p>{video.description}</p>
            <p className="muted">Genres: {video.categories?.join(', ') || '–'}</p>
            <div className="hero-buttons">
              <button className="btn btn-white" onClick={() => navigate(`/watch/${video.id}`)}>▶ Abspielen</button>
              <button className="btn btn-grey" onClick={() => onToggleWatchlist(video)}>
                {video.inWatchList ? '✓ In Meiner Liste' : '+ Meine Liste'}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
