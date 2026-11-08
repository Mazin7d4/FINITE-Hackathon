export type FlowDestination = 'review' | 'choose-schedule';

/** Any local session without a saved goal must finish onboarding before review. */
export function destinationForSignedInProfile(hasGoal: boolean): 'review' | 'choose-apps' {
  return hasGoal ? 'review' : 'choose-apps';
}

/** Settings editors always finish on the dashboard; onboarding continues in order. */
export function destinationAfterAppsSaved(editing: boolean): FlowDestination {
  return editing ? 'review' : 'choose-schedule';
}

/** Saving any editor must discard the editor branch so Back cannot replay onboarding. */
export function dashboardHistory(): [] {
  return [];
}
