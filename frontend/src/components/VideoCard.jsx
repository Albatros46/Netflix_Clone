import { useNavigate } from 'react-router-dom';
import { formatDuration } from '../api';

export default function VideoCard({ video, onToggleWatchlist, onOpen }) {
  const navigate = useNavigate();
  return (
    <div className="video-card" onClick={() => onOpen?.(video)} role="button" tabIndex={0}
         onKeyDown={(e) => e.key === 'Enter' && onOpen?.(video)} aria-label={video.title}>
      {video.poster
        ? <img src={video.poster} alt="" loading="lazy" />
        : <div className="poster-placeholder">{video.title}</div>}
      <div className="video-card-overlay">
        <div className="card-actions">
          <button className="round-btn play" title="Abspielen" aria-label={`${video.title} abspielen`}
                  onClick={(e) => { e.stopPropagation(); navigate(`/watch/${video.id}`); }}>▶</button>
          <button className="round-btn" title={video.inWatchList ? 'Aus Meiner Liste entfernen' : 'Zu Meiner Liste hinzufügen'}
                  aria-label={video.inWatchList ? 'Aus Meiner Liste entfernen' : 'Zu Meiner Liste hinzufügen'}
                  onClick={(e) => { e.stopPropagation(); onToggleWatchlist(video); }}>
            {video.inWatchList ? '✓' : '+'}
          </button>
        </div>
        <div className="card-title">{video.title}</div>
        <div className="card-meta">
          {video.rating && <span className="badge">{video.rating}</span>}
          <span>{video.year}</span>
          <span>{formatDuration(video.duration)}</span>
        </div>
      </div>
    </div>
  );
}
