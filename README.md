<h1 align="center">Project-Zomboid-EtherHack</h1>
<p align="center">
  <img src="demo/EtherLogo.png" alt="EtherHack Logo" width="200">
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Project%20Zomboid-42.17.0%2B-blue" alt="PZ Version">
  <img src="https://img.shields.io/badge/Java-25-orange" alt="Java 25">
  <img src="https://img.shields.io/badge/Gradle-9.1.0-green" alt="Gradle">
  <img src="https://img.shields.io/github/license/cruzgsworks/Project-Zomboid-EtherHack" alt="License">
</p>

> **⚠️ WARNING:** This is a cheat/modification tool for Project Zomboid. Use at your own risk. Most features have only been tested in single-player mode.

## Overview

EtherHack is a Java/Lua-based modification for Project Zomboid that provides additional gameplay functionality. This fork has been updated for **Project Zomboid Build 42.17.0+** compatibility.

### Key Changes from Original
- **Java 25 Bytecode Support:** Updated patching system for PZ 42's Java 25 runtime
- **Tree API Patching:** Replaced MethodVisitor approach with ASM Tree API + COMPUTE_FRAMES to preserve StackMapTable
- **CharacterStat API:** Updated stats system for PZ 42's `Map<CharacterStat, Float>` structure
- **DebugOptions Panel:** New cheats panel using PZ 42's native `DebugOptions` API

---

## Table of Contents
- [Compatibility](#compatibility)
- [Features](#features)
- [Prerequisites](#prerequisites)
- [Building from Source](#building-from-source)
- [Installation](#installation)
- [Uninstallation](#uninstallation)
- [Usage](#usage)
- [Known Issues](#known-issues)
- [Troubleshooting](#troubleshooting)
- [For Developers](#for-developers)
- [Credits](#credits)
- [License](#license)

---

## Compatibility

| Component | Version |
|-----------|---------|
| Project Zomboid | **42.17.0+** |
| Java Runtime | **25** |
| Gradle | **9.1.0** |

> **Note:** This version is NOT compatible with PZ 41.x. For PZ 41, use the original [Yeet-Masta/EtherHack](https://github.com/Yeet-Masta/Project-Zomboid-EtherHack).

---

## Features

All features work in single-player mode. Multiplayer compatibility is untested and not officially supported.

---

## Prerequisites

### Required Software
1. **JDK 25** (for Gradle build and Java 25 bytecode compilation)
   - [Oracle JDK 25](https://www.oracle.com/java/technologies/downloads/)
2. **Steam copy of Project Zomboid** (Build 42.17.0+)

### Environment Variables
Set `JAVA_HOME` pointing to your JDK 25 installation:
```powershell
# Windows PowerShell (temporary)
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"

# Windows System (permanent)
[Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Java\jdk-25.0.3", "Machine")
```

---

## Building from Source

### 1. Clone the Repository
```bash
git clone https://github.com/cruzgsworks/Project-Zomboid-EtherHack.git
cd Project-Zomboid-EtherHack
```

### 2. Copy Game Libraries
Copy these JARs from your Project Zomboid installation directory to `lib/`:
- `zombie.jar` (copy `projectzomboid.jar` and rename, or use directly)
- `Kahlua.jar`
- `fmod.jar`
- `org.jar`

> **Note:** These JARs are not included for copyright reasons.

### 3. Build
```powershell
# Windows
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

.\gradlew.bat --no-daemon build
```

```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk-25
export PATH=$JAVA_HOME/bin:$PATH
./gradlew --no-daemon build
```

### 4. Output
The built JAR will be at `build/EtherHack-{version}.jar`

---

## Installation

1. **Close Project Zomboid** if it's running
2. **Copy the JAR** to your Project Zomboid root folder:
   ```
   C:\Program Files (x86)\Steam\steamapps\common\ProjectZomboid\
   ```
3. **Run the installer**:
   ```powershell
   java -jar EtherHack-{version}.jar --install
   ```
4. **Start the game**

---

## Uninstallation

```powershell
# Run from the Project Zomboid directory
java -jar EtherHack-{version}.jar --uninstall
```

This will:
- Restore the original `projectzomboid.jar`
- Remove all extracted EtherHack files

---

## Usage

| Key | Function |
|-----|----------|
| `Insert` | Open/Close cheat menu |
| `Home` | Reload Lua GUI (close all windows first) |

### Menu Sections
- **Character:** Edit traits, skills, stats
- **Items:** Spawn items, repair vehicles
- **Visuals:** ESP, player highlighting
- **Exploits:** Teleport, safe teleport
- **Cheats:** Debug options panel (PZ 42 native cheats)
- **Settings:** Configure colors, options

---

## Known Issues

### Technical
- **Gradle 8.x incompatible:** Gradle 8.14.2's Kotlin compiler doesn't support Java 25. Must use Gradle 9.0+.
- **MethodVisitor patching:** PZ 42's Java 25 bytecode requires StackMapTable preservation. Tree API + COMPUTE_FRAMES is required.

---

## Troubleshooting

### Build fails with "25.0.3"
**Cause:** Gradle's Kotlin compiler can't parse Java 25 version string.
**Fix:** Use Gradle 9.0 Milestone 2 or newer.

### `UnsupportedClassVersionError` during install
**Cause:** Running installer with wrong Java version.
**Fix:** Ensure `JAVA_HOME` points to JDK 25:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-25.0.3"
& "$env:JAVA_HOME\bin\java.exe" -jar EtherHack.jar --install
```

### Game crashes on startup after install
**Cause:** Corrupted `projectzomboid.jar` or leftover files from previous install.
**Fix:**
1. Run `--uninstall`
2. Verify game files via Steam
3. Reinstall

### Menu doesn't open
**Cause:** Lua files not extracted properly.
**Fix:** Check `ProjectZomboid/EtherHack/lua/` exists. Reinstall if missing.

---

## For Developers

### Adding Custom Lua Methods

In `src/main/java/EtherHack/Ether/EtherAPI.java`, add to `GlobalEtherAPI`:

```java
@LuaMethod(name = "yourMethodName", global = true)
public static String yourMethodName() {
    return "Test!";
}
```

Call from Lua:
```lua
print(yourMethodName())
-- Output: Test!
```

### Loading External Lua
```lua
EtherRequire "path/to/your.lua"
```
Path is relative to the game root folder.

### Project Structure
```
src/
├── main/
│   ├── java/
│   │   └── EtherHack/
│   │       ├── Ether/
│   │       │   ├── EtherAPI.java          # Lua API bridge
│   │       │   ├── EtherLuaMethods.java   # Lua method implementations
│   │       │   ├── StatsModifier.java     # CharacterStat handling
│   │       │   └── ...
│   │       ├── utils/
│   │       │   ├── Patch.java             # Bytecode patching (Tree API)
│   │       │   └── ...
│   │       └── Main.java                  # Entry point
│   └── resources/
│       └── EtherHack/
│           └── lua/                       # GUI components
```

---

## Credits

- **Updater:** cruzgsworks and drkm43
- **Original Author:** Yeet-Masta
- **Contributors:** Community contributors via GitHub

Special thanks to:
- The Project Zomboid modding community
- [Zomboid Decompiler](https://github.com/asledgehammer/ZomboidDecompiler) for codebase analysis
- [demiurgequantified](https://demiurgequantified.github.io/ProjectZomboidJavaDocs/) for PZ JavaDocs

---

## License

This project is under `MIT License` - see the [LICENSE](LICENSE) file for details.

---

## Disclaimer

This software is provided 'as-is', without any express or implied warranty. In no event will the author be held liable for any damages arising from the use of this software. Use of this software may violate the terms of service of the game and could lead to your account being banned. Use at your own risk.

**Use responsibly and understand the consequences that may arise as a result of improper use.**

---

<p align="center">
  <sub>Built with ❤️ for the Project Zomboid community</sub>
</p>
