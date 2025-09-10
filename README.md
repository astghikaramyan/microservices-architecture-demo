## 🧪 Testing Strategy

It was applied a layered testing approach to balance **speed, coverage, and confidence**.

---

### 1. Unit Tests
- **Purpose:** Validate individual classes/methods in isolation
- **Tools:** JUnit, Mockito
- **Scope:** Service, utility, and validation logic
- **Goal:** High coverage, fast feedback, catch regressions early

---

### 2. Integration Tests
- **Purpose:** Test business scenarios at the API/component level, verify collaboration between application layers and external dependencies
- **Tools:** JUnit, Mockito, Testcontainers
- **Scope:** Database, clients, messaging integrations
- **Goal:** Ensure modules work together as expected

---

### 3. Component Tests
- **Purpose:** Test each microservice/module as a black box
- **Tools:** Spring Boot Test, JUnit, Mockito
- **Scope:** Endpoints, message flows, persistence (with external systems mocked)
- **Goal:** Ensure correctness of the service as a standalone deployable unit

---

### 4. Contract Tests
- **Purpose:** Ensure service contracts are honored between producers and consumers
- **Tools:** Pact
- **Scope:** HTTP APIs, messaging (RabbitMQ)
- **Goal:** Prevent breaking changes, enable stub propagation for consumer-driven contracts

---

### 5. End-to-End (E2E) Tests
- **Purpose:** Validate complete user flows across services
- **Tools:** Cucumber
- **Scope:** API layer, cross-service scenarios
- **Goal:** Ensure the system works as a whole from a user perspective

---

### ✅ Approach Summary
- **Combination:** All five test types are applied, striking a balance between speed and coverage.
    - Unit and integration tests form the foundation
    - Component, contract, and end-to-end tests validate business logic and service communication
- **Benefits:**
    - Early bug detection
    - Stable and reliable releases
    - Confidence in refactoring
    - Living documentation of system requirements  
