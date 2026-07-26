<p align="center">
  <img src="img/png.png" alt="SomeUtils" width="128">
</p>

<h1 align="center">SomeUtils by c0redev</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="Java 21">
  <img src="https://img.shields.io/badge/Paper-1.21.11-F37723?style=flat-square" alt="Paper 1.21.11">
  <img src="https://img.shields.io/github/actions/workflow/status/unitdevgcc/SomeUtils/ci.yml?branch=master&style=flat-square&label=build" alt="CI">
  <img src="https://img.shields.io/github/license/unitdevgcc/SomeUtils?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/version-1.0.0-6fae6f?style=flat-square" alt="Version">
</p>

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