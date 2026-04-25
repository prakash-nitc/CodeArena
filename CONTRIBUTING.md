# Contributing to CodeArena

Thanks for your interest in contributing! This project is primarily a learning exercise, but contributions and suggestions are welcome.

## How to Contribute

1. **Fork** the repository
2. **Create a branch** for your feature or fix:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Commit** your changes with clear messages:
   ```bash
   git commit -m "feat: add problem submission endpoint"
   ```
4. **Push** to your fork and open a **Pull Request**

## Commit Message Convention

This project follows [Conventional Commits](https://www.conventionalcommits.org/):

| Prefix | Usage |
|--------|-------|
| `feat:` | New feature |
| `fix:` | Bug fix |
| `docs:` | Documentation only |
| `refactor:` | Code restructuring (no behavior change) |
| `test:` | Adding or updating tests |
| `chore:` | Build config, dependencies, etc. |

## Code Style

- Follow standard Java naming conventions
- Use Lombok annotations where appropriate to reduce boilerplate
- Write unit tests for new service methods
- Keep controllers thin — delegate logic to the service layer

## Reporting Issues

Open an issue on GitHub with:
- A clear title and description
- Steps to reproduce (if it's a bug)
- Expected vs actual behavior
