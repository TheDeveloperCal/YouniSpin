# Contributing to YouniSpin

Thanks for your interest in improving YouniSpin. Focused bug fixes,
accessibility improvements, tests, and documentation updates are welcome.

## Before making a change

- Search existing issues and code to avoid duplicate work.
- For behavior or design changes, describe the user impact and approach in an
  issue before starting substantial implementation.
- Keep changes focused and preserve existing Room data compatibility.
- Do not commit credentials, signing keys, local machine paths, generated build
  output, or user/device identifiers.

## Development

1. Follow [docs/BUILDING.md](docs/BUILDING.md) to set up Android SDK and JDK.
2. Make the smallest change that fully addresses the problem.
3. Add focused tests for new behavior and regressions.
4. Run unit tests, lint, and the debug build:

   ```bash
   ./gradlew testDebugUnitTest lintDebug assembleDebug --no-daemon
   ```

5. In your change description, report the commands run and any checks not
   completed. Do not describe a feature as verified on-device unless it was
   actually exercised on a device.

## Pull requests

Provide a concise problem statement, implementation summary, test results, and
screenshots for visible UI changes. Mention compatibility or migration impacts.
Keep unrelated formatting or generated files out of the change.

## Licensing

By submitting a contribution, you agree it may be distributed under the
project's [MIT License](LICENSE). Keep third-party code and assets under their
original licenses and include required attribution.
