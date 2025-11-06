# EMD Citizen
Service that manages the activation/deactivation operations of citizens' consents on third-party apps.



## Documentation 
The Javadoc documentation for the latest stable version is automatically published and available at the following link:
[**View Javadoc (GitHub Pages)**](https://pagopa.github.io/emd-citizen/index.html)

The OpenAPI specification can be found in another repository: [**openapi.citizen.yml**](https://github.com/pagopa/cstar-infrastructure/blob/main/src/domains/mil-app-poc/api/emd_citizen/openapi.citizen.yml).

## Contributing and Releasing
### Workflow (Trunk Based Development)
This repository uses a **Trunk Based Development (TBD)** approach.

* The `main` branch is the single source of truth and must always remain stable and ready for release.
* All work must be done on short-lived branches (e.g., `feat/my-feature` or `fix/bug-fixed`).
* Branches must be merged into `main` as soon as possible through Pull Requests (PR).

### Feature Flag Usage

To support TBD and keep main stable, incomplete features that are merged must be "hidden" behind a Feature Flag (or "Feature Toggle").

This allows code integration (Continuous Integration) and deployment to production while keeping it "off" or "dark" (dark launch). This way, the technical deployment of code is separated from the actual feature release to users.

### How to Create a Pull Request
To ensure changes are properly detected by the release system, it is **mandatory** to follow the [**Conventional Commits**](https://www.conventionalcommits.org/en/v1.0.0/) standard for commit messages (or, more simply, for the **PR title** when doing "Squash and Merge").

The automatic release system will only trigger if you merge PRs with these prefixes:
* `feat:` (for a new feature, e.g., `feat: add login`)
* `fix:` (for a bug fix, e.g., `fix: correct total calculation`)

Commits with prefixes like `chore:`, `ci:`, `refactor:`, `style:`, `test:`, or `docs:` will **not** trigger a new release.

### Automatic Release Process
The release process is semi-automatic and managed by the `release-please` action.
1. **Release PR Creation:** When a `feat:` or `fix:` commit arrives on `main`, an automatic action opens (or updates) a **"Release PR"** (e.g., `chore: release v1.2.3`). This PR contains the updated `CHANGELOG.md` and incremented versions (e.g., `pom.xml`).
2. **Manual Release:** To release a new version, simply **merge that "Release PR"**.

Merging the "Release PR" triggers the entire publication process:
1. **GitHub Release:** A new GitHub Release is created with the tag and changelog.
2. **Docker Image:** A new Docker image is automatically built and published to GitHub Container Registry (GHCR) with the version tag (e.g., `v1.2.3`).
3. **Javadoc:** The Javadoc documentation is generated and automatically deployed to GitHub Pages.