# Changelog — Vision Tactical: EagleEye & Drones

## [1.0.1] - 2026-11-27
### Added & Fixed
- **Registro de Módulo en Estación de Modificación (`VisionTacticalEagleEye`):** Registrado `eagleeye_datalink_module` vía `VisionGogglesAPI.registerAddonModule` para permitir su instalación y extracción en las gafas dentro de la Estación de Modificación de `Vision Goggles`.
- **Dron 3D en Inventario y Sprites `Tactical Charcoal` (`TacticalDroneItem`, `TacticalDroneItemRenderer`, `scripts/polish_assets_and_lang.py`):** `TacticalDroneItem` implementa `GeoItem` con `GeoItemRenderer` renderizando `tactical_drone.geo.json` en 3D en GUI/mano/suelo; regeneradas las texturas `16×16` de `eagleeye_datalink_module`, `lattice_fpv_controller`, `drone_payload_emp` y `drone_payload_he` con la paleta `Tactical Charcoal` de `Vision Goggles`.
- **Creative Tab y Tooltips Compactos (16 idiomas):** Título acortado a `"Vision Tactical: EagleEye"` en todos los idiomas y tooltips largos divididos en 2 líneas cortas.
- **Modo FPV Limpio y Bloqueo de Inventario (`FpvGameRendererMixin`, `FpvGuiMixin`, `FpvInventoryMixin`, `MinecraftCameraMixin`, `EagleEyeHudOverlay`):** Cancelado `GameRenderer#renderItemInHand` en cámara FPV (sin brazo ni armas TaCZ), ocultadas hotbar/barras vanilla, bloqueados scroll/teclas `1..9`, inventario (`E`), drop (`Q`) y uso/ataque mientras se pilota el dron, y limpiadas las etiquetas `[TGT-xxx]` en pantalla (corchete compacto + barra HP en objetivos periféricos, etiqueta completa `[UAV]` solo en el objetivo central).
- **Guardas Estrictas del HUD AR Operador (`EagleEyeModuleItem`, `EagleEyeHudOverlay`):** El overlay AR fuera de FPV exige obligatoriamente las 3 condiciones simultáneas: gafas equipadas y encendidas (`isVisionActive`), módulo `eagleeye_datalink_module` instalado en esas gafas (`hasModule`) y al menos un dron aliado vivo con batería en rango (eliminado el bypass de sostener el controlador o tener un dron sin módulo).
- **Render 3D en Forge, Recarga `nvg_battery`, Feedback de Vinculación, Proyección Exacta y Vista EO/UAV (#175):**
  - Añadido `ForgeItemMixin` (`IClientItemExtensions#getCustomRenderer`) para que `TacticalDroneItemRenderer` renderice el dron 3D en inventario/mano en Forge (corrigiendo el ítem invisible).
  - Corregido el bug de `BAT 1%` (`getBatteryPct() * 100`) y añadida recarga con `vision_goggles:nvg_battery` (clic derecho sobre dron desplegado, clic derecho en inventario y receta `tactical_drone_recharge.json`).
  - Añadido estado NBT de vinculación en `LatticeControllerItem` (`isFoil`, tooltip verde `Vinculado: Dron #<id> (X, Y, Z)` o rojo `[SIN VINCULAR]` y mensaje `[LATTICE] Dron #<id> vinculado`).
  - Reducido el TTL de objetivos a `10` ticks (`0.5s`) con poda inmediata al perder línea de visión (`5` ticks de escaneo/sync) y proyección 3D→2D exacta con relación de aspecto GUI y `getPosition(partialTicks)`.
  - Añadido `FpvVisionShaderSuppressMixin` para suprimir el shader verde NVG durante el modo FPV y renderizar la cámara militar EO/UAV propia (tinte azul-pizarra frío, scanlines sutiles, viñeta óptica y retícula EO cian).
- **Aislamiento Total FPV, PostChain GLSL `uav_eo`, Vinculación Explícita y Cuerpo Quieto (#181):**
  - Añadidos `FpvVisionHudSuppressMixin` y `FpvVisionRendererSuppressMixin` para ocultar al 100% el HUD de `Vision Goggles` (batería/modo/biometría), suprimir el lightmap NVG y bloquear la tecla `N`/rueda radial/zoom mientras se pilota en FPV.
  - Creado PostChain GLSL dedicado `shaders/post/uav_eo.json` + `program/uav_eo.{json,fsh}` (`DroneEoShaderManager`, `PostChainAccessor`) con estética militar EO/UAV (desaturación fría azul-pizarra de alto contraste, realce nocturno propio, grano CMOS, scanlines y RF glitch dinámico).
  - Eliminada la auto-vinculación al colocar el dron (`TacticalDroneItem`): ahora se vincula explícitamente con Clic Derecho sobre el dron usando el controlador, y tanto `LatticeTrackManager` como `EagleEyeHudOverlay` muestran únicamente objetivos del dron vinculado.
  - Añadido `FpvLocalPlayerMixin` para congelar el cuerpo y la cabeza del operador (`aiStep`, `setSprinting`, velocidad horizontal `0`) mientras pilota en FPV.
- **Claridad Nocturna EO Real, Eliminación de Linterna del Dron y Escáner Sin X-Ray (#185):**
  - Eliminado el emisor de linterna/foco IR del dron (`DroneFpvClientController.emitDroneIrSpots`) y activado el lightmap nocturno limpio durante FPV (`FpvVisionRendererSuppressMixin#shouldApplyNightVisionLight` -> `true`) combinado con `uav_eo.fsh` calibrado para máxima claridad nocturna azul-pizarra sin verde NVG.
  - Exigido raycast real de oclusión `ClipContext.Block.VISUAL` + cono frontal de cámara (`dot > 0.55`) tanto en servidor (`TacticalDroneEntity#performLatticeScan`) como en cliente FPV (`EagleEyeHudOverlay#renderProjectedTracks`), eliminando el bypass X-Ray de `16m` y cualquier mob detrás de bloques o fuera de cámara.

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
