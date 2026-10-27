# Brandkit & Identità Visiva — Assistente MeDiTech Mobile
### Istituto MeDiTech • Dipartimento Tecnologie Innovative • SUPSI (Lugano, Svizzera)

---

## 1. Fondamenti del Marchio & Posizionamento
L'**Istituto MeDiTech** (Istituto di tecnologie digitali per cure sanitarie personalizzate) della **SUPSI** opera all'intersezione tra ingegneria biomedica, intelligenza artificiale e salute di precisione.

L'applicazione **Assistente MeDiTech Mobile** incarna i quattro pilastri della **Medicina P4**:
1. **Preventiva**: Monitoraggio continuo e predizione precoce di anomalie nei biosegnali.
2. **Predittiva**: Modelli di intelligenza artificiale on-device e analisi statistica clinica.
3. **Personalizzata**: Assistenza su misura per clinici, ricercatori e pazienti.
4. **Partecipativa**: Comunicazione trasparente, telemedicina e gestione integrata (agenda, email cliniche, protocolli).

---

## 2. Palette Colori Ufficiale (Clinical Azzurrino & Slate)

Il colore primario è un **blu leggermente chiaro (Azzurrino Clinico / Sky)**, studiato per trasmettere serenità clinica, rigore scientifico e trasparenza tecnologica.

| Nome Token | HEX | Ruolo & Applicazione | Contrasto WCAG |
| :--- | :--- | :--- | :--- |
| **`MeditechCyanPrimary`** | `#0284C7` | **Colore Primario Marchio**, pulsanti CTA, badge attivi | AAA su bianco |
| **`MeditechSupsiBlue`** | `#00A3E0` | **Azzurro Istituzionale SUPSI**, highlight luminosi | AA+ |
| **`MeditechCyanDark`** | `#0369A1` | Zaffiro clinico per titoli ad alto contrasto e bordi attivi | AAA |
| **`Sky Azzurrino Light`** | `#38BDF8` | Nodo telemetrico, gradiente superiore del logo | Focus / Glow |
| **`MeditechCyanSoft`** | `#E0F2FE` | Sfondo chip selezionati, card evidenziate | - |
| **`MeditechCyanUltraLight`**| `#F0F9FF` | Sfondo icona app, canvas secondario | - |
| **`MeditechBg`** | `#F8FAFC` | Sfondo schermate (Slate 50 clinico) | - |
| **`MeditechSurface`** | `#FFFFFF` | Superficie card e dialog | - |
| **`MeditechTealActive`** | `#0D9488` | Biosegnali ECG attivi, dispositivi wearable connessi | AAA |
| **`MeditechTextPrimary`** | `#0F172A` | Testo primario (Slate 900) ad alta leggibilità | >14:1 |
| **`MeditechTextSecondary`** | `#334155` | Testo secondario, sottotitoli | >7:1 |
| **`MeditechTextTertiary`** | `#64748B` | Didascalie, metadati | >4.5:1 |

---

## 3. Marchio Ufficiale Approvato: Concetto 1 (Swiss Clinical Cross & Pulse)

Il marchio approvato unisce la precisione del design elvetico accademico SUPSI con la dinamica dei biosegnali e della salute digitale:
- **Geometria Primaria**: Croce medica elvetica proporzionata e smussata con precisione geometrica.
- **Tracciato Telemetrico**: Onda ECG continua intagliata a cuore bianco nel corpo della croce.
- **Bio-Nodo Centrale**: Sensore luminoso azzurro cielo (`#38BDF8`) che rappresenta il monitoraggio on-device.

### Master Asset Esportati (`logos/export/`)
- **Icona Vettoriale Master**: [`logos/export/icon.svg`](file:///c:/Users/manue/OneDrive/Desktop/Assistente_Meditech_Mobile/logos/export/icon.svg) (512x512)
- **Marchio Combinato Master**: [`logos/export/logo.svg`](file:///c:/Users/manue/OneDrive/Desktop/Assistente_Meditech_Mobile/logos/export/logo.svg) (1024x512)
- **Pacchetto PNG Universale**:
  - `icon-16.png`, `icon-32.png`, `icon-48.png`, `icon-64.png`, `icon-128.png`, `icon-192.png`, `icon-512.png`, `icon-1024.png`, `icon-2048.png`
  - `logo-512.png`, `logo-1024.png`, `logo-2048.png`
- **Anteprima Interattiva**: [`logos/preview.html`](file:///c:/Users/manue/OneDrive/Desktop/Assistente_Meditech_Mobile/logos/preview.html)

---

## 4. Integrazione Completa Android (Mipmap & Drawables)

L'intera dotazione grafica dell'app Android è stata aggiornata con gli asset del Concetto 1:

1. **Adaptive Vector Drawables (`app/src/main/res/drawable/`)**:
   - [`ic_launcher_background.xml`](file:///c:/Users/manue/OneDrive/Desktop/Assistente_Meditech_Mobile/app/src/main/res/drawable/ic_launcher_background.xml): Gradiente clinico chiaro `#F0F9FF` &rarr; `#E0F2FE` &rarr; `#BAE6FD` con aura radiale soffusa.
   - [`ic_launcher_foreground.xml`](file:///c:/Users/manue/OneDrive/Desktop/Assistente_Meditech_Mobile/app/src/main/res/drawable/ic_launcher_foreground.xml): Croce clinica elvetica centrata nella safe zone (66x66dp), gradiente azzurrino e tracciato ECG bianco puro.
   - [`ic_launcher_monochrome.xml`](file:///c:/Users/manue/OneDrive/Desktop/Assistente_Meditech_Mobile/app/src/main/res/drawable/ic_launcher_monochrome.xml): Compatibilità icone Material You (Android 13+).
   - [`meditech_avatar.png`](file:///c:/Users/manue/OneDrive/Desktop/Assistente_Meditech_Mobile/app/src/main/res/drawable/meditech_avatar.png): Avatar 512x512 ad alta risoluzione per l'assistente AI nella chat clinica.

2. **Raster Mipmap Density Buckets (`app/src/main/res/mipmap-*/`)**:
   - `mipmap-mdpi/`: `ic_launcher.png` e `ic_launcher_round.png` (48x48)
   - `mipmap-hdpi/`: `ic_launcher.png` e `ic_launcher_round.png` (72x72)
   - `mipmap-xhdpi/`: `ic_launcher.png` e `ic_launcher_round.png` (96x96)
   - `mipmap-xxhdpi/`: `ic_launcher.png` e `ic_launcher_round.png` (144x144)
   - `mipmap-xxxhdpi/`: `ic_launcher.png` e `ic_launcher_round.png` (192x192)

---

## 5. Regole d'Uso & Linee Guida

### Clear Space
Mantenere sempre uno spazio libero attorno all'icona pari ad almeno la metà della larghezza del braccio della croce ($0.5 \times X$).

### Legibilità a Piccole Dimensioni (Favicon Test)
Il logo è ottimizzato per preservare contrasto e riconoscibilità fino a 16px (la croce conserva il nodo centrale e il profilo solido azzurro-bianco senza sfocature).
