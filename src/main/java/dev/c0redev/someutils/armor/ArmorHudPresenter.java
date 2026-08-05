package dev.c0redev.someutils.armor;

import org.bukkit.entity.Player;

interface ArmorHudPresenter {
    void update(Player player, ArmorHudRender render);

    void remove(Player player);
}
