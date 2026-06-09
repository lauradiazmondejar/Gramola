export interface Song {
  id: number;
  title: string;
  artist: string;
  uri: string;
  priority: boolean;
  date: string;
}

// Formato normalizado que usa la UI (igual que un Track de Spotify)
export interface SongTrack {
  name: string;
  artists: { name: string }[];
  uri: string;
  esCatalogo?: boolean;
}
