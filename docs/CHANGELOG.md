# Changelog — Vision Tactical: EagleEye & Drones

## [1.0.0] - 2026-11-26
### Added
- Inicialización del proyecto Architectury 1.20.1 (`common`, `forge`, `fabric`) con soporte de GeckoLib 4.4.4 y Vision Goggles 2.0.0 API.
- Diseño del plan arquitectónico en `docs/planes/plan_v1.md` inspirado en Anduril EagleEye y Lattice OS.
- Generador de assets y localización `scripts/generate_assets.py` (PIL/JSON):
  - Texturas pixel-art 16x16 (`tactical_drone`, `lattice_fpv_controller`, `eagleeye_datalink_module`, `drone_payload_emp`, `drone_payload_he`), textura UV 64x64 del dron (`textures/entity/tactical_drone.png`) y logo 128x128 (`icon.png`).
  - Modelo GeckoLib 4 `geo/tactical_drone.geo.json` (`root`, `body`, `gimbal`, `payload_bay`, `rotor_fl/fr/bl/br`) y animaciones `animations/tactical_drone.animation.json` (`fly`, `hover`, `idle`).
  - Modelos de ítems (`models/item/*.json`), mapeo de sonidos seguros a eventos vanilla en `sounds.json` (`drone_hum`, `drone_deploy`, `lattice_link`, `lattice_scan`, `emp_blast`, `payload_arm`) y 5 recetas de supervivencia (`data/vision_tactical_eagleeye/recipes/*.json`).
  - Paridad completa de 16 idiomas (`en_us`, `es_es`, `es_mx`, `pt_br`, `fr_fr`, `de_de`, `it_it`, `ru_ru`, `ja_jp`, `ko_kr`, `zh_cn`, `zh_tw`, `pl_pl`, `tr_tr`, `uk_ua`, `nl_nl`) con 36 claves verificadas cada uno.
- Validación de compilación multi-loader (`:common:compileJava :forge:compileJava :fabric:compileJava`).
- Compilación final remapeada (`.\gradlew.bat build --no-daemon`) generando `forge/build/libs/vision-tactical-eagleeye-forge-1.0.0.jar` y `fabric/build/libs/vision-tactical-eagleeye-fabric-1.0.0.jar`, con despliegue verificado a las instancias de ATLauncher `Minecraft1201withForge` y `Minecraft1201withFabric`.
