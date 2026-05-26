# LUVOY — Flutter Structure

## Architecture

Feature-first architecture.

---

## Folder Structure

lib/
├── core/
├── shared/
├── features/
│   ├── auth/
│   ├── onboarding/
│   ├── home/
│   ├── destinations/
│   ├── itinerary/
│   ├── profile/
│   └── favorites/
├── theme/
└── main.dart

---

## Rules

Each feature contains:
- presentation
- domain
- data

---

## Widgets

Prefer:
- reusable widgets
- composable UI
- modular components

Avoid:
- giant screens
- duplicated UI