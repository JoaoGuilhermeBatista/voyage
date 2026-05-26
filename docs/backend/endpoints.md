# LUVOY — API Endpoints

## Authentication

### POST /auth/signup
Creates new user.

### POST /auth/login
Authenticates user.

### POST /auth/logout
Ends session.

---

## User

### GET /users/me
Returns authenticated user.

### PATCH /users/preferences
Updates onboarding preferences.

---

## Destinations

### GET /destinations
Returns curated destinations.

### GET /destinations/:id
Returns destination details.

---

## Itineraries

### POST /itineraries
Creates itinerary.

### GET /itineraries
Returns user itineraries.

### GET /itineraries/:id
Returns itinerary details.

### DELETE /itineraries/:id
Soft deletes itinerary.

---

## Favorites

### POST /favorites
Adds favorite destination.

### DELETE /favorites/:id
Removes favorite.

---

## PDF

### POST /pdf/export
Generates itinerary PDF.