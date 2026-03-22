---
title: Beginner's Tutorial
navigation:
  title: Beginner
---

# Beginner's Tutorial: Getting Started

This tutorial is for developers who are just starting to use Ageratum.

## Create Your First Document

### Step 1: Create File Structure

```
src/main/resources/
└── assets/
    └── mymod/
        └── ageratum/
            └── en_us/
                └── index.md
```

### Step 2: Write index.md

```markdown
---
title: My Mod Guide
---

# Welcome

This is my mod's guide.

## Getting Started

[Check out the quick start guide](../guide)
```

### Step 3: Run the Game

```
/ageratum mymod
```

## FAQ

**Q: Is the file name case-sensitive?**

A: It depends on your operating system. We recommend using lowercase and underscores.

**Q: Can I use Chinese file names?**

A: Not recommended. Use English file names and set Chinese titles in front matter.

**Q: How many documents are suitable?**

A: There's no limit, but we recommend not exceeding 50 top-level documents for performance.
## Navigation

- [Intermediate Tutorial](intermediate)
- [Advanced Tutorial](advanced)
- [Back to Homepage](../../index)

