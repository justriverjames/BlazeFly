# BlazeFly
A "fly for fuel" plugin for Minecraft. Burn fuel from your inventory to fly in survival/adventure mode — like a furnace, but the furnace is you.

## Building
Requires JDK 25+ (Paper 26.2's minimum) and Maven.

```
mvn package
```

Jar comes out at `target/blazefly-<version>.jar`. Drop it in your server's `plugins/` folder.

## Config
On first run, `plugins/BlazeFly/config.yml` and `strings.yml` get generated. Edit those, then `/bfreload` (needs `blazefly.reload`).

Fuel is a priority list (`fuels` in config.yml) — when a player needs to burn fuel, BlazeFly checks their inventory against the list top to bottom and burns the first material it finds. Defaults ship with vanilla furnace burn times: lava bucket > blaze rod > block of coal > coal/charcoal > oak planks. Reorder or add entries to change what's preferred.

By default `blazefly.use` (the `/fly` permission) is granted to everyone. Everything else — `/flyspeed`, `/flyoff`, no-fuel flight (`blazefly.nofuel`, e.g. for admins), unbreakable wings, flying in disabled worlds, `/bfreload` — is op-only; grant those via a permissions plugin if you want to hand any out.
