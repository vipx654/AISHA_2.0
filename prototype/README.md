# AISHA Core Engine — Python Prototype

Runs the LOCKED pipeline from the spec (§5) as a console app so mood/bond/memory
behaviour can be observed and tuned before the Android port.

```
python3 cli.py                     # interactive chat + commands
python3 simulate.py day            # 24-hour simulation (spec §24)
python3 simulate.py months --profile interactive --days 180
python3 simulate.py months --profile busy
python3 simulate.py months --profile ignored
python3 simulate.py storage --days 40
```

Optional real LLM: `GEMINI_API_KEY=... python3 cli.py` (falls back to MockLLM offline).

Module map mirrors the Android package 1:1:
models / identity / mood / bond / decision / presence / safety / prompt_builder /
llm / validator / memory / storage / engine.

NOTE: `storage.py` uses zlib+base64+SHA-256 (prototype-grade). The Android app
must use AES via Android Keystore through `security.EncryptionService`.
