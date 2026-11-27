# VESTIA — Dossier Tecnico, Architetturale e Strategico Completo

**Progetto:** VESTIA — Atelier Digitale & Camerino Virtuale Intelligente  
**Data Audit & Aggiornamento:** 26 Novembre 2026  
**Versione Codebase:** `1.0.0+1` ([pubspec.yaml](pubspec.yaml)) — Enterprise Quiet Luxury Release  
**Piattaforme Supportate:** Flutter Web (WebAssembly + CanvasKit), Android (SDK 36), iOS  
**Stato Analisi Statica (`flutter analyze`):** `No issues found!` (0 errori, 0 warning)  
**Stato Suite di Test (`flutter test`):** **91 / 91 test superati (100% Pass Rate)**  

---

## Indice

1. [Visione, Posizionamento "Quiet Luxury" e Regole Auree](#1-visione-posizionamento-quiet-luxury-e-regole-auree)
2. [Architettura Software e Mappa Completa dei File](#2-architettura-software-e-mappa-completa-dei-file)
3. [Funzionamento Dettagliato dei Moduli Principali](#3-funzionamento-dettagliato-dei-moduli-principali)
4. [Storico dei Problemi Risolti in Corso d'Opera](#4-storico-dei-problemi-risolti-in-corso-dopera)
5. [Audit del Codice: Problemi Aperti, Debito Tecnico e Soluzioni Proposte](#5-audit-del-codice-problemi-aperti-debito-tecnico-e-soluzioni-proposte)
6. [Matrice Completa dei Test Automatizzati (91/91)](#6-matrice-completa-dei-test-automatizzati-9191)
7. [Analisi Strategica: Pro, Contro e Vantaggio Competitivo](#7-analisi-strategica-pro-contro-e-vantaggio-competitivo)
8. [Roadmap per la Produzione & Lancio Marketing](#8-roadmap-per-la-produzione--lancio-marketing)
9. [Guida Operativa, Sicurezza Web e Comandi](#9-guida-operativa-sicurezza-web-e-comandi)

---

## 1. Visione, Posizionamento "Quiet Luxury" e Regole Auree

**VESTIA** è un atelier digitale privato e guardaroba intelligente progettato per valorizzare i capi già posseduti dall'utente, eliminare l'indecisione quotidiana davanti all'armadio e guidare eventuali acquisti in negozio con criterio sartoriale.

A differenza delle applicazioni fashion-tech tradizionali (che funzionano come vetrine di affiliazione piene di prezzi, banner promozionali e metriche di consumo), VESTIA aderisce rigorosamente a quattro **guardrail di design e dominio**:

1. **Zero Metriche Consumistiche:** Assenza totale di prezzi, valute, carrelli, sconti o indicatori di "cost-per-wear". Il valore di un capo risiede nella fibra nobile (cashmere, seta naturale, lana vergine, lino delavé, popeline doppio ritorto), nel taglio e nell'armonia cromatica.
2. **Purezza Tipografica (Zero Emoji):** Nessuna emoji nella UI o nelle risposte editoriali dell'AI. Gerarchia basata esclusivamente su:
   * `Bodoni Moda` (`AppTypography.brandLogo`, `displayLarge`, `displayMedium`, `displaySmall`, `editorialItalic`) per titoli e logotipi ad alto contrasto.
   * `Inter` (`titleMedium`, `bodyLarge`, `bodyMedium`, `bodySmall`, `button`, `label`) per testi operativi, micro-label e leggibilità.
3. **Palette Cromatica Didone (`lib/core/constants/app_colors.dart`):**
   * Bianco Ottico / Superfici: `#FFFFFF` (`background`, `surface`) e `#F7F6F3` (`surfaceVariant` alabastro caldo)
   * Nero Inchiostro Architettonico: `#111111` (`textPrimary`, `borderFocused`)
   * Oro Champagne & Bronzo Toscano: `#B8976C` (`gold`), `#8A6B3D` (`goldDark`), `#F6F3EC` (`champagneMist`), `#E2DDD5` (`borderGold`)
   * Rosso Editoriale Couture (solo badge/stati intenzionali): `#D93829` (`accent`)
4. **Architettura Offline-First:** La fruizione di armadio, filtri, manichino e profilo stile avviene a latenza zero su database locale Drift (SQLite nativo su mobile, WebAssembly `sqlite3.wasm` + IndexedDB su Web).

---

## 2. Architettura Software e Mappa Completa dei File

Il codebase adotta una **Clean Architecture** stratificata (`core`, `data`, `domain`, `presentation`) con gestione reattiva dello stato tramite **Flutter Riverpod (`^2.5.1`)**:

### Struttura Completa del Repository

```text
VESTIA/
├── lib/
│   ├── main.dart                                      # Cold-boot immediato, dotenv load, init async Supabase & RevenueCat
│   ├── core/
│   │   ├── constants/
│   │   │   ├── app_colors.dart                        # Palette Didone, oro champagne, alabastro, nero ossidiana
│   │   │   ├── app_theme.dart                         # ThemeData globale Material 3 light couture
│   │   │   └── app_typography.dart                    # Scala tipografica Bodoni Moda + Inter
│   │   ├── services/
│   │   │   ├── background_removal_service.dart        # Cascade Rembg cloud -> ML Kit -> Saliency & Multi-Cluster locale
│   │   │   ├── fal_tryon_service.dart                 # FAL.ai IDM-VTON, hashing SHA-256, caching 2 stadi, ottimizzazione JPG
│   │   │   ├── fashn_api_service.dart                 # FASHN.ai v1/run + polling asincrono (fallback provider)
│   │   │   ├── replicate_tryon_service.dart           # Replicate cuuupid/idm-vton + proxy serverless + cache in-memory
│   │   │   ├── gemini_stylist_service.dart            # Personal Stylist (gemini-3.8-flash), auto-catalogazione, sanitizzazione
│   │   │   ├── monetization_service.dart              # Singleton RevenueCat (entitlement premium_access / vestia_vip) + sandbox
│   │   │   ├── storage_service.dart                   # Persistenza locale immagini originali / PNG scontornati (File vs Data URL Web)
│   │   │   ├── supabase_service.dart                  # Auth Google/Email/Anonima, Edge Functions proxy, sync crediti e capi
│   │   │   ├── wardrobe_sync_engine.dart              # Outbox pattern + riconciliazione bidirezionale con garments_cloud
│   │   │   └── weather_service.dart                   # Geolocator + Open-Meteo API con fallback Roma e codici WMO in italiano
│   │   └── utils/
│   │       └── color_harmony.dart                     # CIELAB D65, CIEDE2000 (ΔE00), Itten harmony, Chromotest incarnato/ITA
│   ├── data/
│   │   └── database/
│   │       ├── app_database.dart                      # Schema Drift (Clothes, Outfits, UserSettings) & atomic recordOutfitWorn
│   │       ├── app_database.g.dart                    # Codice generato Drift
│   │       └── connection/
│   │           ├── connection.dart                    # Conditional export (native vs web)
│   │           ├── connection_native.dart             # NativeDatabase SQLite
│   │           ├── connection_web.dart                # WasmDatabase ('vestia_db', sqlite3.wasm, drift_worker.js)
│   │           └── connection_unsupported.dart
│   ├── domain/
│   │   ├── engine/
│   │   │   ├── credits_engine.dart                    # Sblocco progressivo (3 prove gratis) + Pattern Saga reserve/commit/rollback
│   │   │   └── outfit_recommendation_engine.dart      # Layering role, formality score (1-10), meteo, Atelier Care, pre-filtro Stylist
│   │   └── models/
│   │       ├── garment_metadata.dart                  # Brand, fabric, care, thermalWeight, isArchived, cicli riposo fibre (24h/48h)
│   │       ├── outfit_recommendation.dart             # Modello raccomandazione con harmony score & thermalAdvice
│   │       ├── user_style_profile.dart                # 5 stagioni ArmocromiaSeason, 3 BodySilhouette, archetipi di stile
│   │       └── weather_data.dart                      # Meteo strutturato (isCold, isHot, isRainy, conditionDescription)
│   └── presentation/
│       ├── providers/
│       │   ├── app_providers.dart                     # Provider singleton servizi core + databaseProvider
│       │   ├── auth_provider.dart                     # AuthController (Google, Email Sign-In/Sign-Up, Sign-Out)
│       │   ├── credits_provider.dart                  # Stato crediti & integrazione CreditsEngine
│       │   ├── fitting_room_provider.dart             # FittingViewMode (3D/FlatLay/Neural), 5 layer abbigliamento, Try-On pipeline
│       │   ├── outfit_provider.dart                   # Raccomandazione giornaliera e archivio outfit salvati
│       │   ├── style_profile_provider.dart            # Stato armocromia, silhouette, gender manichino e risultato Chromotest
│       │   ├── stylist_provider.dart                  # StylistChatNotifier con history conversazionale e allegati immagine
│       │   ├── wardrobe_provider.dart                 # Stream capi Drift, filtri categoria/Archivio/Wishlist, seeding capsule
│       │   └── weather_provider.dart                  # FutureProvider meteo locale
│       ├── screens/
│       │   ├── home_screen.dart                       # Shell con SharedAxisTabStack, bottom dock glass, parsing URL ?tab=&modal=
│       │   ├── wardrobe/wardrobe_screen.dart          # Griglia capi, filtri, Travel Concierge, Wardrobe Insights, Demo Capsule
│       │   ├── ingestion/ingestion_screen.dart        # Acquisizione foto, split slider prima/dopo, sensibilità scontorno, AI Vision
│       │   ├── fitting_room/
│       │   │   ├── fitting_room_screen.dart           # Camerino Tri-Mode, rotazione orbitale con inerzia, export PNG 3x
│       │   │   └── mannequin_painter.dart             # CustomPainter manichino anatomico uomo/donna da atelier
│       │   ├── stylist/stylist_screen.dart            # Chat AI Stylist, badge verdetto acquisto, renderer tipografico no-asterischi
│       │   ├── daily_look/daily_look_screen.dart      # Editorial Look del Giorno basato su meteo e armonia cromatica
│       │   └── paywall/paywall_modal.dart             # Piani VIP e pacchetti crediti via MonetizationService
│       └── widgets/                                   # 13 widget modulari (account, detail modal, insights, travel capsule, ecc.)
├── supabase/
│   ├── schema.sql                                     # Schema PostgreSQL (profiles, deduct_credit, garments_cloud, try_on_cache, RLS)
│   └── functions/
│       ├── gemini-stylist/index.ts                    # Edge Function Deno proxy per Gemini 3.8 Flash con auth JWT & anti-asterischi
│       └── try-on/index.ts                            # Edge Function Deno proxy FAL.ai IDM-VTON con cache SHA-256 & Saga crediti
├── test/                                              # 14 file di test automatizzati (91 test totali)
├── pubspec.yaml                                       # Dipendenze Flutter/Dart e configurazione assets
└── serve_web.py                                       # Server HTTP Python con COOP/COEP credentialless, CORP cross-origin, MIME WASM/MD
```

---

## 3. Funzionamento Dettagliato dei Moduli Principali

### 3.1 Persistenza Offline-First (`app_database.dart` & `connection_web.dart`)
* **Database Relazionale Drift (`schemaVersion => 1`):**
  * Tabella `Clothes`: memorizza `localImagePath`, `maskImagePath`, `category` (`Top`, `Bottom`, `Shoes`, `Outerwear`, `Accessories`), `subcategory`, `colorHex`, `season`, `formality`, `wearCount`, `createdAt`, oltre a `remoteId` usato anche come contenitore JSON per `GarmentMetadata` (tessuto, brand, note sartoriali, `isArchived`, `lastWorn`, `thermalWeight`).
  * Tabella `Outfits`: salva composizioni curate (`clothesIds` JSON) e condizioni meteo.
  * Tabella `UserSettings`: traccia `dailyStreak`, `lastLoginDate`, `cachedCredits`, `isPremium`.
* **Runtime WebAssembly:** Su browser carica `sqlite3.wasm` e `drift_worker.js` tramite `WasmDatabase.open('vestia_db', ...)` mantenendo persistenza locale in IndexedDB.
* **Atelier Care in DB:** `recordGarmentWorn` e `recordOutfitWorn(List<int> clotheIds)` aggiornano atomicamente `wearCount` e `lastWorn` per attivare il calcolo di riposo delle fibre nobili.

### 3.2 Acquisizione Capi, Scontorno e Auto-Catalogazione (`IngestionScreen`)
1. **Scontorno a 3 Livelli (`DefaultBackgroundRemovalService`):**
   * *Livello 1 (Cloud AI, se configurato):* chiama `REPLICATE_PROXY_URL` o `https://api.replicate.com/v1/models/cjwbw/rembg/predictions`.
   * *Livello 2 (On-Device Mobile):* `google_mlkit_subject_segmentation` (`SubjectSegmenter`) estrae il soggetto o la confidence mask.
   * *Livello 3 (Locale Multiplatform / Web fallback):* motore *High-Accuracy Saliency & Multi-Cluster Color Segmentation* on-device (`_smartLocalSegmentation`) con parametro `sensitivity` regolabile dall'utente via `SplitImageSlider`, pensato anche per gestire aperture interne come scollature e giromanica di gilet.
2. **Auto-Catalogazione Zero-Click (`GeminiStylistService.analyzeGarment`):**
   * Invia i byte dell'immagine al modello vision (`gemini-3.8-flash` -> `gemini-flash-latest` -> `gemini-3.5-flash`) chiedendo un JSON rigido con `category`, `subcategory`, `colorHex`, `colorName`, `season`, `formality`, `fabric`, `thermalWeight`, `editorialDescription`, `careGuide` ed `estimatedLighting` (`neutral_studio`, `warm_indoor`, `cool_fluorescent`).

### 3.3 Scienza del Colore, CIELAB D65 & Chromotest (`color_harmony.dart`)
* **Conversione Percettiva (`colorToLab` / `labToColor`):** Converte sRGB nello spazio uniforme CIE $L^*a^*b^*$ rispetto all'illuminante standard D65.
* **Formula CIEDE2000 (`ciede2000`):** Implementa il calcolo ufficiale completo della distanza cromatica percettiva $\Delta E_{00}$ per valutare abbinamenti monocromatici, analoghi, complementari, triadici o su base neutra (`HarmonyResult`).
* **Calibrazione White Balance & Chromotest:** Estrae le metriche dell'incarnato (`lightnessL`, `chromaC`, `hueAngle`, `itaAngle`) classificando il sottotono in `SkinSubtone` (`cool`, `warm`, `neutral`) e mappandolo sulle stagioni `ArmocromiaSeason` (`winter`, `autumn`, `spring`, `summer`, `universal`).

### 3.4 Motore Sartoriale & Atelier Care (`OutfitRecommendationEngine`)
* **Stratificazione (`GarmentLayerRole`):** Classifica automaticamente in `baseLayer`, `midLayer`, `outerLayer`, `trousers`, `footwear`, `accessory`.
* **Matrice di Formalità (1–10):** `calculateFormalityScore` assegna 10 a smoking/tuxedo/frac, 9 ad abiti completi, 8 a blazer/trench, 7 a camicie sartoriali/derby, 5 a cardigan/chino/polo/mocassini, 2 a shorts/infradito. `isFormalityCompatible` scarta abbinamenti con scarto $> 4$ punti.
* **Cicli di Riposo delle Fibre (`GarmentMetadata`):**
  * Cashmere, lana vergine, vigogna, alpaca, mohair, Super 100s+: **48 ore di riposo**.
  * Seta, lino, merino: **24 ore di riposo**.
  * Tutti i capi in riposo o archiviati (`isArchived`) vengono esclusi dallo Shuffle, dal Look del Giorno e dal pre-filtro dello Stylist (`filterCandidateGarmentsForStylist`).
* **Compatibilità Termica:** Incrocia temperatura, pioggia e `thermalWeight` usando `WeatherService` (Geolocator + Open-Meteo API).

### 3.5 Camerino Virtuale Tri-Mode & Try-On Neurale (`FittingRoomScreen` & `FalTryOnService`)
* **Modalità 1 — Manichino 3D Sartoriale:** Scultura neutra disegnata tramite `mannequin_painter.dart` (uomo/donna) con rotazione orbitale interattiva, animazione ad inerzia, pinch-to-zoom e sovrapposizione fino a 5 layer fisici.
* **Modalità 2 — Tavolo Sartoriale (Flat-Lay Moodboard):** Vista zenitale editoriale dei capi selezionati su sfondo alabastro/champagne con palette armocromatica ed esportazione HD (`pixelRatio: 3.0`) + condivisione (`EditorialShareModal`).
* **Modalità 3 — Try-On Neurale AI:** Pipeline con caching SHA-256 deterministico, ridimensionamento ottimizzato fino a `768x1024` JPG qualità 85, supporto fino a IDM-VTON su FAL.ai (`queue.fal.run/fal-ai/idm-vton`), fallback FASHN.ai e Replicate (`cuuupid/idm-vton`), o modalità dimostrativa simulata quando le chiavi non sono configurate.

### 3.6 Atelier Personal Stylist (`GeminiStylistService` & `StylistScreen`)
* **Modelli & Multi-Turn:** Utilizza `gemini-3.8-flash` (fallback `gemini-flash-latest`, `gemini-3.5-flash`) inviando il profilo stile utente, il contesto meteo, fino a 25 capi pre-filtrati del guardaroba e fino a 8 turni recenti di conversazione con alternanza rigorosa `user` / `model`.
* **Verdetti in Negozio:** Analizza foto scattate dal vivo e restituisce badge strutturati (`CONSIGLIATO`, `DA VALUTARE`, `NON NECESSARIO`).
* **Sanitizzazione & Rendering Editoriale:** `sanitizeStylistText` elimina qualsiasi marcatore markdown (`***`, `**`, `*`, `###`) convertendo i bullet in punti puliti `• `; nella UI `_buildFormattedStylistText` renderizza ogni punto elenco con pallino dorato (`AppColors.goldDark`) e titolo in grassetto nativo.
* **Motore Locale Offline:** Riconosce saluti, singoli indumenti (pantaloncini, blazer, camicia, gonna, ecc.), colori, Gap Analysis, armonia cromatica e occasioni d'uso generando consigli su misura anche senza connettività.

### 3.7 Crediti, Monetizzazione & Sincronizzazione (`CreditsEngine`, `MonetizationService`, `WardrobeSyncEngine`)
* **Sblocco Progressivo Anti-Spreco (`CreditsEngine`):** Fino a 3 prove gratuite sbloccabili progressivamente (1ª prova con $\ge 3$ capi catalogati, 2ª al 2º giorno consecutivo di streak, 3ª al 3º giorno).
* **Pattern Saga (`executeTryOnSaga`):** `reserveCredit` decrementa in modo provvisorio; a immagine generata `commitCredit` conferma la transazione; su errore/eccezione `rollbackCredit` ripristina il credito senza perdite per l'utente.
* **Monetizzazione (`MonetizationService`):** Integrazione RevenueCat per iOS/Android (entitlement `premium_access` / `vestia_vip`, pacchetti da 10 o 25 crediti) e modalità sandbox/local-first su Web o in assenza di chiavi store.

---

## 4. Storico dei Problemi Risolti in Corso d'Opera

| Area | Problema Iniziale | Soluzione Implementata | Verifica |
| :--- | :--- | :--- | :--- |
| **Chiamate Gemini AI** | Gli endpoint `gemini-2.5-flash` e `gemini-2.0-flash` restituivano HTTP `404 NOT_FOUND`, facendo cadere ogni messaggio sul fallback locale generico sul meteo. | Migrazione a `gemini-3.8-flash` / `gemini-flash-latest` / `gemini-3.5-flash` + supporto cronologia multi-turno (`conversationHistory`) + NLP offline potenziato per capi, colori e saluti. | Test dal vivo su API + `gemini_stylist_test.dart` (9/9 test passati). |
| **Formattazione Bot Stylist** | Le risposte contenevano `**`, `***` e `###` visibili in chiaro nella chat. | Direttiva esplicita anti-asterischi nel `systemInstruction`, metodo `sanitizeStylistText` lato servizio/Edge Function e widget `_buildFormattedStylistText` in UI. | Test dedicato `sanitizeStylistText` + build Web rilasciata. |
| **Blocco Immagini Web (COOP/COEP)** | `COEP: require-corp` necessario per `SharedArrayBuffer` / `WasmDatabase` impediva il caricamento di asset AI remoti privi di header CORP. | Configurazione `Cross-Origin-Embedder-Policy: credentialless` e `Cross-Origin-Resource-Policy: cross-origin` in [serve_web.py](serve_web.py) e nella Edge Function `try-on`. | Header verificati sul server Web locale e via tunnel. |
| **Distorsione Cromatica Luce Calda** | Il campionamento in solo spazio RGB falsava i capi chiari fotografati sotto lampade calde (2700K). | Conversione in spazio percettivo CIELAB D65, calcolo distanze `ciede2000` e correzione white-balance guidata da `estimatedLighting` di Gemini Vision. | `color_harmony_cielab_test.dart` (13/13 test passati). |
| **Perdita Crediti Try-On & Ricalcoli** | Un errore nello Stadio 2 o la semplice sostituzione della giacca sprecava crediti e tempo GPU. | Cache deterministica SHA-256 sullo Stadio 1 + `CreditsEngine.executeTryOnSaga` con rollback compensativo automatico. | `tryon_saga_resilience_test.dart` (7/7 test passati). |
| **Tunnel Cloudflare IPv6** | `cloudflared` falliva il precheck HTTP/2 su interfacce IPv6 locali. | Avvio con `--edge-ip-version 4 --no-autoupdate` per stabilire connessione QUIC verso edge Cloudflare (`fco01`). | HTTP/2 200 OK verificato su dominio `trycloudflare.com`. |

---

## 5. Audit del Codice: Problemi Aperti, Debito Tecnico e Soluzioni Proposte

Dall'ispezione diretta dei sorgenti alla data attuale emergono **6 criticità tecniche reali** da risolvere prima del rilascio in produzione su larga scala:

### 5.1 Bug nel Two-Pass Chaining di `FalTryOnService` (Critico Funzionale)
* **File e righe:** [lib/core/services/fal_tryon_service.dart](lib/core/services/fal_tryon_service.dart#L383-L437)
* **Problema rilevato:** Nel metodo `runTwoPassTryOn`, lo Stadio 1 genera `pass1` (foto utente con il pantalone applicato) restituendo un URL remoto (`pass1.outputImageUrl`), ma nello Stadio 2 (riga 422) `runVirtualTryOn` viene richiamato passando ancora `modelBytes: modelBytes` (la foto originale nuda/iniziale dell'utente!) anziché scaricare i byte risultanti da `pass1.outputImageUrl`. Di conseguenza, in modalità live con API key attiva, lo Stadio 2 sovrascrive la foto originale perdendo il pantalone applicato allo Stadio 1.
* **Soluzione proposta:**
  1. Aggiungere il parametro opzionale `String? modelImageUrl` a `runVirtualTryOn` (oppure un helper `_resolveOutputImageBytes(String url)` che scarica i byte di `pass1.outputImageUrl` via `http.get`, gestendo anche il caso di simulazione).
  2. Passare i byte dell'immagine prodotta dallo Stadio 1 come input `modelBytes` dello Stadio 2, mantenendo la cache su `stage1Hash`.

### 5.2 Disallineamento Schema Database Supabase: `profiles` vs `user_credits` (Critico Backend)
* **File e righe:** [supabase/schema.sql](supabase/schema.sql#L6-L60) vs [supabase/functions/try-on/index.ts](supabase/functions/try-on/index.ts#L69-L140) e [lib/core/services/supabase_service.dart](lib/core/services/supabase_service.dart#L253-L275)
* **Problema rilevato:**
  * `schema.sql` crea la tabella `public.profiles` (con chiave primaria `id`, colonne `credits` e `is_vip`) e definisce la RPC `deduct_credit(user_uuid, amount)` su `public.profiles`.
  * Al contrario, sia `supabase/functions/try-on/index.ts` (righe 70 e 137) sia `SupabaseService` (`syncCreditsToCloud` e `fetchCloudCredits`) interrogano una tabella `user_credits` con colonna `user_id` non definita in `schema.sql`. Inoltre `try-on/index.ts` controlla `currentBalance <= 0` prima della chiamata a FAL.ai senza esentare gli utenti con `is_vip = true` e senza effettuare una prenotazione atomica (`PENDING`) prima della richiesta remota (esponendo a race condition su doppi click simultanei).
* **Soluzione proposta:**
  1. Unificare tutto su `public.profiles` (oppure aggiungere una tabella dedicata `public.credit_transactions` + vista/tabella `user_credits` in `schema.sql`).
  2. Introdurre tre RPC atomiche su Postgres: `reserve_credit_tx(user_uuid, tx_id)`, `commit_credit_tx(tx_id)` e `rollback_credit_tx(tx_id)` con controllo `is_vip` integrato e lock `FOR UPDATE`.
  3. Aggiornare `SupabaseService` e `try-on/index.ts` affinché leggano e scrivano sullo stesso schema verificato.

### 5.3 Esposizione di API Key Client-Side nel Bundle `.env` (Critico Sicurezza Produzione)
* **File e righe:** [pubspec.yaml](pubspec.yaml#L56-L57), [lib/core/services/gemini_stylist_service.dart](lib/core/services/gemini_stylist_service.dart#L63-L64), [lib/core/services/fal_tryon_service.dart](lib/core/services/fal_tryon_service.dart#L65), [lib/core/services/background_removal_service.dart](lib/core/services/background_removal_service.dart#L70)
* **Problema rilevato:** `.env` è incluso negli asset compilati (`assets: - .env`), pertanto in `build/web/assets/.env` è accessibile pubblicamente via HTTP qualsiasi chiave configurata lato client (attualmente `GEMINI_API_KEY` è presente in chiaro in `.env`, mentre `SUPABASE_URL`, `FASHN_API_KEY` e `REPLICATE_API_TOKEN` hanno placeholder). Su produzione Web questo espone la chiave a estrazione e abuso.
* **Soluzione proposta:**
  1. Configurare `SUPABASE_URL` e `SUPABASE_ANON_KEY` reali nel client.
  2. Spostare `GEMINI_API_KEY`, `FAL_KEY`, `REPLICATE_API_TOKEN` e `FASHN_API_KEY` esclusivamente nei **Supabase Secrets** server-side.
  3. Aggiornare `GeminiStylistService`, `FalTryOnService` e `DefaultBackgroundRemovalService` affinché utilizzino prioritariamente `SupabaseService.invokeEdgeFunction(...)` (completando anche i proxy per `analyzeGarment` e `remove_bg`), rimuovendo `.env` dagli asset pubblici di produzione o lasciandovi solo `SUPABASE_URL` e `SUPABASE_ANON_KEY`.

### 5.4 Volatilità in RAM dell'Outbox di Sincronizzazione (`WardrobeSyncEngine`)
* **File e righe:** [lib/core/services/wardrobe_sync_engine.dart](lib/core/services/wardrobe_sync_engine.dart#L37-L57)
* **Problema rilevato:** La coda `_outbox` è una semplice `List<SyncOutboxItem>` mantenuta in memoria RAM e non viene invocata automaticamente al salvataggio/eliminazione dei capi. Se l'utente chiude l'app o aggiorna la scheda del browser mentre è offline, le operazioni in coda vanno perse.
* **Soluzione proposta:**
  1. Portare `AppDatabase.schemaVersion` da `1` a `2` aggiungendo la `MigrationStrategy` e una tabella `SyncOutbox` persistente su SQLite/WASM.
  2. Collegare `WardrobeController` (`wardrobe_provider.dart`) a `WardrobeSyncEngine` per accodare automaticamente `insert`, `update`, `delete` e `archive` ed eseguire `flushOutbox()` al ripristino della connettività o all'avvio sessione.

### 5.5 Archiviazione Base64 Inline su Flutter Web (`StorageService`)
* **File e righe:** [lib/core/services/storage_service.dart](lib/core/services/storage_service.dart#L25-L55)
* **Problema rilevato:** Su Web, `saveOriginalImage` e `saveCutoutPng` codificano l'intero file come `data:image/...;base64,...` salvandolo direttamente nelle colonne testo di Drift (IndexedDB WASM). Con 40–100 capi ad alta risoluzione il database locale cresce rapidamente a decine/centinaia di MB, rallentando `getAllClothes()` e lo streaming in UI.
* **Soluzione proposta:**
  1. Ridimensionare e comprimere i cutout Web (es. max `900px` lato lungo) prima della codifica locale.
  2. Quando `SupabaseService` è attivo, caricare i PNG scontornati su un bucket privato **Supabase Storage** (`garments/<user_id>/<garment_id>.png`) salvando in `garments_cloud.image_url` un percorso o URL firmato, e usando il blob locale solo come cache offline temporanea.

### 5.6 Relay Immagini Try-On & Qualità Posa Fotografica
* **File e righe:** [supabase/functions/try-on/index.ts](supabase/functions/try-on/index.ts#L116-L154), [lib/presentation/providers/fitting_room_provider.dart](lib/presentation/providers/fitting_room_provider.dart)
* **Problema rilevato:**
  1. `try-on/index.ts` restituisce direttamente `resultUrl` generato dai server FAL.ai; se il CDN esterno modifica gli header CORS/CORP o fa scadere l'URL temporaneo, l'utente Web perde l'immagine generata anche se salvata in `try_on_cache`.
  2. `FittingRoomScreen` non effettua un pre-check automatico sulla qualità della foto utente (inquadratura a busto vs figura intera, luce troppo scura) prima di consumare un job GPU da 6–12 secondi.
* **Soluzione proposta:**
  1. Nella Edge Function `try-on`, scaricare il buffer dell'immagine risultante da FAL.ai, caricarlo su **Supabase Storage** privato e restituire un URL controllato con header CORP compatibili.
  2. Riutilizzare una verifica rapida on-device (o pre-check Gemini Flash Lite) sulla foto del modello per avvisare l'utente se la figura non è frontale/intera prima di inviare la richiesta a FAL.ai.

---

## 6. Matrice Completa dei Test Automatizzati (91/91)

Tutti i **91 test** eseguiti con `flutter test` il **26 Novembre 2026** sono passati al **100%**:

| File di Test | Quantità | Ambito Verificato |
| :--- | :---: | :--- |
| `test/color_harmony_cielab_test.dart` | 13 | Spazio colore CIE $L^*a^*b^*$ D65, formula ufficiale `ciede2000` ($\Delta E_{00}$), neutri, white balance studio/warm/cool. |
| `test/auth_test.dart` | 12 | `SupabaseService` & `AuthController`: Google Sign-In, login/registrazione email con validazioni, fallback locale, sign-out. |
| `test/gemini_stylist_test.dart` | 9 | Caricamento chiave `.env`, chiamate live a Gemini 3.8 Flash, cronologia multi-turno, sanificazione asterischi `sanitizeStylistText`, fallback sartoriale su capi/colori ("pantaloncini viola") e saluti. |
| `test/atelier_care_and_layering_test.dart` | 8 | Riposo fibre (48h cashmere, 24h seta), esclusione capi in riposo/archiviati, formality matrix (1–10), `GarmentLayerRole`, `recordOutfitWorn` su Drift. |
| `test/flatlay_and_chromotest_test.dart` | 7 | Stati `FittingViewMode` (`mannequin3D`, `flatLayMoodboard`, `neuralTryOn`) e `analyzeSkinTone` per sottotoni caldi, freddi, neutri. |
| `test/replicate_tryon_test.dart` | 7 | `ReplicateTryOnService`: simulazione, validazione input, cache a costo 0, gestione token runtime e proxy URL. |
| `test/tryon_saga_resilience_test.dart` | 7 | Hash deterministico SHA-256 Stadio 1, riuso cache bottom, Saga `reserveCredit`/`commitCredit`/`rollbackCredit` su fallimenti rete. |
| `test/fal_tryon_test.dart` | 6 | `FalTryOnService`: configurazione runtime key, simulazione, mapping categorie, Two-Pass hashing e caching. |
| `test/monetization_test.dart` | 5 | Singleton `MonetizationService`, stato abbonamento, acquisto VIP e pacchetti crediti tier 1/2 in sandbox, restore. |
| `test/wardrobe_sync_engine_test.dart` | 5 | Outbox vuoto iniziale, accodamento insert, flush verso cloud, persistenza in coda offline e `reconcileFromCloud`. |
| `test/widget_test.dart` | 5 | Smoke test UI navigazione principale (`HomeScreen`), tab bar, drawer e rendering schermate core. |
| `test/background_removal_test.dart` | 3 | Input vuoti/corrotti, segmentazione su sfondo solido verso alpha trasparente, preservazione sagoma su capi campione. |
| `test/tutorial_test.dart` | 3 | `kTutorialChapters` (4 capitoli), completezza metadati e rendering widget `EditorialTutorialModal`. |
| `test/gilet_cutout_test.dart` | 1 | Segmentazione complessa su sagoma gilet con aperture interne e scollo puliti. |
| **TOTALE VERIFICATO** | **91** | **100% Superati (`All tests passed!`) — `flutter analyze`: 0 issues** |

---

## 7. Analisi Strategica: Pro, Contro e Vantaggio Competitivo

### Punti di Forza (Pro)
* **Identità e Posizionamento Distintivo:** L'estetica editoriale Didone (`Bodoni Moda` / `Inter`) priva di prezzi ed emoji differenzia immediatamente VESTIA da app consumer o marketplace mascherati.
* **Efficienza Economica dell'Inferenza AI:** Doppio livello di caching SHA-256 lato client + tabella `try_on_cache` lato database azzerano i costi di ricalcolo su combinazioni già provate (< $0.015 su IDM-VTON FAL.ai per nuove generazioni).
* **Ingegneria Difensiva:** Pattern Saga per i crediti, fallimenti graziosi con fallback locale in ogni servizio critico (Stylist, Background Removal, Try-On, Weather, Auth, RevenueCat) e pre-filtraggio deterministico del guardaroba per contenere latenza e token di Gemini.
* **Domain Modeling Sartoriale Autentico:** Poche app implementano riposo biologico delle fibre naturali, scala di formalità dress-code e distanza cromatica CIEDE2000 su illuminante D65.

### Trade-Off e Criticità Attuali (Contro)
* **Dipendenza da Rete per le Funzioni Core Più Spettacolari:** Scontorno basato su Rembg cloud, risposte creative di Gemini 3.8 Flash e Try-On IDM-VTON richiedono rete e crediti API attivi (i fallback locali coprono comunque usabilità base e dimostrazione).
* **Latenza Remota del Try-On Neurale (6–12s):** Pur mitigata da shimmer editoriali, ottimizzazione immagini `768x1024` e caching, la generazione su GPU remota resta asincrona e non istantanea come il manichino 2D/3D locale.
* **Prontezza Backend Parziale:** I servizi cloud (`SupabaseService`, Edge Functions e RevenueCat) sono progettati con interfacce pulite ma attualmente operano in fallback/sandbox locale finché non vengono configurati i progetti Supabase/RevenueCat/FAL.ai di produzione e risolti i 3 bug backend elencati nella Sezione 5.

---

## 8. Roadmap per la Produzione & Lancio Marketing

### Fase 1 — Hardening Tecnico Pre-Lancio (Priorità Alta)
1. Correggere il bug di chaining del Pass 2 in `FalTryOnService.runTwoPassTryOn` affinché passi l'output di Stage 1 come immagine di partenza per Stage 2.
2. Allineare `supabase/schema.sql`, `try-on/index.ts` e `SupabaseService` sullo schema unificato `public.profiles` + RPC Saga atomiche.
3. Caricare i Secrets (`GEMINI_API_KEY`, `FAL_KEY`, `REPLICATE_API_TOKEN`) su Supabase Edge Functions e rimuovere le API key sensibili da `.env` prima della pubblicazione Web/Store.
4. Implementare tabella `SyncOutbox` in Drift (`schemaVersion: 2`) e relay delle immagini Try-On su bucket Supabase Storage privato.

### Fase 2 — Asset Visivi, Brand & Campagna Quiet Luxury
* **Brand Identity & Logo:** Monogramma `V` in oro anticato (`#B8976C`) su fondo ossidiana (`#111111`) con micro-bordatura `0.8px` e logotipo `Bodoni Moda` a tracciatura larga.
* **Produzione Video & Screen Capture:**
  * *Hero Teaser (30s):* Macro su tessuti nobili -> scansione e scontorno in 0.4s -> rotazione 360° sul manichino Bonaveri -> dissolvenza su Try-On Neurale reale.
  * *Reels Use-Case (15s):* Consulenza istantanea Stylist AI basata su meteo reale e armocromia CIELAB.
* **Go-To-Market ("The Founders Club"):** Lista d'attesa ad invito limitato e seeding presso micro-creator di sartoria italiana, armocromia e cultura Quiet Luxury.

---

## 9. Guida Operativa, Sicurezza Web e Comandi

### Verifica Qualità Codebase
```bash
export PATH="/home/chris/flutter/bin:/home/chris/.local/bin:$PATH"
flutter analyze
flutter test
```

### Compilazione WebAssembly & Nota di Sicurezza
```bash
flutter build web --wasm --release
```
> **Nota di Sicurezza:** I documenti di architettura interna (`PROGETTO_VESTIA.txt` o il presente `.md`) non devono restare esposti permanentemente nella cartella pubblica `build/web/` dopo il download da parte del proprietario, per evitare di fornire a terzi la mappa dettagliata di endpoint, tabelle e servizi di pagamento. Per rimuovere il file dopo averlo scaricato:
> ```bash
> rm -f /home/chris/VESTIA/build/web/VESTIA_DOSSIER_COMPLETO.md
> ```

### Server Locale & Tunnel HTTPS
```bash
python3 /home/chris/VESTIA/serve_web.py
/home/chris/.local/bin/cloudflared tunnel --edge-ip-version 4 --no-autoupdate --url http://localhost:8080
```
