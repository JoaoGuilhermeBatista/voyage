---
description: Gera o spec de uma nova feature do LUVOY seguindo o padrão do projeto
---

## Input do usuário

```text
$ARGUMENTS
```

## Passos

1. Leia `.specify/memory/constitution.md` para carregar as regras do projeto.
2. Leia os docs relevantes em `docs/` conforme a feature descrita.
3. Crie o arquivo de spec em `docs/<módulo>/<feature>.md` com:

```markdown
# LUVOY — <Feature Name>

## Objetivo
[O que esta feature entrega para o usuário]

## Escopo MVP
### Inclui
### Não inclui

## Backend
### Endpoint(s)
### Regras de negócio
### Erros esperados

## Frontend
### Tela(s) / Widgets
### Estado (Riverpod provider)
### Fluxo de navegação

## Testes
### Casos de erro obrigatórios
```

4. Confirme que o spec não contradiz a constituição antes de salvar.
