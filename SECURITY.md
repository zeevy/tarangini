# Security

## Report a problem in private

Do not open a public issue for a security problem, and do not describe it in a pull request or a discussion.

Report it through GitHub: open the [Security tab](../../security/advisories/new) of this repo and choose "Report a vulnerability". Only the maintainers can read the report.

Please include:

- What the problem is and what an attacker could do with it
- The steps to see it, with the app version, the phone and the Android version
- A stream URL, a file or a small piece of code that shows it, if you have one

## What happens next

| Step | When |
|---|---|
| We confirm that we got the report | Within 7 days |
| We tell you whether we can see the problem and what we plan | Within 21 days |
| We fix it, publish a new version and then publish the advisory, with your name if you want it | As soon as the fix is ready |

Please give us the time to ship a fix before you tell anyone else.

## Which versions get fixes

Only the latest released version of the app gets security fixes.

## In scope

- The app code in this repo: playback, the library scan, radio stream and playlist parsing, Android Auto, saved settings
- The build and release setup in this repo

A problem in a library we use should go to that library first. Tell us too, so that we can update.
