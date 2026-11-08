import {destinationForSignedInProfile} from './flowPolicy';

test('routes incomplete signed-in profiles back into onboarding', () => {
  expect(destinationForSignedInProfile(false)).toBe('choose-apps');
  expect(destinationForSignedInProfile(true)).toBe('review');
});
