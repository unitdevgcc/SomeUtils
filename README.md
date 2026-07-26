SomeUtils by c0redev
=========

Minecraft плагин для Paper 1.21.11. Утилиты и фичи для сервера.

Функции
-------

- **Inventory Tweaks** — GUI-сортировка контейнеров
- **Jade/Waila** — HUD-панель имитирующая поведение Jade/Waila


Зависимости
-----------

- Paper 1.21.11+
- packetevents (опционально, для кастомных моделей в GUI)

Сборка
------

```
./gradlew build
```

JAR будет в `build/libs/SomeUtils-1.0.0.jar`.

Конфигурация
------------

`config.yml` — Основные опции:

- `jade.enabled` — включить HUD-панель
- `resource-pack.enabled` — включить раздачу ресурспака
- `inv-tweaks.enabled` — включить сортировку инвентаря
- `inv-tweaks.gui-controls` — показывать GUI-кнопки управления

Команда
-------

```
/someutils [sort|jade|refill|pack|reload|help]
/su [sort|jade|refill|pack|reload|help]
```

Лицензия
---------

MIT
