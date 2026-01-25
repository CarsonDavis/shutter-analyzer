# Claude Code Instructions

Instructions for Claude Code when working on this repository.

---

## At Session Start

1. Read `INDEX.md` to understand the current state of the repository
2. Review linked documentation as needed for context

---

## After Any Work

1. **Update INDEX.md** immediately to reflect changes:
   - New files added
   - Files removed
   - Structural changes
   - Updated descriptions

2. **Update linked documentation** that was affected:
   - `REQUIREMENTS.md` - if requirements change or are completed
   - `IMPLEMENTATION_LOG.md` - log what was done, verification steps, results
   - `docs/*.md` - if theory, how-to, or specs change
   - `android/docs/*.md` - if Android-specific docs change

3. **Keep documentation consistent** across all files

---

## Documentation Protocol

After completing each phase or feature:

```markdown
## [Date] - [Phase/Feature Name]

### Changes Made
- `file.py`: description of changes

### Verification
- Command: `uv run ...`
- Result: PASS/FAIL

### Notes
- Any relevant observations
```

---

## Python Development

- Always use `uv` to run Python and install packages
- Never use naked `python` or `pip` directly
- Example: `uv run python -m shutter_analyzer ...`

---

## Key Files

| File | Purpose |
|------|---------|
| `INDEX.md` | Project overview, file index, primary entry point |
| `REQUIREMENTS.md` | Functional and non-functional requirements |
| `IMPLEMENTATION_LOG.md` | Progress tracking and verification log |
| `CLAUDE.md` | Short-form instructions (loaded automatically) |
| `instructions.md` | This file - detailed instructions |
