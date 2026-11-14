# GeoArmorRenderer renders the complete 3D armor model in first person (no camera check)

## Environment

- Minecraft 1.21.1 / NeoForge 21.1.x
- GeckoLib `geckolib-neoforge-1.21.1-4.9.3`
- Client-only observation (no server code involved)

## Description

While in **first person**, GeckoLib-based armor is still rendered. Because `GeoArmorRenderer` is a standalone
`HumanoidModel` (it is not drawn as a layer of the vanilla `PlayerRenderer` body), whatever renders it in first person
gets the *whole* armor model — chest, legs, helmet plates — floating around the camera.

This affects every GeckoLib armor set, so different mods show the exact same symptom.

### Reproduced armor sets

| Mod | Armor | Renderer chain (verified with `javap`) |
|---|---|---|
| ScorchedGuns 1.5 | Adrien / Exosuit / Cog Knight / Redcoat / … | `…scguns.client.render.armor.*ArmorRenderer` (GeckoLib armor renderers) |
| Iron's Spells 'n Spellbooks 3.16.3 | **Gold Crown / Tarnished Crown** | `GoldCrownArmorItem` (GeoItem) → `GoldCrownModel extends GeoModel<GoldCrownArmorItem>` → `GenericCustomArmorRenderer extends GeoArmorRenderer` |

Additional check on Iron's side: all 15 of its armor model/renderer classes
(`GenericCustomArmorRenderer`, `DyeableArmorRenderer`, `PriestArmorRenderer`, `PumpkinArmorRenderer`,
`GenericArmorModel`, `ArchevokerArmorModel`, …) contain **zero** references to `CameraType` / `isFirstPerson`,
so the missing guard is inherited straight from GeckoLib.

Vanilla armour (`HumanoidArmorLayer`) is never visible in first person, because vanilla does not render the player's
armor layers in that camera mode at all. GeckoLib's renderer has no equivalent guard.

## Steps to reproduce

1. Install any mod that registers a `GeoArmorRenderer` armor set.
2. Equip the full set.
3. Press `F5` until the camera is in first person.
4. The armor model is still drawn, floating around the camera.

Expected: nothing of the armor is visible in first person (matching vanilla behaviour).
Actual: the complete armor model is rendered.

## Root cause

`software.bernie.geckolib.renderer.GeoArmorRenderer` extends `HumanoidModel` and decides which bones to draw purely from
the **visibility of the base model parts**:

```java
protected void applyBoneVisibilityByPart(EquipmentSlot slot, ModelPart part, HumanoidModel<?> baseModel) {
    // maps HumanoidModel.head/body/leftArm/rightArm/leftLeg/rightLeg visibility onto the matching GeoBone
}
```

`verify` with `javap` on the shipped class:

```
$ javap -c -p software.bernie.geckolib.renderer.GeoArmorRenderer | grep -i 'camera\|firstperson'
(no output — 0 hits)
```

So there is **no camera-type check anywhere in the class**. Whenever the renderer is invoked while the camera is in first
person (e.g. because some client mod renders the player's arms/layers in that pass), all bones are considered visible and
the full armor model is drawn.

## Suggested fix

Skip the render (or hide all bones) when the armor belongs to the local player and the camera is in first person.
`setAllBonesVisible(false)` already exists, so this is only a missing guard, not new functionality:

```java
// in GeoArmorRenderer#prepForRender(...) or at the start of render/applyBoneVisibilityByPart
Minecraft mc = Minecraft.getInstance();
if (mc.player != null
        && currentEntity == mc.player
        && mc.options.getCameraType().isFirstPerson()) {
    setAllBonesVisible(false);
    return; // or let the caller continue with an empty bone set
}
```

## Notes

- Third-person rendering must keep working; the guard has to be limited to `cameraType.isFirstPerson()` **and**
  `currentEntity == localPlayer`.
- A local mod-side workaround is possible (cancel `HumanoidArmorLayer#render` in first person for the local player), but
  it is a shotgun fix: it also kills legitimate renderers. The correct place is the GeckoLib renderer itself.
