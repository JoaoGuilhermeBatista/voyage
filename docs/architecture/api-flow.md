# LUVOY — API Flow

## Communication

Flutter communicates with Supabase APIs using:
- Dio
- repositories
- typed models

---

## Auth Flow

Login
→ token generation
→ token persistence
→ route guard validation

---

## Error Handling

All API requests must:
- handle timeout
- handle offline state
- display friendly errors
- avoid crashes

---

## Loading Rules

Every async action must:
- provide visual feedback
- avoid blocking UI
- handle retries gracefully