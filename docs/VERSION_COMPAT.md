# PVP Reach Overlay — Version Compatibility Matrix

## Sürüm Destek Durumu

| Minecraft Sürümü | Build Durumu | Fabric API | Loom | Notlar |
|-------------------|-------------|------------|------|--------|
| 1.20              | ✅ Derlendi  | 0.83.0+1.20 | 1.2  | İlk build |
| 1.20.1            | ⏳ Test edilecek | - | - | - |
| 1.20.2            | ⏳ Test edilecek | - | - | - |
| 1.20.3            | ⏳ Test edilecek | - | - | - |
| 1.20.4            | ⏳ Test edilecek | - | - | - |
| 1.20.5            | ⏳ Adaptasyon gerekebilir | - | 1.6+ | Data Components |
| 1.20.6            | ⏳ Adaptasyon gerekebilir | - | 1.6+ | Data Components |
| 1.21              | ⏳ Adaptasyon gerekebilir | - | - | Major değişiklikler |
| 1.21.1            | ⏳ Test edilecek | - | - | - |
| 1.21.3            | ✅ Derlendi  | 0.114.1+1.21.3 | 1.10 | Matrix3x2fStack adaptasyonu |
| 1.21.4            | ✅ Derlendi  | 0.119.4+1.21.4 | 1.10 | Matrix3x2fStack adaptasyonu |
| 1.21.5            | ✅ Derlendi  | 0.128.2+1.21.5 | 1.10 | Matrix3x2fStack adaptasyonu |
| 1.21.6            | ✅ Derlendi  | 0.128.2+1.21.6 | 1.10 | RenderPipelines API geçişi |
| 1.21.7            | ✅ Derlendi  | 0.129.0+1.21.7 | 1.10 | - |
| 1.21.8            | ✅ Derlendi  | 0.136.1+1.21.8 | 1.10 | - |
| 1.21.9            | ❌ Hata (Loom) | - | - | Unpick sürüm hatası |
| 1.21.10           | ⏳ Derleniyor | - | - | - |
| 1.21.11           | ⏳ Hedef sürüm | - | - | - |

## Kırılma Noktaları

| Aralık | Kırılma | Detay |
|--------|---------|-------|
| 1.20 → 1.20.4 | 🟢 Düşük risk | Minor değişiklikler |
| 1.20.4 → 1.20.5 | 🔴 Yüksek risk | NBT → Data Components |
| 1.20.6 → 1.21 | 🔴 Yüksek risk | ResourceLocation, Vertex, Render değişiklikleri |
| 1.21 → 1.21.11 | 🟡 Orta risk | Bilinmiyor, test gerekli |
