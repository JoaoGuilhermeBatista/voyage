# LUVOY — Database

## Database Provider

- PostgreSQL
- Supabase

---

## Main Entities

### users
### destinations
### itineraries
### experiences
### favorites
### onboarding_preferences

---

## Rules

All tables must:
- use UUID
- use timestamps
- support soft delete when needed
- support RLS

---

## Security

Enable:
- Row Level Security
- authenticated access
- protected writes