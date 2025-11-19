# Atari Go - Scala Implementation (GUI + TUI)

This project is an implementation of Atari Go, developed in Scala with both a JavaFX graphical interface and a text-based UI.  
It was built as part of a university assignment focused on functional programming, recursion, GUI event handling, and game state management.

The game supports configurable board sizes, time limits, capture goals, undo functionality, deterministic randomness through a seed file, and full rule enforcement for Atari Go.

---

## Overview

The project provides two ways to play:

### 1. GUI Version (JavaFX)
A JavaFX interface allowing users to:
- Select board size, capture goal, and time limit
- Place stones by clicking
- Visualize captures immediately
- Undo moves
- Restart the game

### 2. TUI Version (Terminal)
A text based version supporting:
- Menu navigation
- Coordinate based move input
- Undo
- Timer enforcement
- Printed board state after each turn

Both versions share the same underlying game logic, implemented in a purely functional style.

---

## Key Concepts Implemented

### Functional Programming
The core engine uses:
- Immutable board representations (`List[List[Stone]]`)
- Pure functions for move logic and capture resolution
- Tail recursive coordinate generation
- Deterministic random move generation via a custom `MyRandom` class
- Algebraic data types (case classes, enums)

### Game Logic
The project implements:
- Liberties and capture detection
- Group detection via DFS
- Suicide move prevention
- Open coordinate tracking
- Game ending conditions based on capture counts

### JavaFX and Event Driven Programming
The GUI version includes:
- FXML layout setup
- Controller class binding with `@FXML`
- Event handlers for board clicks
- Dynamic rendering of stones (JavaFX `Circle`)
- Time based turn enforcement

---

## Project Structure

```

src/
├── Controller.scala # JavaFX controller for the GUI
├── Controller.fxml # GUI layout
├── GUIApp.scala # Launches the JavaFX GUI
├── TUI.scala # Full terminal based UI
├── Project.scala # Core game logic (captures, moves, rules)
├── GameState.scala # Stores snapshots for undo
├── Stone.scala # Stone definitions (Black/White/Empty)
├── Types.scala # Type aliases for board and coordinates
├── MyRandom.scala # Deterministic random generator
└── SeedManager.scala # Persists RNG seeds between sessions


```

## How to Run

This project can be run directly from your IDE (such as IntelliJ).

### GUI Version
1. Open the project in your IDE  
2. Locate the class:
   `GUIApp`
3. Run the `main` method inside `GUIApp` to launch the JavaFX interface.

### TUI Version
1. Open the project in your IDE  
2. Locate the object:
   `TUIApp`
3. Run the `main` method inside `TUIApp` to start the text-based version.

JavaFX libraries must be available on your system for the GUI version.

---

## Features Implemented

- Configurable board size
- Configurable capture goal
- Player vs CPU
- Undo history (both GUI and TUI)
- Prevention of illegal/suicidal moves
- Automatic capture resolution after every move
- Time limited turns (for both player and CPU)
- Clean GUI rendering and event handling

---




