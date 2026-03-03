# OptiCare

Hospital patient-to-bed assignment optimizer. Dynamically assigns patients to departments, rooms, and beds while minimizing a cost function and respecting safety (cohorting), clinical, and policy constraints.

## Overview

- **Goal:** Optimal dynamic assignment of patients to departments, rooms, and beds in a hospital.
- **Objective function:** Minimize  
  \( Z = \sum_{i=1}^{P} (C_{safety}^i + C_{clinical}^i + C_{policy}^i + C_{transfer}^i) \)
- **Constraints:** Hard (violation → Big M penalty) and soft (smaller penalties).
- **Algorithm:** Simulated Annealing with **Assign**, **Move**, and **Swap** neighborhood moves.
- **Architecture:** MVC (Model, Controller, View).

## Tech stack

- **Java 17**
- **Maven** (build, test)
- **JUnit 5** (unit tests)

## Project structure
