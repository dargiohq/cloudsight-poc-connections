# CloudSight POC Connections

Public demo URL:

- `https://cloudsight-poc-connections.onrender.com`

Purpose:

- demonstrate the `provider connection / billing-first` integration path
- show how a client registers AWS, GCP, Azure, and OpenAI account connections
- show readiness, collector coverage, and connection inventory against CloudSight

Key endpoints:

- `/`
- `/health`
- `/demo/contract`
- `/demo/overview`
- `/demo/register-all`
- `/demo/validate-all`
- `/demo/audit`

CloudSight target:

- `https://dargio-cloudsight-backend.onrender.com`

This app sends:

- provider identifiers
- secret references
- scope metadata
- no raw secrets and no PII in payloads
