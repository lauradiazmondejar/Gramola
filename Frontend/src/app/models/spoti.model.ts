export interface SpotiToken {
  access_token: string;
  refresh_token: string;
  token_type: string;
  expires_in: number;
  scope: string;
}

export interface SpotiDevice {
  id: string;
  name: string;
  type: string;
  is_active: boolean;
  volume_percent: number;
}

export interface SpotiTrack {
  id: string;
  name: string;
  uri: string;
  artists: { id: string; name: string }[];
  album: { name: string; images: { url: string }[] };
  duration_ms: number;
}

export interface SpotiPlaylist {
  id: string;
  name: string;
  uri: string;
  images: { url: string }[];
  tracks: { total: number };
}
