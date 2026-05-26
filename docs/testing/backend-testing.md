# Backend Tests

# Objetivo

Garantir estabilidade das APIs e regras de negócio.

---

# Tipos de Teste

## Unit Tests

Testar:
- services
- geração de roteiro
- validações
- filtros

---

## Integration Tests

Testar:
- banco PostgreSQL
- autenticação
- endpoints REST
- persistência

---

# Regras Críticas

O backend NÃO pode:
- gerar roteiros irreais;
- permitir dados inconsistentes;
- salvar entidades inválidas.

---

# Fluxos Críticos

## User
- criação
- login
- atualização

---

## Trip
- criação
- edição
- exclusão

---

## Itinerary
- organização por dia
- distância lógica
- quantidade de atividades

---

# Segurança

Validar:
- autenticação;
- autorização;
- endpoints protegidos.