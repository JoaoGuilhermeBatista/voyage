# LUVOY — Backend Entities

## User

Fields:
- id
- name
- email
- avatar
- onboarding_completed
- preferences

---

## Destination

Fields:
- id
- title
- country
- city
- hero_image
- description
- tags

---

## Itinerary

Fields:
- id
- user_id
- destination_id
- days
- experiences
- exported_pdf

---

## Experience

Fields:
- id
- title
- category
- coordinates
- description
- images