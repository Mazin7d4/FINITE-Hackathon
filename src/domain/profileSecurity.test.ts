import {isHashOnlyProfile} from './profileSecurity';

describe('local profile security boundaries', () => {
  test('accepts only salted derived credentials and never plaintext', () => {
    expect(isHashOnlyProfile({passwordSalt: 'salt', passwordHash: 'hash', passwordIterations: 120000})).toBe(true);
    expect(isHashOnlyProfile({password: 'secret', passwordSalt: 'salt', passwordHash: 'hash', passwordIterations: 120000})).toBe(false);
  });
});
