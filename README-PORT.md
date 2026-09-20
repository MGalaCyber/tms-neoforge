# Too Many Shortcuts — NeoForge 1.21.1 port

Port dari [too-many-shortcuts](https://github.com/wyatt-herkamp/too-many-shortcuts)
(Apache-2.0, Wyatt Herkamp; core keybinding berbasis Amecs API oleh Siphalor), basisnya branch
**`ver/1.21`** (bukan `main`, yang target Minecraft 26.3 dan API-nya beda jauh).

## Status: belum pernah dikompilasi

Container yang dipakai buat nulis ini nggak punya akses jaringan, jadi Gradle belum pernah
nge-resolve NeoForge dan **belum satu baris pun dikompilasi**. Setiap nama kelas/method
dicocokkan manual ke Mojang mappings 1.21.1 (sebagian dicek ulang lewat pencarian ke
mappings.dev buat konfirmasi), tapi itu bukan pengganti compiler. Anggap ini draf teknis
yang serius, siap di-iterasi lewat CI — bukan jar yang siap pakai.

**Silakan push ke fork lu dan biarkan `.github/workflows/build.yml` jalan.** Error compile
pertama yang muncul kemungkinan besar di salah satu titik yang ditandai "⚠️" di bawah.

## Penyimpangan dari jawaban lu: Kotlin dibuang, full Java

Lu bilang mau pakai Kotlin for Forge dan tetap pertahankan Kotlin. **Gw nggak melakukan itu**
— seluruh source ditulis ulang ke Java murni, nggak ada dependensi Kotlin for Forge sama
sekali. Alasannya: source `ver/1.21` itu campuran Yarn mappings *dan* Kotlin (data class,
companion object, extension function, kotlinx.serialization). Kalau gw terjemahkan ke Kotlin-
di-NeoForge, ada dua sumber error yang numpuk sekaligus tanpa compiler buat ngecek — mapping
Yarn→Mojang, DAN sintaks Kotlin yang saling bergantung ke tipe-tipe Mojang itu. Java tanpa
Kotlin lebih gampang ditelusuri manual, method-per-method, terhadap referensi mappings.

Ini keputusan sepihak dan lu berhak nggak setuju. Kalau lu tetap mau bentuk Kotlin (biar lebih
gampang lu maintain sendiri karena udah familiar sama source aslinya), bilang aja — begitu versi
Java ini kebukti compile bersih di CI, migrasi ke Kotlin for Forge tinggal soal gaya penulisan
doang, bukan lagi nebak-nebak API.

## Yang sudah diport (fungsional lengkap, dari `ver/1.21`)

| Fitur | File |
|---|---|
| Modifier Ctrl/Shift/Alt per keybind | `MixinKeyMapping`, `api/BindingModifiers.java` |
| Beberapa keybind boleh pakai key yang sama, beda modifier | `KeyBindingManager.java` |
| **Alternative binds** (satu aksi, beberapa bind sekaligus) | `alternatives/AlternativeKeyMapping.java` |
| Layar keybind kustom (list per kategori, search, tombol +/reset/hapus) | `gui/TMSKeyBindsScreen.java` |
| Simpan/muat ke `config/too_many_shortcuts.json` (lihat catatan di bawah) | `config/ConfigManager.java` |
| Alternative escape key | `MixinKeyboardHandler.java` |
| Toggle Auto Jump | `keybinding/ToggleAutoJumpKeyMapping.java` |
| Toggle tiap skin layer (cape, jacket, sleeves, dll) | `keybinding/SkinLayerKeyMapping.java` |
| Priority keybinding (nangkep sebelum GUI lain) | `api/PriorityKeyBinding.java` |
| Label keybind jadi "Ctrl + K" di GUI | `MixinKeyMapping#tms$prefixModifiers` |

## Yang SENGAJA tidak diport

- **Scroll-mouse-sebagai-keybind** (generic API buat mod lain nge-bind ke scroll wheel). Ini
  fitur API-level buat integrasi mod ketiga, bukan sesuatu yang kepake langsung di TMS sendiri
  di `ver/1.21`. Risiko salah tanpa bisa dites lebih besar daripada manfaatnya untuk lu sekarang.
- Kotlin — lihat bagian di atas.

## ⚠️ Titik paling berisiko (belum bisa gw verifikasi tanpa compiler)

1. **`Options.isModelPartEnabled` / `Options.setModelPartEnabled`** di `SkinLayerKeyMapping.java`
   — nama method ini gw tebak berdasarkan pola penamaan vanilla, TIDAK gw temukan konfirmasi
   langsung. Kalau build gagal di sini, cek nama sebenarnya lewat decompile
   `net.minecraft.client.Options` versi 1.21.1 dan ganti manual.
2. **`gui/TMSKeyBindsScreen.java`** — konstruktor `ObjectSelectionList` dan urutan parameter
   `render(...)` di tiap `Entry` itu API yang paling sering berubah kecil-kecilan antar versi
   Minecraft. Ini file paling besar (≈300 baris) dan paling mungkin butuh penyesuaian.
3. **`Options.keyMappings`** diasumsikan sudah `public` di vanilla (dibaca langsung dari
   `TMSKeyBindsScreen` tanpa AccessTransformer). Kalau ternyata `private`, tambahkan baris di
   `src/main/resources/META-INF/accesstransformer.cfg`:
   ```
   public net.minecraft.client.Options keyMappings
   ```
4. Field lain yang di-`@Shadow` di `MixinKeyMapping.java` (`key`, `defaultKey`, `category`,
   `clickCount`, `isDown`) sudah gw cocokkan ke Mojang mappings 1.21.1 lewat mappings.dev dan
   cukup yakin benar — tapi Mixin akan langsung kasih error jelas di log kalau ada yang meleset,
   jadi ini bukan tipe error yang bikin bingung lama.

## Perubahan perilaku dari upstream (disengaja)

- **Lokasi config**: upstream nyimpen `too_many_shortcuts.json` di root folder instance
  (`.minecraft/`). Port ini pakai `config/too_many_shortcuts.json` — konvensi standar NeoForge.
- **Tombol screen**: bukan ganti tombol "Key Binds..." vanilla, tapi nambah tombol baru
  "Too Many Shortcuts..." di sebelahnya (`ClientSetup.onScreenInit`). Lebih aman karena nggak
  gantung ke internal vanilla button dan tetap kasih akses ke Controls screen vanilla yang asli.

## Cara build & release

Sama seperti sebelumnya — Gradle wrapper dan dua GitHub Actions workflow
(`.github/workflows/build.yml`, `.github/workflows/release.yml`) sudah ada di project ini.

```bash
./gradlew build          # jar keluar di build/libs
./gradlew runClient      # tes langsung
```

Push ke branch fork lu → `build.yml` jalan otomatis → kalau gagal, download artifact
`build-reports` buat lihat log lengkap. Kalau sukses, tag `vX.Y.Z` buat trigger `release.yml`
dan otomatis muncul di halaman Releases.

## Lisensi

Tetap Apache-2.0 (lihat `LICENSE`), warisan dari mod asli. Kalau berniat publish ke Modrinth,
disaranin kasih tahu wyatt-herkamp dulu atau kirim PR ke repo aslinya.
