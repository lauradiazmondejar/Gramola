export interface LoginResponse {
  clientId: string;
  bar: string;
  signature: string;
}

export interface RegisterRequest {
  bar: string;
  email: string;
  pwd1: string;
  pwd2: string;
  clientId: string;
  clientSecret: string;
  lat?: number;
  lon?: number;
  signature?: string;
  songPriceCents?: number;
}
