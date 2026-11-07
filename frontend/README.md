# LedgerLine frontend

The LedgerLine frontend uses React, Vite, Tailwind CSS 4, and Axios. Its initial dashboard reports the backend health endpoint and intentionally displays no sample account or transaction data.

## Development

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev
```

Set `VITE_API_BASE_URL` in `.env.local` to configure the API base URL. The default is `http://localhost:8080/api`.

## Checks

```powershell
npm run build
npm run lint
```

For database configuration and backend instructions, see the project [README](../README.md).
