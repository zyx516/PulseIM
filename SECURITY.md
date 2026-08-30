# Security Policy

PulseIM is a demo distributed IM system. Please do not commit real production credentials, tokens, private keys, database dumps, or user data.

## Reporting

If you find a security issue, open a private report or contact the repository owner directly. Public issues should avoid exposing exploitable details or secrets.

## Local configuration

Runtime secrets should be supplied through environment variables or a local `.env` file. The repository only keeps `.env.example` as a non-sensitive template.
