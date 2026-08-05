package dev.c0redev.someutils.armor;

import java.util.UUID;

record ArmorHudRender(UUID playerId, long generation, long sequence,
                      ArmorHudPosition position, ArmorSnapshot snapshot, String text, boolean rightSide,
                      boolean compact, int pulseThreshold, boolean showOffhand) {
}
