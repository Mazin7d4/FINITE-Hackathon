/** The only credential fields that may be persisted for a local profile. */
export type PasswordVerifier = {passwordSalt: string; passwordHash: string; passwordIterations: number};

/** Rejects legacy records that contain the removed plaintext password field. */
export function isHashOnlyProfile(value: unknown): value is PasswordVerifier {
  if (!value || typeof value !== 'object') return false;
  const item = value as Partial<PasswordVerifier> & {password?: unknown};
  const iterations = item.passwordIterations;
  return typeof item.passwordSalt === 'string' && typeof item.passwordHash === 'string' && typeof iterations === 'number' && Number.isInteger(iterations) && iterations >= 100000 && item.password === undefined;
}
