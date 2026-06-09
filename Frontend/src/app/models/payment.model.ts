export interface Price {
  code: string;
  amount: number;
}

export interface StripeConfirmRequest {
  stripeId: string;
  internalId: string;
  token: string | null;
}
