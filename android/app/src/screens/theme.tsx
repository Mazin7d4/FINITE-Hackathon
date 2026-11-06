import React from "react";

export default function FiniteUsageScreen() {
  const watched = 4;
  const limit = 70;
  const progress = Math.min((watched / limit) * 100, 100);
  const remaining = Math.max(limit - watched, 0);

  return (
    <>
      <style>{`
        :root {
          --finite-top: #B0D6DD;
          --finite-middle: #BADBCC;
          --finite-bottom: #C3DDC5;
          --finite-primary: #5B7A8C;
          --finite-text: #162B39;
          --finite-secondary: #607684;
          --finite-surface: #FFFFFF;
          --finite-divider: #DCE8E5;
        }

        * {
          box-sizing: border-box;
        }

        body {
          margin: 0;
          font-family: Inter, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
          color: var(--finite-text);
        }

        .finite-page {
          min-height: 100vh;
          min-height: 100svh;
          padding: 28px 20px;
          background: linear-gradient(
            180deg,
            var(--finite-top) 0%,
            var(--finite-middle) 55%,
            var(--finite-bottom) 100%
          );
        }

        .finite-screen {
          width: 100%;
          max-width: 390px;
          margin: 0 auto;
        }

        .finite-header {
          display: flex;
          align-items: center;
          justify-content: space-between;
          margin-bottom: 52px;
        }

        .finite-logo {
          font-size: 20px;
          font-weight: 600;
          letter-spacing: -0.4px;
        }

        .finite-mark {
          color: var(--finite-primary);
          font-size: 23px;
          margin-right: 5px;
        }

        .eyebrow {
          margin: 0 0 10px;
          color: var(--finite-secondary);
          font-size: 12px;
          font-weight: 600;
          letter-spacing: 0.08em;
          text-transform: uppercase;
        }

        h1 {
          margin: 0;
          font-size: 30px;
          line-height: 1.15;
          letter-spacing: -0.7px;
        }

        .intro {
          margin: 12px 0 26px;
          color: var(--finite-secondary);
          font-size: 15px;
          line-height: 1.5;
        }

        .usage-card {
          padding: 22px;
          border-radius: 18px;
          background: var(--finite-surface);
          box-shadow: 0 8px 24px rgba(34, 65, 75, 0.08);
        }

        .card-label {
          margin: 0 0 16px;
          color: var(--finite-secondary);
          font-size: 14px;
        }

        .count {
          display: flex;
          align-items: baseline;
          gap: 8px;
          margin-bottom: 18px;
        }

        .count-number {
          font-size: 42px;
          font-weight: 700;
          letter-spacing: -1.5px;
        }

        .count-total {
          color: var(--finite-secondary);
          font-size: 16px;
        }

        .progress-track {
          height: 9px;
          overflow: hidden;
          border-radius: 999px;
          background: #EAF0EE;
        }

        .progress-fill {
          height: 100%;
          border-radius: inherit;
          background: var(--finite-primary);
          transition: width 200ms ease;
        }

        .card-footer {
          display: flex;
          justify-content: space-between;
          gap: 12px;
          margin-top: 12px;
          color: var(--finite-secondary);
          font-size: 13px;
        }

        .actions {
          display: grid;
          gap: 12px;
          margin-top: 24px;
        }

        .primary-button,
        .secondary-button {
          min-height: 50px;
          border: 0;
          border-radius: 13px;
          font: inherit;
          font-size: 15px;
          font-weight: 600;
          cursor: pointer;
        }

        .primary-button {
          color: white;
          background: var(--finite-primary);
        }

        .secondary-button {
          color: var(--finite-text);
          background: white;
        }

        .note {
          margin: 22px 8px 0;
          color: var(--finite-secondary);
          font-size: 13px;
          line-height: 1.5;
          text-align: center;
        }
      `}</style>

      <main className="finite-page">
        <section className="finite-screen">
          <header className="finite-header">
            <div className="finite-logo">
              <span className="finite-mark">◷</span>
              finite
            </div>
          </header>

          <p className="eyebrow">Your daily check-in</p>
          <h1>Today’s usage</h1>
          <p className="intro">
            Here’s how many short videos you’ve watched today.
          </p>

          <section className="usage-card">
            <p className="card-label">Short videos today</p>

            <div className="count">
              <span className="count-number">{watched}</span>
              <span className="count-total">of {limit} videos</span>
            </div>

            <div
              className="progress-track"
              role="progressbar"
              aria-label="Daily video limit used"
              aria-valuenow={watched}
              aria-valuemin={0}
              aria-valuemax={limit}
            >
              <div
                className="progress-fill"
                style={{ width: `${progress}%` }}
              />
            </div>

            <div className="card-footer">
              <span>{remaining} left today</span>
              <span>Daily limit: {limit}</span>
            </div>
          </section>

          <div className="actions">
            <button className="primary-button" type="button">
              Start scrolling
            </button>
            <button className="secondary-button" type="button">
              Change my limit
            </button>
          </div>

          <p className="note">
            Your limit is personal, and you can change it anytime.
          </p>
        </section>
      </main>
    </>
  );
}