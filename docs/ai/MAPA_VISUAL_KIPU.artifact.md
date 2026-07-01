# Mapa Visual y Arquitectónico de Kipu

Este documento sirve como brújula para cualquier desarrollador o IA que trabaje en el proyecto. Está diseñado para ser procesado rápidamente y ahorrar tokens.

## 1. Grafo de Dependencias (Estructura de Módulos)
Kipu utiliza una arquitectura **Clean Architecture Multi-módulo**.

```mermaid
graph TD
    subgraph App
        A[":app"]
    end

    subgraph Características (Features)
        F1[":feature:home"]
        F2[":feature:movements"]
        F3[":feature:envelopes"]
        F4[":feature:commitments"]
        F5[":feature:profile"]
        F6[":feature:plan"]
        F7[":feature:receipts"]
        F8[":feature:juntas"]
    end

    subgraph Núcleo (Core)
        C1[":core:domain"]
        C2[":core:data"]
        C3[":core:designsystem"]
    end

    A --> F1 & F2 & F3 & F4 & F5 & F6 & F7 & F8
    A --> C1 & C2 & C3

    F1 & F2 & F3 & F4 & F5 & F6 & F7 & F8 --> C1
    F1 & F2 & F3 & F4 & F5 & F6 & F7 & F8 --> C3

    F7 --> C2
    F3 --> F2
    C2 --> C1
```

## 2. Flujo de Navegación (User Journey)
Cómo se mueven los datos y el usuario entre pantallas.

```mermaid
stateDiagram-v2
    [*] --> Onboarding: onboardingCompleted == false
    Onboarding --> PlanWizard: "Comenzar con mi plan"
    PlanWizard --> Home: "Finalizar"
    Onboarding --> Home: "Configurar después"

    state BottomBar {
        Home --> Movements
        Movements --> Envelopes
        Envelopes --> Commitments
        Commitments --> Profile
    }

    Home --> Receipts_Hub: "Escanear"
    Movements --> Receipts_Hub
    Profile --> Juntas: "Ver mis juntas"
    Profile --> Privacidad: "Leer política"
    Envelopes --> PlanWizard: "Ajustar límites"
```

## 3. Modelo de Datos (Room v12)
Relación entre las entidades principales de la base de datos.

```mermaid
erDiagram
    MOVIMIENTO }|--|| CATEGORIA : "pertenece a"
    MOVIMIENTO }|--o| SOBRE : "afecta presupuesto"
    MOVIMIENTO }|--o| COMPROMISO : "vinculado a (Ingresos)"
    MOVIMIENTO }|--o| GASTO_JUNTA : "fuente de"

    PLAN_FINANCIERO ||--o{ SOBRE : "define límites"
    PLAN_FINANCIERO ||--o{ COMPROMISO : "incluye metas"

    JUNTA ||--o{ GASTO_JUNTA : "contiene"

    MOVIMIENTO {
        string id PK
        long amountCents
        string categoryId FK
        string source "RECEIPT, NOTIFICATION, MANUAL"
        string status "CONFIRMED, PENDING"
    }

    SOBRE {
        string id PK
        string categoryId FK
        long weeklyLimit
    }
```

## 4. Estado del Proyecto (Roadmap)
Visualización de las 27 fases completadas.

```mermaid
pie title Progreso Actual (Fase 27/27)
    "Completado (MVP + Pulido)" : 100
    "Pendiente (Internal Testing)" : 0
```
