# Pac-Man Game (Java Swing)

A classic **Pac-Man** game implemented using **Java Swing**, featuring multiple levels, score tracking, sound effects, and a custom HUD.  
This project was developed as part of a college assignment to demonstrate concepts of **OOP, event handling, graphics rendering, and game state management** in Java.

---

## ⚙️ Technologies Used

- Java (JDK 8+)
- Java Swing
- AWT Graphics & Event Handling
- Java Sound API (`Clip`, `AudioInputStream`)

---

## ▶️ How to Run

1. Ensure JDK is installed
2. Compile and run:
   ```bash
   javac App.java
   java App

---

## 🎮 Features

- Classic Pac-Man gameplay
- Grid-aligned movement (smooth and responsive turning)
- Multiple levels with different maps
- Food pellets and bonus cherry
- Score system:
  - Per-level score
  - Cumulative total score across levels
- Lives system with visual heart icons
- Game states:
  - Playing
  - Paused
  - Level transition
  - Life lost
  - Game over
  - Win condition
- Sound effects and background music
- Mute / unmute option
- Custom retro font (**Press Start 2P**)

---

## 🧠 Game Rules & Scoring

### Movement
- Pac-Man moves on a fixed grid.
- Direction changes are allowed **only when aligned to the grid**, similar to the original Pac-Man.

### Scoring
| Item        | Points |
|-------------|--------|
| Food pellet |  10    |
| Cherry      |  100   |

- `levelScore` tracks score for the **current level only**
- `totalScore` is updated **only when a level is completed**
- If Pac-Man loses all lives before completing a level, the level score is not added to the total score

---

## 🍒 Cherry Logic

- A **single cherry spawns once per level**
- Cherry appears after reaching **100 points in the current level**
- Spawn position is randomly selected from existing food positions
- Once collected, the cherry does **not respawn** in the same level

---

## ⌨️ Controls

| Key        | Action |
|------------|--------|
| Arrow Keys / WASD | Move Pac-Man |
| `P`       | Pause / Resume game |
| `M`       | Mute / Unmute sound |
| `ENTER`   | Continue / Restart (context-based) |

---

## 🔊 Audio

- Background music plays during gameplay
- Sound effects for:
  - Eating food
  - Eating cherry
  - Pac-Man death
- Volume controlled using `FloatControl`
- All sounds stop when muted

---

## 🖥️ HUD (Top Panel)

Displays:
- Game title
- Current score (level score)
- Remaining lives (heart icons)
- Current level number
- Mute status icon

HUD is rendered separately using a vertical offset to keep gameplay area clean.

---

## 🗂️ Project Structure

```text
PacManJava/
├── README.md
├── .vscode/
│   └── settings.json
│
└── src/
    ├── App.java
    ├── Levels.java
    ├── Pacman.java
    │
    ├── Fonts/
    │   └── PressStart2P-Regular.ttf
    │
    ├── Images/
    │   ├── wall.png
    │   ├── pacmanUp.png
    │   ├── pacmanDown.png
    │   ├── pacmanLeft.png
    │   ├── pacmanRight.png
    │   ├── blueGhost.png
    │   ├── orangeGhost.png
    │   ├── pinkGhost.png
    │   ├── redGhost.png
    │   ├── cherry.png
    │   ├── heart.png
    │   ├── mute.png
    │   └── unmute.png
    │
    └── Music/
        ├── PacmanMusic.wav
        ├── Eating.wav
        ├── Cherry.wav
        └── Death.wav
```
