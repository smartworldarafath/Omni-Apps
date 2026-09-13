<a id="top"></a>
<div align="center">

<img src="docs/logo.png" width="410" alt="Omni Apps logo"/>

# OMNI APPS

<img src="https://readme-typing-svg.demolab.com/?font=Orbitron&weight=700&size=20&duration=3000&pause=1200&color=00F0FF&center=true&vCenter=true&width=680&lines=Multi-source+Android+app+store;GitHub%2C+GitLab%2C+F-Droid%2C+IzzyOnDroid%2C+APKPure+%26+Aptoide;Open+source+%C2%B7+Zero+ads+%C2%B7+Zero+bloat" alt="typing tagline" width="680" height="40"/>

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:00F0FF,50:7AA8FF,100:FF2D78&height=90&section=header&animation=fadeIn" width="100%" height="90" alt="divider"/>

[![License](https://img.shields.io/badge/License-AGPL--3.0-FF2D78?style=for-the-badge&labelColor=000000)](LICENSE)
[![Release](https://img.shields.io/github/v/release/smartworldarafath/Omni-Apps?style=for-the-badge&color=00F0FF&labelColor=000000)](https://github.com/smartworldarafath/Omni-Apps/releases)
[![Stars](https://img.shields.io/github/stars/smartworldarafath/Omni-Apps?style=for-the-badge&color=FF2D78&labelColor=000000&logo=github&logoColor=00F0FF)](https://github.com/smartworldarafath/Omni-Apps/stargazers)
[![Kotlin](https://img.shields.io/badge/Kotlin-100%25-00F0FF?style=for-the-badge&logo=kotlin&logoColor=black&labelColor=000000)](https://kotlinlang.org)
[![Min SDK](https://img.shields.io/badge/API-26+-FF2D78?style=for-the-badge&labelColor=000000)](#)

[**⬇ Download APK**](https://github.com/smartworldarafath/Omni-Apps/releases/latest) &nbsp;·&nbsp; [**🌐 Website**](https://smartworldarafath.github.io/Omni-Apps/) &nbsp;·&nbsp; [**🐛 Report Bug**](https://github.com/smartworldarafath/Omni-Apps/issues)

<br/>

[![Features](https://img.shields.io/badge/Features-00F0FF?style=for-the-badge&labelColor=000000&logoColor=black)](#features)
[![Screenshots](https://img.shields.io/badge/Screenshots-00F0FF?style=for-the-badge&labelColor=000000)](#screenshots)
[![Install](https://img.shields.io/badge/Install-FF2D78?style=for-the-badge&labelColor=000000)](#installation)
[![Tech Stack](https://img.shields.io/badge/Tech_Stack-00F0FF?style=for-the-badge&labelColor=000000)](#tech-stack)

</div>

---

> ⚠️ **Official Source Notice**
> The ONLY official source for Omni Apps is this repository.
> APKs from any other website, Telegram channel, or source are
> unofficial and may be tampered with. Always verify the signature.

<a id="features"></a>
## ✨ Features

<table>
<tr>
<td width="50%" valign="top">

**🔍 6 sources, one store**
GitHub, GitLab, F-Droid, IzzyOnDroid, APKPure, and Aptoide — scanned and merged into a single feed.

**🗂 17 curated categories**
Games, Productivity, Security, Dev Tools, Media, Finance and more, plus smart sections like Trending and Newly Launched.

**🛡 Signature verification**
Every downloaded APK is checked against the installed app's signing certificate before install — a hijacked repo or redirected release can't silently overwrite what's on your phone.

**🥷 Silent installs via Shizuku**
Skip the system install confirmation screen entirely when Shizuku is running.

**🛡 Trust Score system**
0–100 score based on stars, activity, releases, and forks.

**🔔 Background update monitoring**
WorkManager checks installed apps against all six sources and notifies you of updates.

</td>
<td width="50%" valign="top">

**📱 Home screen widget**
App of the Day plus your pending update count, refreshed every 30 minutes.

**⚖️ App comparison mode**
Compare two apps side-by-side.

**📸 Auto-extracted screenshots**
Pulled straight from each repo's README.

**🔄 Install history & rollback**
Roll back to a previous version straight from your install history.

**⭐ GitHub starred repos sync**
Sync your stars into favourites.

**🌍 16 languages**
English, Hindi, Spanish, French, German, Japanese, Portuguese, Italian, Russian, Chinese, Korean, Arabic, Dutch, Turkish, Polish, Swedish.

**📢 In-app announcements**
Dismissible banners for giveaways, releases, and community updates.

</td>
</tr>
</table>

<a id="screenshots"></a>
## 📱 Screenshots

<div align="center">

<img src="docs/e1.jpg" width="190" height="422" alt="sc1"/> <img src="docs/e2.jpg" width="190" height="422" alt="sc2"/> <img src="docs/e3.jpg" width="190" height="422" alt="sc3"/> <img src="docs/e4.jpg" width="190" height="422" alt="sc4"/>

*Home ·*

<img src="docs/c1.png" width="190" height="422" alt="sc1"/> <img src="docs/c2.png" width="190" height="422" alt="sc2"/> <img src="docs/c4.png" width="190" height="422" alt="sc3"/> <img src="docs/c5.png" width="190" height="422" alt="sc4"/>

*Home · Apps · Profile · Settings*

<img src="docs/5.jpeg" width="190" height="422" alt="Home"/> <img src="docs/6.jpeg" width="190" height="422" alt="Apps"/> <img src="docs/7.jpeg" width="190" height="422" alt="Profile"/> <img src="docs/8.jpeg" width="190" height="422" alt="Settings"/>

*Home · Apps · Profile · Settings*

<img src="docs/n3.jpg" width="190" height="422" alt="Screenshot 1"/> <img src="docs/n5.jpg" width="190" height="422" alt="Screenshot 2"/> <img src="docs/n4.jpg" width="190" height="422" alt="Screenshot 3"/> <img src="docs/n2.jpg" width="190" height="422" alt="Screenshot 4"/>

</div>

<a id="installation"></a>
## 📥 Installation

1. Download the latest APK from [Releases](https://github.com/smartworldarafath/Omni-Apps/releases/latest)
2. On your Android device: **Settings → Apps → Special access → Install unknown apps** → enable for your browser/file manager
3. Tap the downloaded APK to install

> 💡 Optional: install [Shizuku](https://shizuku.rikka.app/) for silent, confirmation-free installs of every app you update through Omni Apps.

<details>
<summary><b>🔧 Building from Source</b></summary>
<br/>

```bash
git clone https://github.com/smartworldarafath/Omni-Apps.git
cd Omni-Apps
```

Open the project in Android Studio (JDK 17, compileSdk 37, targetSdk 36, minSdk 26). It will build and run out of the box — the following `local.properties` keys are all **optional** and only needed to reproduce specific production behavior:

| Key | Purpose | If omitted |
|---|---|---|
| `signing.storeFile` / `storePassword` / `keyAlias` / `keyPassword` | Release signing | Unsigned release APK |
| `gumroad.product.id` | Liquid Glass Pro license verification | Verification disabled — Pro themes stay locked |
| `lg.hmac.secret` / `lg.script.url` | Liquid Glass license signing endpoint | N/A in forks |

Everything else — the six-source scanner, Trust Score, comparison mode, widget, and free themes — works fully without any secrets configured.

</details>

<a id="tech-stack"></a>
## 🛠 Tech Stack

<div align="center">

<img src="https://skillicons.dev/icons?i=kotlin,androidstudio,git,github,gradle&theme=dark" alt="tech icons"/>

</div>

- [Kotlin](https://kotlinlang.org/) + [Jetpack Compose](https://developer.android.com/jetpack/compose) + [Material 3](https://m3.material.io/)
- [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/) — GitHub / GitLab / F-Droid / IzzyOnDroid / APKPure / Aptoide clients
- [Coil](https://coil-kt.github.io/coil/) — image loading
- [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) — background update checks
- [Shizuku](https://shizuku.rikka.app/) — silent installs without root
- [AndroidX Security Crypto](https://developer.android.com/jetpack/androidx/releases/security) — encrypted storage for tokens and license keys
- [Backdrop](https://github.com/Kyant0/Backdrop) — real-time blur for the Liquid Glass Pro themes

<a id="contributing"></a>


---

## ☕ Support / Buy Me a Coffee & Become a Sponsor

If you find **Omni Apps** helpful and want to support ongoing development, maintenance, and new features, consider contributing through any of the options below! Your support means the world and helps keep this project open-source.

<div align="center">

<table>
  <tr>
    <td align="center" width="25%" valign="top">
      <h4>☕ SupportKori</h4>
      <a href="https://www.supportkori.com/arafathrahman" target="_blank">
        <img src="assets/supportkori-qr.jpg" alt="SupportKori QR" width="180" style="border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.15);" />
      </a><br/><br/>
      <a href="https://www.supportkori.com/arafathrahman" target="_blank">
        <img src="https://img.shields.io/badge/Support-SupportKori-FF5E5B?style=for-the-badge&logo=buy-me-a-coffee&logoColor=white" alt="SupportKori Badge" />
      </a><br/>
      <sub>Cards / bKash / Nagad / Global</sub>
    </td>
    <td align="center" width="25%" valign="top">
      <h4>⚡ nsave</h4>
      <img src="assets/nsave-qr.jpg" alt="nsave QR" width="180" style="border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.15);" /><br/><br/>
      <img src="https://img.shields.io/badge/nsave-@arafath__rahman9-000000?style=for-the-badge&logoColor=white" alt="nsave Badge" /><br/>
      <sub>Ntag: <code>@arafath_rahman9</code></sub>
    </td>
    <td align="center" width="25%" valign="top">
      <h4>🔴 RedotPay</h4>
      <img src="assets/redotpay-qr.jpg" alt="RedotPay QR" width="180" style="border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.15);" /><br/><br/>
      <img src="https://img.shields.io/badge/RedotPay-1965421414-E51E2B?style=for-the-badge&logoColor=white" alt="RedotPay Badge" /><br/>
      <sub>ID: <code>1965421414</code></sub>
    </td>
    <td align="center" width="25%" valign="top">
      <h4>🅿️ Payoneer</h4>
      <img src="assets/payoneer-info.jpg" alt="Payoneer Info" width="180" style="border-radius: 12px; box-shadow: 0 4px 15px rgba(0,0,0,0.15);" /><br/><br/>
      <a href="mailto:arafathrahman710@gmail.com?subject=Support%20via%20Payoneer">
        <img src="https://img.shields.io/badge/Payoneer-70366820-FF4800?style=for-the-badge&logo=payoneer&logoColor=white" alt="Payoneer Badge" />
      </a><br/>
      <sub>Email: <code>arafathrahman710@gmail.com</code></sub>
    </td>
  </tr>
</table>

<br/>

| Method | Details / Direct Link |
| :--- | :--- |
| **☕ SupportKori** | [https://www.supportkori.com/arafathrahman](https://www.supportkori.com/arafathrahman) |
| **⚡ nsave** | Ntag: `@arafath_rahman9` • `Md Arafath Rahman` |
| **🔴 RedotPay** | Account ID: `1965421414` |
| **🅿️ Payoneer** | Email: `arafathrahman710@gmail.com` • Customer ID: `70366820` |

</div>

## 🤝 Contributing

Contributions are welcome! Open an issue first to discuss what you'd like to change.

1. Fork the repo
2. Create a feature branch (`git checkout -b feature/cool-thing`)
3. Commit your changes (`git commit -m 'Add cool thing'`)
4. Push to the branch (`git push origin feature/cool-thing`)
5. Open a Pull Request

## 📄 License

[![License](https://img.shields.io/badge/License-AGPL--3.0-FF2D78?style=for-the-badge&labelColor=000000)](LICENSE)

<br/>

<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:FF2D78,50:7AA8FF,100:00F0FF&height=90&section=footer" width="100%" height="90" alt="divider"/>

[⬆ Back to top](#top)

</div>
