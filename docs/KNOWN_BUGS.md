# PVP Reach Overlay — Known Bugs

> Bu dosya, modun geliştirme sürecinde tespit edilen tüm hataları takip eder.
> Her yeni özellik eklenirken bu liste kontrol edilmeli ve regression testleri yapılmalıdır.

---

## Açık Buglar

_Henüz bilinen bir bug yok._

---

## Potansiyel Sorunlar (İzlenmesi Gereken)

### ~~[WATCH-002] OptiFine Custom Crosshair Uyumsuzluğu~~ → ✅ Çözüldü
- **Durum:** 🟢 Sorun Yok (Test Edildi)
- **Açıklama:** `drawTexture(GUI_ICONS, ...)` Minecraft'ın resource manager'ından geçiyor. Herhangi bir resource pack `textures/gui/icons.png` dosyasını değiştirdiğinde, bizim overdraw kodumuz da otomatik olarak pack'in versiyonunu kullanıyor. Farklı crosshair texture pack'i ile test edildi — mükemmel uyumlu.
- **OptiFine Notu:** OptiFine Fabric'te çalışmaz (kullanıcılar Sodium/Iris kullanır), dolayısıyla pratikte sorun teşkil etmez.
- **Tarih:** 2026-05-06

### [WATCH-001] Köşeli Bakış Açısı Problemi
- **Durum:** 🟡 İzleniyor
- **Açıklama:** Oyuncu düz bir açıyla değil, köşeli/diagonal bir açıyla baktığında blok tespiti doğru çalışmayabilir
- **Özellik:** ReachOverlayRenderer — ray stepping algoritması
- **Planlanan Çözüm:** 0.25 blok adım boyutu kullanılıyor, gerekirse daha küçük adıma inilecek
- **Tarih:** 2026-05-05

---

## Çözülmüş Buglar

### [BUG-001] Gradle Java 21 Uyumsuzluğu
- **Durum:** 🟢 Çözüldü
- **Açıklama:** Gradle 8.1.1 Java 21'in class file major version 65'i desteklemiyordu
- **Çözüm:** Gradle 8.5'e yükseltildi
- **Tarih:** 2026-05-04

### [BUG-002] Cross Renk Değişimi Kalp/Açlık Çubuğunu Etkiliyor
- **Durum:** 🟢 Çözüldü
- **Açıklama:** InGameHudMixin'de `RenderSystem.setShaderColor()` ve `blendFuncSeparate()` değişikliği, `renderCrosshair` method'undan sonraki HUD render çağrılarına (kalp, açlık, XP barı) sızıyordu.
- **Kök Neden:** HEAD injection'da blend mode değişimi + shader color ayarı, RETURN'de tam reset yapılsa bile vanilla'nın iç render state'ini bozuyordu.
- **Çözüm:** "Tinting" yaklaşımı tamamen kaldırıldı. Yerine: vanilla crosshair normal render edilir, ardından RETURN injection'da aynı crosshair texture renkli olarak **üstüne çizilir** (overdraw). Tüm render state çizimden hemen sonra reset edilir — sıfır sızıntı.
- **Etkilenen Dosya:** `InGameHudMixin.java`
- **Dikkat:** İleride HUD render state'ini değiştiren herhangi bir mixin eklenirken, render state'in **aynı injection içinde** reset edildiğinden emin olunmalı. Vanilla render pipeline'ına blend/color state bırakılmamalı.
- **Tarih:** 2026-05-05

---

## Notlar

- Her yeni ekleme öncesi bu dosya okunmalıdır
- Yeni bug bulunduğunda BUG-XXX formatında eklenmeli
- Çözülen buglar "Çözülmüş" bölümüne taşınmalı
