---
description: Implementa uma feature do LUVOY a partir do seu spec aprovado
---

## Input do usuário

```text
$ARGUMENTS
```

## Passos

1. Leia `.specify/memory/constitution.md`.
2. Leia o spec da feature em `docs/<módulo>/<feature>.md`.
3. Implemente na ordem:
   a. Backend: Entity → Repository → Service → Controller → DTO
   b. Testes: unit tests do service → controller tests
   c. Frontend: domain entity → repository interface → datasource (Dio) → provider → screen → widgets
4. A cada arquivo criado, verifique se respeita as regras da constituição.
5. Não implemente o que não está no escopo MVP do spec.
