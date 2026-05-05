# Training Backend — Technical Spec

**Phase**: 4 (future)
**Language**: Python 3.11+
**Framework**: Flask
**Purpose**: Accept 20 audio samples from the companion app, train a custom openWakeWord
model, return a .tflite file

## Status
- [ ] Not yet started — Phase 4

## Planned architecture

```
POST /train
  body: { wake_word: "Hallo Oma", samples: [base64_wav, ...] }  (min 20 samples)
  → runs openWakeWord training pipeline
  → returns { model_url: "/models/{id}.tflite" }

GET /models/{id}.tflite
  → returns trained model file
```

## Self-hostable
The backend should be deployable with `docker compose up` by any technical user.
It is not a shared cloud service — each family runs their own instance or uses
the community-hosted instance at a URL TBD.

## openWakeWord training reference
https://github.com/dscripka/openWakeWord
See `notebooks/` in that repo for the training pipeline.

## Community model library (Phase 4b)
A simple GitHub-hosted index (JSON file) of contributed wake word models,
with name, language, and download URL. Discovery and installation happen
inside the companion app settings screen.
