import { useCallback, useEffect, useMemo, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api, formatDuration } from '../api';
import VideoCard from '../components/VideoCard';
import VideoModal from '../components/VideoModal';
import { toggleWatchlist } from '../useWatchlistToggle';

export default function Browse() {
  const navigate = useNavigate();
  const [videos, setVideos] = useState([]);
  const [featured, setFeatured] = useState([]);
  const [heroIndex, setHeroIndex] = useState(0);
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [selected, setSelected] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/videos/featured').then(setFeatured).catch(() => setFeatured([]));
  }, []);

  // Suche mit kurzer Verzoegerung, damit nicht bei jedem Tastendruck geladen wird
  useEffect(() => {
    const t = setTimeout(() => setQuery(search.trim()), 300);
    return () => clearTimeout(t);
  }, [search]);

  useEffect(() => {
    setLoading(true);
    api(`/videos/published?size=100${query ? `&search=${encodeURIComponent(query)}` : ''}`)
      .then((page) => { setVideos(page.content); setError(''); })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, [query]);

  useEffect(() => {
    if (featured.length < 2) return;
    const t = setInterval(() => setHeroIndex((i) => (i + 1) % featured.length), 8000);
    return () => clearInterval(t);
  }, [featured.length]);

  const handleToggle = useCallback(async (video) => {
    try {
      const updated = await toggleWatchlist(video);
      const replace = (list) => list.map((v) => (v.id === updated.id ? { ...v, inWatchList: updated.inWatchList } : v));
      setVideos(replace);
      setFeatured(replace);
      setSelected((s) => (s && s.id === updated.id ? { ...s, inWatchList: updated.inWatchList } : s));
    } catch (err) {
      setError(err.message);
    }
  }, []);

  // Zeilen nach Genre gruppieren
  const rows = useMemo(() => {
    const map = new Map();
    for (const v of videos) {
      for (const c of v.categories?.length ? v.categories : ['Weitere']) {
        if (!map.has(c)) map.set(c, []);
        map.get(c).push(v);
      }
    }
    return [...map.entries()].sort((a, b) => b[1].length - a[1].length);
  }, [videos]);

  const hero = featured[heroIndex];

  return (
    <main className="browse">
      {!query && hero && (
        <section className="hero" style={{ '--hero-img': `url("${hero.poster}")` }}>
          <div className="hero-content">
            <h1>{hero.title}</h1>
            <div className="card-meta">
              {hero.rating && <span className="badge">{hero.rating}</span>}
              <span>{hero.year}</span>
              <span>{formatDuration(hero.duration)}</span>
              <span>{hero.categories?.join(' · ')}</span>
            </div>
            <p>{hero.description}</p>
            <div className="hero-buttons">
              <button className="btn btn-white" onClick={() => navigate(`/watch/${hero.id}`)}>▶ Abspielen</button>
              <button className="btn btn-grey" onClick={() => setSelected(hero)}>ⓘ Weitere Infos</button>
            </div>
          </div>
          <img className="hero-poster" src={hero.poster} alt="" />
        </section>
      )}

      <div className={`browse-toolbar ${query || !hero ? 'no-hero' : ''}`}>
        <input type="search" className="search" placeholder="Titel, Beschreibung suchen …" value={search}
               onChange={(e) => setSearch(e.target.value)} aria-label="Filme suchen" />
      </div>

      {error && <div className="alert alert-error page-alert">{error}</div>}
      {loading && <div className="spinner" />}

      {!loading && query && (
        <section className="row">
          <h2>Suchergebnisse für „{query}“ ({videos.length})</h2>
          {videos.length === 0
            ? <p className="muted">Keine Filme gefunden.</p>
            : <div className="grid">{videos.map((v) => (
                <VideoCard key={v.id} video={v} onToggleWatchlist={handleToggle} onOpen={setSelected} />))}</div>}
        </section>
      )}

      {!loading && !query && rows.map(([category, list]) => (
        <section className="row" key={category}>
          <h2>{category}</h2>
          <div className="row-scroller">
            {list.map((v) => <VideoCard key={v.id} video={v} onToggleWatchlist={handleToggle} onOpen={setSelected} />)}
          </div>
        </section>
      ))}

      {!loading && !query && videos.length === 0 && !error && (
        <p className="muted empty">Noch keine veröffentlichten Filme vorhanden.</p>
      )}

      {selected && <VideoModal video={selected} onClose={() => setSelected(null)} onToggleWatchlist={handleToggle} />}
    </main>
  );
}
