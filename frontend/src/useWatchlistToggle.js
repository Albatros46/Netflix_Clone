import { api } from './api';

// Fuegt einen Film zur Merkliste hinzu bzw. entfernt ihn und gibt den neuen Zustand zurueck
export async function toggleWatchlist(video) {
  if (video.inWatchList) {
    await api(`/videos/watchlist/${video.id}`, { method: 'DELETE' });
  } else {
    await api(`/videos/watchlist/${video.id}`, { method: 'POST' });
  }
  return { ...video, inWatchList: !video.inWatchList };
}
