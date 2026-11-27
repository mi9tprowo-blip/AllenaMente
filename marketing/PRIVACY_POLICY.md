# Informativa sulla Privacy — AllenaMente

**Ultimo aggiornamento:** 27 Novembre 2026  
**Applicazione:** AllenaMente (`com.aistudio.allenamente.nqvtxr`)  
**Contatto Titolare del Trattamento:** mi9tprowo@gmail.com

---

## 1. Natura dell'Applicazione (Disclaimer Non-Clinico)
AllenaMente è un'applicazione di benessere cognitivo e intrattenimento ludico. I punteggi, i livelli, il Radar a 4 Assi, l'Indice di Velocità (pt), l'Indice di Armonia e i suggerimenti generati dal Coach AI hanno scopo esclusivamente ricreativo e informativo. L'app **non** fornisce diagnosi, pareri medici, valutazioni cliniche o terapie.

## 2. Dati Raccolti e Finalità
AllenaMente riduce al minimo i dati trattati:
* **Dati di Autenticazione (Google Sign-In / Firebase Authentication):** identificativo univoco utente (UID), nome visualizzato e indirizzo email, utilizzati esclusivamente per creare il tuo profilo privato e sincronizzare i progressi tra dispositivi.
* **Dati di Utilizzo e Progressi (Firebase Firestore):** punteggi dei 4 pilastri, serie attiva, livelli sbloccati, storico sintetico delle sessioni di gioco e preferenze di personalizzazione (tema, mascotte, audio, modalità Flusso Zen, fascia d'età selezionata).
* **Coach AI (Firebase AI Logic):** quando richiedi volontariamente un verdetto del Coach AI, vengono inviati in forma protetta solo i dati aggregati del tuo profilo ludico e delle ultime sessioni per generare il consiglio personalizzato. I dati non vengono venduti né usati per profilazione pubblicitaria.
* **Statistiche di Utilizzo Anonime (Google Analytics for Firebase — Opt-In Esplicito):** la raccolta è **disattivata di default** all'avvio dell'app e si attiva esclusivamente se selezioni *"Accetta"* nel banner di consenso (con bottone *"Rifiuta"* di pari evidenza) o attivi l'interruttore in *Impostazioni & Privacy*. La raccolta dell'Advertising ID è disabilitata a livello di app e nessun dato personale viene incluso negli eventi.
* **Modalità Provvisoria (Locale):** se accedi in modalità provvisoria locale, nessun dato viene salvato sul database cloud Firestore; i progressi restano temporaneamente in memoria sul dispositivo per la sola sessione di prova.
* **Minori di 14 anni (Art. 2-quinquies Codice Privacy):** l'utilizzo autonomo dell'app è riservato a chi ha almeno 14 anni; sotto i 14 anni è richiesto il consenso di un genitore o tutore legale.

## 3. Permessi del Dispositivo e Disiscrizione
* **Connessione Internet (`INTERNET`):** necessaria per l'accesso con Google, la sincronizzazione cloud e il Coach AI.
* **Notifiche (`POST_NOTIFICATIONS`, facoltativo):** richiesto solo se attivi esplicitamente il promemoria quotidiano nelle Impostazioni (disattivabile in 1 tocco). Nessuna newsletter o email promozionale viene inviata.
* **Nessun accesso a foto, archivio esterno, fotocamera, microfono o posizione.** L'esportazione della card immagine PNG utilizza esclusivamente la cache interna protetta dell'app tramite `FileProvider`.

## 4. Elenco Completo SDK Esterni, Condivisione e Pubblicità
AllenaMente **non** integra SDK pubblicitari, disattiva la raccolta dell'Advertising ID e **non** vende né cede i tuoi dati personali a broker o reti di marketing. Gli unici SDK integrati sono: **Firebase Authentication + Credential Manager**, **Cloud Firestore**, **Firebase AI Logic + App Check**, **Google Analytics for Firebase (opt-in)** e **Coil** (immagini profilo). L'infrastruttura cloud è protetta con crittografia in transito (HTTPS/TLS) e regole zero-trust per utente. Consulta anche i [Termini e Condizioni e Licenze](TERMS_AND_CONDITIONS.md).

## 5. Diritti dell'Utente (GDPR — UE 2016/679)
Direttamente all'interno dell'app (*Impostazioni & Privacy*) puoi esercitare in autonomia i tuoi diritti:
* **Portabilità dei dati:** esportazione immediata in formato **JSON** del profilo e di tutte le sessioni.
* **Diritto all'oblio (Cancellazione Integrale):** eliminazione definitiva del profilo, delle statistiche giornaliere e di tutte le sessioni dal database cloud in 2 passaggi confermati.

Per qualsiasi richiesta ulteriore relativa alla privacy puoi scrivere a: **mi9tprowo@gmail.com**.

---

# Privacy Policy (English Summary)

**Last Updated:** November 27, 2026 — **Contact:** mi9tprowo@gmail.com

AllenaMente is a non-clinical cognitive play and wellness app. When signing in with Google, we store only your Firebase Auth account identifier, display name, email, app preferences, and gameplay progress in isolated per-user Firebase Firestore storage. No ad trackers or third-party data brokers are used. You can export all your data as JSON or permanently delete all cloud and local records directly inside **Settings > Privacy & GDPR**.
