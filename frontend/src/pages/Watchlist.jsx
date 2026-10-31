import { useCallback, useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import VideoCard from '../components/VideoCard';
import VideoModal from '../components/VideoModal';
import { toggleWatchlist } from '../useWatchlistToggle';

export default function Watchlist() {
  const [videos, setVideos] = useState([]);
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/videos/watchlist?size=100')
      .then((page) => setVideos(page.content))
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  const handleToggle = useCallback(async (video) => {
    try {
      const updated = await toggleWatchlist(video);
      if (!updated.inWatchList) {
        setVideos((list) => list.filter((v) => v.id !== updated.id));
        setSelected(null);
      }
    } catch (err) {
      setError(err.message);
    }
  }, []);

  return (
    <main className="page">
      <h1>Meine Liste</h1>
      {error && <div className="alert alert-error">{error}</div>}
      {loading && <div className="spinner" />}
      {!loading && videos.length === 0 && (
        <p className="muted">Ihre Liste ist leer. Fügen Sie Filme über <b>+</b> auf der <Link to="/browse">Startseite</Link> hinzu.</p>
      )}
      <div className="grid">
        {videos.map((v) => <VideoCard key={v.id} video={v} onToggleWatchlist={handleToggle} onOpen={setSelected} />)}
      </div>
      {selected && <VideoModal video={selected} onClose={() => setSelected(null)} onToggleWatchlist={handleToggle} />}
    </main>
  );
}
