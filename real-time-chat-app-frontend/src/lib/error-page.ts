export function renderErrorPage(): string {
  return `<!doctype html>
<html lang="en" data-bs-theme="dark">
  <head>
    <meta charset="utf-8" />
    <title>This page didn't load</title>
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <style>
      :root { color-scheme: dark; --sofismar-bg: #0D0D0D; --sofismar-surface-2: #252226; --sofismar-surface-3: #555259; --sofismar-text-primary: #F2F2F2; --sofismar-text-secondary: #BFBFBF; --sofismar-primary-button-bg: #F2F2F2; --sofismar-primary-button-color: #0D0D0D; --sofismar-primary-button-hover: #D9D9D9; --sofismar-border: rgba(242, 242, 242, 0.08); }
      body { font: 15px/1.5 system-ui, -apple-system, sans-serif; background: var(--sofismar-bg); color: var(--sofismar-text-primary); display: grid; place-items: center; min-height: 100vh; margin: 0; padding: 1.5rem; }
      .card { max-width: 28rem; width: 100%; text-align: center; padding: 2rem; background: var(--sofismar-surface-2); border: 1px solid var(--sofismar-border); border-radius: 1rem; }
      h1 { font-size: 1.25rem; margin: 0 0 0.5rem; }
      p { color: var(--sofismar-text-secondary); margin: 0 0 1.5rem; }
      .actions { display: flex; gap: 0.5rem; justify-content: center; flex-wrap: wrap; }
      a, button { padding: 0.5rem 1rem; border-radius: 0.375rem; font: inherit; cursor: pointer; text-decoration: none; border: 1px solid transparent; }
      .primary { background: var(--sofismar-primary-button-bg); color: var(--sofismar-primary-button-color); }
      .primary:hover { background: var(--sofismar-primary-button-hover); }
      .secondary { background: var(--sofismar-surface-3); color: var(--sofismar-text-primary); }
    </style>
  </head>
  <body>
    <div class="card">
      <h1>This page didn't load</h1>
      <p>Something went wrong on our end. You can try refreshing or head back home.</p>
      <div class="actions">
        <button class="primary" onclick="location.reload()">Try again</button>
        <a class="secondary" href="/">Go home</a>
      </div>
    </div>
  </body>
</html>`;
}
