# Privacy Policy — RustSensei

**Last updated:** September 10, 2026

## Overview

RustSensei is an offline-first Rust programming tutor for Android, published by Syltech AI Systems, Inc. The app is designed with privacy as a core principle: the AI tutor, the book, exercises, quizzes, and all your progress run entirely on your device.

Two optional features send data off the device, and only when you ask them to. They are described in full under [Network Usage](#network-usage).

## Data Collection

**RustSensei does not collect, transmit, or share any personal data.**

- No analytics or tracking
- No advertising SDKs
- No user accounts or registration
- No telemetry or crash reporting sent to external servers

## Data Stored on Device

The following data is stored locally on your device and is never uploaded anywhere:

- **Chat conversations** — your messages and AI responses, stored in a local database
- **Learning progress** — chapters read, exercises completed, quiz scores, study streaks
- **Flashcard data** — spaced repetition scheduling and review history
- **Notes** — any notes you write while reading
- **App preferences** — theme, inference settings, selected model

Your **saved exercise and Playground code** is also stored locally. It stays on the device unless you tap Compile or Run tests, which sends that code to the Rust Playground as described under [Network Usage](#network-usage).

All local data can be deleted at any time from the Settings screen within the app.

## Network Usage

RustSensei makes network requests in exactly three situations. There are no others.

### 1. Downloading the AI model (one time)

The on-device language model (~1.2 GB) is downloaded from [Hugging Face](https://huggingface.co). This is a plain file download. No personal data is sent with it. After the model is downloaded the tutor works with no connection at all.

### 2. Compile (Playground)

If you tap **Compile** in the Playground, the Rust source code currently in the editor is sent over HTTPS to the official Rust Playground service at `play.rust-lang.org`, operated by the Rust Project. It is compiled there by real `rustc` and the compiler output is returned to your device.

### 3. Run tests (Exercises)

If you tap **Run tests** on an exercise, your solution code and that exercise's test code are sent over HTTPS to the same Rust Playground service and the test output is returned.

**What is sent:** only the code in the editor at that moment, plus fixed compiler options (channel, edition, mode). No account identifier, no device identifier, no other app data.

**When it is sent:** only when you tap Compile or Run tests. The first time you do, RustSensei asks for your explicit consent and explains where the code is going. If you decline, both features stay off and everything else keeps working, including the simulated on-device runner in the Playground.

**Who receives it:** the Rust Project, under its own terms. See the [Rust Playground](https://play.rust-lang.org) and [rust-lang.org policies](https://www.rust-lang.org/policies/privacy).

RustSensei does not upload your chat history, notes, progress, flashcards, or settings to any server, ever.

## AI Processing

The AI tutor runs a language model entirely on your device using Google's LiteRT framework with GPU acceleration. Your questions and the AI's responses are processed locally and are never sent to any external service.

## Third-Party Services

RustSensei does not integrate with any third-party analytics, advertising, or data-brokering services.

Two external services are contacted, both described above:

| Service | Purpose | What it receives |
|---|---|---|
| [Hugging Face](https://huggingface.co) | One-time model download | Nothing beyond a standard file request |
| [Rust Playground](https://play.rust-lang.org) (the Rust Project) | Compile and Run tests, only on your explicit action | The Rust code in the editor at that moment |

## Children's Privacy

RustSensei does not knowingly collect any information from children under 13. The app contains no age-restricted content.

## Changes to This Policy

If this privacy policy is updated, the changes will be reflected in this document with an updated date.

## Contact

RustSensei is a product of **Syltech AI Systems, Inc.**, Kitchener, Ontario, Canada.

If you have questions about this privacy policy:

- Web: [syltechai.dev](https://syltechai.dev/)
- Issues: [github.com/SyltechAI/RustSensei/issues](https://github.com/SyltechAI/RustSensei/issues)
- LinkedIn: [Syltech AI Systems](https://www.linkedin.com/company/syltech-ai-systems-inc/)
