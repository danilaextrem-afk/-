# РАЗЛОМ — обязательный контроль качества ассетов (UE5 / Windows PC)

## Это не отчёт о готовой AAA-графике
В репозитории сейчас есть черновые 3D OBJ и процедурные текстуры. Новая 4К-генерация
создаёт PBR-карты, но не заменяет ручной скульпт, UV-bake и арт-дирекшен.
Ни один ассет пока не прошёл проверку в установленном UE5.

## Первая очередь переработки
| Объект | Состояние | Критерии следующей версии |
| --- | --- | --- |
| Чёрный колокол | Hero v2 OBJ, >190 тыс. треугольников, 1K PBR | художественная асимметрия, сколы, глубокий рельеф, 4K bake, UE5 close-up |
| Святилище семи колоколов | OriginalProps черновой OBJ | собственная архитектура, проработанные камни, скульптура, Lumen-сцена |
| Ворота Ордена Завесы | OriginalProps черновой OBJ | оригинальная ковка, разрушения, уникальный герб, 4K материалы |
| Подъезд Москвы 2026 | OriginalProps черновой OBJ | убедительные жилые детали, штукатурка, свет, следы быта |
| 6 монстров для среза | отсутствуют production meshes | скелет, анимации, AI, уникальный силуэт, in-engine тест |
| Игровая механика | Godot эксперимент | UE5 Pawn, транспорт, 3–5-сек проблеск без телепорта |

## Что является фотодоказательством
- Рендер реального текущего OBJ следует подписывать «техническое превью модели».
- Скриншот UE5 подписывать «UE5» только если импорт и захват реально состоялись.
- Концепт-арт не считается фотографией готовой игровой модели.
- Финальный арт-ассет: геометрия, UV, PBR, карта нормалей, понятный scale,
  визуальная проверка при дневном/ночном свете, коллизии/Nanite/производительность.

## Следующая конкретная работа
Настроить доступную Windows/GPU UE5-среду для настоящего UE5 импорта; затем
реально пересоздать каждый черновой объект и делать фото *после* каждой версии.
Платные fal-запуски запрещены текущим ограничением бюджета 0 ₽.


## 2026-11-21 — ART REVIEW: BLACK BELL v3 REJECTED

**Owner feedback:** «Это не уровень, это не ААА» (rejected). The v3 geometry preview is **NOT** approved for production even though model generation and texture CI succeeded. It is a nonphotorealistic technical sculpt preview; making a 4K screenshot or a 4K texture pack does not cure the problem.

### Root causes visible in the actual model preview
- Silhouette is too ordinary and uniform: generic trapezoid bell + oversized straight oak beam.
- Crown/hooks are thin and cartoonish, metal relief too regular and unconvincing.
- Turquoise bosses and gilded stripes read like cheap decorative jewelry, not ominous medieval casting.
- No cast-metal microrelief, chipped inscription depth, weathered edges, grime pooling, believable physically based specular response.
- Generic studio shading with no UE5 Lumen/Nanite import, no believable cathedral/crypt scene, no close-up acceptance.

### Replacement direction — do NOT iterate by merely adding tubes or polygons
1. **Re-sculpt a new silhouette**: irregular monumental 1348 bronze casting, weighty profile, battered lip, credible suspension assembly and forged iron yoke.
2. **Model unique, irregular high/low frequency relief**: genuinely recessed carved glyphs and partial destroyed iconography; no mechanically repeated round ornaments.
3. **Physically based production materials**: aged black bronze with believable oxidation and worn specular edges, selective tar/grime, rough wooden yoke and iron forging; 4K texture bake from the *actual* hi-poly mesh to low-poly UVs.
4. **Actual engine acceptance**: import asset+textures into a UE5 Windows project, inspect with controlled neutral light and cinematic Lumen, capture a real 4K frame and closeups, validate Nanite collision, scale and VRAM/FPS.
5. Do not label a visual **AAA** unless the in-engine proof exists and user accepts it. New version must have substantially different *art*, not just triangle count.

**Current status:** production-quality gate = FAIL. Preserve v2/v3 as archived experiments only.
