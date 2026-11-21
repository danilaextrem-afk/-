# РАЗЛОМ — бесплатные облачные вычисления / статус 2026-11-21

**Реально подключено:** GitHub Actions, публичный репозиторий danilaextrem-afk/-.
Стандартные GitHub-hosted runner'ы для публичных репозиториев доступны без платы за минуты.
Нет обязательной регистрации у нового провайдера или ввода платёжной карты.

## Что автоматизировано
- `windows-2025` — облачная Windows VM на время GitHub Actions job. Проверяет структуру нативных UE5 исходников и записывает конфигурацию машины в `runner_report.json`. **Не содержит Unreal Editor**, не гарантирует GPU.
- `ubuntu-24.04` — облачная Linux VM. Устанавливает свободный Blender через пакетный менеджер, импортирует действительную OBJ-модель и рендерит ее в PNG на CPU. **Это Blender render, не UE5**.
- Job'ы запускаются автоматически при изменениях скриптов; результаты сохраняются как GitHub Actions Artifacts (retention: 7 дней), без ручного управления телефоном.

## Запуск и результаты
Открой в GitHub вкладку Actions → `RAZLOM free cloud compute (Windows QA + Blender CPU render)`.
В конкретном запуске → Artifacts скачай `RAZLOM-Windows-cloud-hardware-report` и `RAZLOM-Blender-actual-OBJ-preview`.
После неуспешной сборки читаем конкретные логи, не объявляем задачу выполненной.

## Ограничения
- Эти машинки временные: не полноценный постоянный удалённый Windows-десктоп.
- Стандартный runner (на момент выбора) имеет 4 vCPU, 16GB RAM, 14GB SSD; выделенный GPU не включён. Полный Unreal Engine 5 Editor / 4K Lumen не запустится гарантированно.
- Colab/Kaggle предлагают временные GPU при наличии квот, но работают как notebook в Linux и не заменяют управляемый Windows+UE5 cloud PC.
- Не оплачиваем никакие cloud услуги и не запускаем платную fal-генерацию без явного согласования.
- Честный критерий **UE5 verified**: Windows GPU + установленный Unreal Editor, импорт UE5, реальный рендер из движка, проверка FPS/VRAM. Пока не выполнен.

Ссылки на официальную документацию:
- https://docs.github.com/en/actions/how-tos/write-workflows/choose-where-workflows-run/choose-the-runner-for-a-job
- https://dev.epicgames.com/documentation/unreal-engine/hardware-and-software-specifications-for-unreal-engine
