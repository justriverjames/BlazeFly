# BlazeFly
A "fly for fuel" plugin for Minecraft. Burn blaze rods (or coal, for VIPs) to fly in survival/adventure mode.

## Building
Requires JDK 25+ (Paper 26.2's minimum) and Maven.

```
mvn package
```

Jar comes out at `target/blazefly-<version>.jar`. Drop it in your server's `plugins/` folder.

## Config
On first run, `plugins/BlazeFly/config.yml` and `strings.yml` get generated. Edit those, then `/bfreload` (needs `blazefly.reload`).

By default `blazefly.use` (the `/fly` permission) is granted to everyone. Everything else — `/flyspeed`, VIP fuel rate, `/flyoff`, no-fuel flight, unbreakable wings, flying in disabled worlds, `/bfreload` — is op-only; grant those via a permissions plugin if you want to hand any out.
