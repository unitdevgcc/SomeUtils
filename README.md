<p align="center">
  <img src="img/png.png" alt="SomeUtils" width="128">
</p>

<h1 align="center">SomeUtils by c0redev</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Paper-1.21.11-F37723?style=flat-square" alt="Paper 1.21.11">
  <img src="https://img.shields.io/github/actions/workflow/status/unitdevgcc/SomeUtils/ci.yml?branch=master&style=flat-square&label=build" alt="CI">
  <img src="https://img.shields.io/github/license/unitdevgcc/SomeUtils?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/version-1.2.4-6fae6f?style=flat-square" alt="Version">
</p>

Minecraft плагин для Paper 1.21.11. Утилиты и фичи для сервера.

Функции
-------

- **InvTweaks**: сортировка контейнеров, GUI-кнопки, refill
- **Jade/Waila**: server-side HUD блоков и сущностей
- **Armor HUD**: scoreboard sidebar с vanilla armor textures, durability bar и процентом каждого предмета
- **Damage Indicator**: server-side индикаторы урона

Armor HUD
---------

- Работает без клиентского мода
- Показывает helmet, chestplate, leggings, boots и непустой off-hand
- Использует отдельные custom textures для пустых slots
- Подсвечивает низкую прочность анимированной трещиной
- `compact` скрывает пустые slots и уменьшает вертикальные отступы
- Цвета рамки меняются в `/su` без ручной пересборки resource pack

Jade HUD
--------

- `compact` ограничивает панель двумя приоритетными строками
- Иконки, вторичные детали и интервалы переключаются в `/su`
- Панель использует общие цвета Armor HUD
- Прогресс разрушения заменяет второстепенные строки в compact mode

Зависимости
-----------

- Paper 1.21.11+
- PacketEvents 2.7+ (опционально, для Jade)

Сборка
------

Нужен JDK 21+; компиляция использует Java 21 toolchain.

```
./gradlew build
```

JAR будет в `build/libs/SomeUtils-<version>.jar`.

Версия сборки: `SOMEUTILS_VERSION=v1.2.0` или `-PreleaseVersion=1.2.0`.

Конфигурация
------------

`config.yml` — основные опции:

- `jade.enabled` — включить HUD-панель
- `resource-pack.enabled` — включить раздачу ресурспака
- `resource-pack.public-url` — публичный URL resource pack; обязателен для клиентов вне localhost/LAN
- `resource-pack.http-port` — HTTP-порт встроенной раздачи resource pack
- `resource-pack.force` — отключать клиента при отказе от resource pack
- `invtweaks.enabled` — включить InvTweaks (сортировка)
- `armor-hud.enabled` — включить Armor HUD
- `armor-hud.compact` — скрыть пустые slots и лишние отступы
- `armor-hud.pulse-threshold` — порог анимации критической прочности, `0` отключает
- `armor-hud.show-offhand` — показать непустой off-hand
- `armor-hud.border.*` — цвета рамки и percentage cards
- `jade.compact` — ограничить Jade двумя строками
- `jade.show-icons` — показывать block/entity/tool icons
- `jade.show-details` — показывать вторичную строку данных
- `jade.line-gap-bars` — интервал между строками в full mode
- `invtweaks.gui-controls` — показывать GUI-кнопки управления

Команда
-------

```
/someutils [sort|jade|refill|pack|reload|help]
/su [sort|jade|armor|refill|pack|reload|help]
```

Сортировка: `/su sort [default|columns|stack]` или кнопки InvTweaks в GUI открытого контейнера.

Armor HUD переключается командой `/su armor` или кнопкой в меню `/su`.

Лицензия
---------

MIT
