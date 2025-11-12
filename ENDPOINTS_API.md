# API Endpoints - Architecture Modulaire

## Vue d'ensemble

L'application utilise maintenant une architecture modulaire avec des contrôleurs spécialisés pour chaque type de document.

## Endpoints Disponibles

### 📧 EmailController (`/api/email`)

#### Traitement d'un email
```
POST /api/email/process
Content-Type: application/json

{
  "userId": 123,
  "subject": "Sujet de l'email",
  "bulletPoints": ["Point 1", "Point 2"],
  "recipientEmails": ["email1@example.com", "email2@example.com"]
}
```

#### Récupération d'un email spécifique
```
GET /api/email/{userId}/{requestId}
```

#### Liste de tous les emails
```
GET /api/email
```

### 📋 PvController (`/api/pv`)

#### Traitement d'un PV
```
POST /api/pv/process
Content-Type: application/json

{
  "userId": 123,
  "date": "2024-01-15",
  "startTime": "14:00",
  "closingTime": "16:00",
  "location": "Salle de réunion A",
  "participants": ["Participant 1", "Participant 2"],
  "bulletPoints": ["Point 1", "Point 2"]
}
```

#### Récupération d'un PV spécifique
```
GET /api/pv/{userId}/{requestId}
```

#### Liste de tous les PV
```
GET /api/pv
```

## Architecture

```
Client Request
    ↓
EmailController / PvController (Spécialisé)
    ↓
EmailService / PvService (Spécialisé)
    ↓
LlmService (Commun)
    ↓
EmailRequestRepo / PvRequestRepo
```

## Avantages

- **Modularité** : Chaque contrôleur gère un domaine spécifique
- **Clarté** : URLs plus explicites (`/api/email/*` vs `/api/process/email`)
- **Maintenabilité** : Code séparé et plus facile à maintenir
- **Évolutivité** : Facile d'ajouter de nouvelles fonctionnalités

## Migration

Les anciens endpoints (`/api/process/email`, `/api/process/pv`) ne sont plus disponibles. 
Les clients doivent migrer vers les nouveaux endpoints spécialisés.

