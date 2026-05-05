# Getting Started

This guide will help you integrate Ageratum into your NeoForge mod in under 5 minutes.

---

## 1. Add the Dependency

Add Ageratum to your `build.gradle`:

```groovy
repositories {
    maven {
        name = "Ageratum"
        url = "https://server.cjsah.net:1002/maven"
    }
}

dependencies {
    // API-only dependency (not bundled in your jar)
    compileOnly "dev.anvilcraft.resource:ageratum-neoforge-1.21.1:0.0.1"
    // Also add runtimeOnly if you need it during local testing
    runtimeOnly "dev.anvilcraft.resource:ageratum-neoforge-1.21.1:0.0.1"
}
```

---

## 2. Create Documentation Files

Create Markdown files inside your resource pack following this directory layout:

```
src/main/resources/
└── assets/
    └── <your_modid>/
        └── ageratum/
            ├── en_us/
            │   └── index.md        ← English home page (required)
            └── zh_cn/
                └── index.md        ← Chinese home page (optional)
```

**Example `assets/<your_modid>/ageratum/en_us/index.md`:**

```markdown
# My Mod Guide

Welcome to My Mod! This guide will help you get started.

## Features

- Feature A
- Feature B

## Quick Links

- [Getting Started](getting_started)
- [FAQ](faq)
```

---

## 3. Open Documentation with a Command

Run the following client-side command in-game:

```
/ageratum <your_modid>
```

Ageratum automatically selects the documentation matching the client's current language, falling back to `en_us` if not found.

### Command Syntax

```
/ageratum <namespace>               # Opens <namespace>:ageratum/<lang>/index.md
/ageratum <namespace> <file>        # Opens <namespace>:ageratum/<lang>/<file>.md
/ageratum <namespace> <dir>/<file>  # Opens a file in a subdirectory
```

> Both arguments support Tab completion, listing namespaces and files that actually exist in the resource pack.

---

## 4. Open from the Server Side

If your mod needs to trigger the guide from the server (or the server thread in SSP):

```java
import dev.anvilcraft.resource.ageratum.Ageratum;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

// Call this from an event handler or command
public void onPlayerAction(ServerPlayer player) {
    ResourceLocation docLocation = ResourceLocation.fromNamespaceAndPath(
        "mymod", "ageratum/en_us/getting_started.md"
    );
    Ageratum.openGuide(player, docLocation);
}
```

Ageratum sends a network packet to notify the client to open the specified document.

---

## 5. Add More Documentation

You can freely create subdirectories and additional `.md` files under `ageratum/<lang>/`:

```
assets/<your_modid>/ageratum/
├── en_us/
│   ├── index.md
│   ├── getting_started.md
│   ├── faq.md
│   └── tutorial/
│       ├── basics.md
│       └── advanced.md
└── zh_cn/
    ├── index.md
    └── ...
```

Link between documents using **relative paths**:

```markdown
[Getting Started](getting_started)
[Basics Tutorial](tutorial/basics)
```

---

## Next Steps

- See [Markdown Syntax Reference](03-markdown-syntax.md) for supported syntax
- See [Extension Components](04-extension-components.md) to add custom block components
- See [API Reference](07-api-reference.md) for the full public API

