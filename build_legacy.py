import os
import re
import subprocess
import shutil
import sys

def run_cmd(cmd):
    print(f"Running: {cmd}")
    subprocess.run(cmd, shell=True, check=True)

def replace_exact(path, old_text, new_text, label="", replace_all=False):
    """Exact string replacement with verification."""
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    if old_text not in content:
        print(f"  !! FAILED: '{label}' not found in {os.path.basename(path)}")
        print(f"     Looking for: {repr(old_text[:80])}...")
        sys.exit(1)
    count = content.count(old_text)
    if replace_all:
        content = content.replace(old_text, new_text)
    else:
        content = content.replace(old_text, new_text, 1)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print(f"  OK: {label} ({count}x) in {os.path.basename(path)}")

def remove_line_containing(path, text, label=""):
    """Remove any line containing the given text."""
    with open(path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    new_lines = [l for l in lines if text not in l]
    if len(new_lines) == len(lines):
        print(f"  !! WARNING: '{label}' - no line containing '{text}' in {os.path.basename(path)}")
    else:
        print(f"  OK: {label} in {os.path.basename(path)}")
    with open(path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)

def build_version(mc, yarn, fapi, out_jar):
    print(f"\n{'='*60}")
    print(f"  Building {mc}")
    print(f"{'='*60}")
    props = f"""# Done to increase the memory available to gradle.
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true

# Fabric Properties
minecraft_version={mc}
yarn_mappings={yarn}
loader_version=0.16.14
loom_version=1.11-SNAPSHOT

# Mod Properties
mod_version=1.0.0
maven_group=com.pvpmod
archives_base_name=IWantToHitFromThreeBlocks

# Dependencies
fabric_api_version={fapi}
"""
    with open("gradle.properties", "w", encoding="utf-8") as f:
        f.write(props)
    run_cmd(".\\gradlew clean build")
    shutil.copy("build/libs/IWantToHitFromThreeBlocks-1.0.0.jar", f"versions/{out_jar}")
    print(f"  Successfully built {mc} -> versions/{out_jar}")

# ============================================================
# Files that need version-specific patches
# ============================================================
BACKUP_FILES = [
    ("src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java", "InGameHudMixin.java.bak"),
    ("src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java", "WorldRendererMixin.java.bak"),
    ("src/client/java/com/pvpmod/client/mixin/TitleScreenMixin.java", "TitleScreenMixin.java.bak"),
    ("src/client/java/com/pvpmod/client/render/ReachOverlay3DRenderer.java", "ReachOverlay3DRenderer.java.bak"),
    ("src/client/java/com/pvpmod/client/render/ReachOverlayRenderer.java", "ReachOverlayRenderer.java.bak"),
    ("src/client/java/com/pvpmod/client/PvpModClient.java", "PvpModClient.java.bak"),
    ("src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java", "HitDistanceEditScreen.java.bak"),
    ("src/client/java/com/pvpmod/client/config/ReachOverlayConfigScreen.java", "ReachOverlayConfigScreen.java.bak"),
    ("src/client/java/com/pvpmod/client/hud/HitDistanceHud.java", "HitDistanceHud.java.bak"),
    ("src/client/java/com/pvpmod/client/update/UpdateChecker.java", "UpdateChecker.java.bak"),
    ("src/client/java/com/pvpmod/client/update/UpdateNotificationScreen.java", "UpdateNotificationScreen.java.bak"),
]

# Backup all files
for src, bak in BACKUP_FILES:
    shutil.copy(src, bak)

try:
    # ================================================================
    # 1.21.2 BUILD (closest to 1.21.1 base, built first)
    # ================================================================
    print("\n--- Applying 1.21.2-specific patches ---")

    # 1.21.2: setShaderColor no longer tints drawGuiTexture with RenderLayer pipeline
    # Replace entire crosshair rendering block to use drawGuiTexture's color parameter
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, cx, cy, 15, 15);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);""",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    int crossColor = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
                    context.drawGuiTexture(RenderLayer::getGuiTextured, CROSSHAIR_TEXTURE, cx, cy, 15, 15, crossColor);""",
        "1.21.2 InGameHudMixin crosshair color param"
    )

    # 1.21.2: WorldRenderer.render() gains ObjectAllocator as first parameter
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {""",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            net.minecraft.client.util.ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {""",
        "1.21.2 WorldRendererMixin ObjectAllocator param"
    )

    try:
        build_version("1.21.2", "1.21.2+build.1", "0.106.1+1.21.2",
                       "IWantToHitFromThreeBlocks-v1.0.0-1.21.2+1.21.3.jar")
    except subprocess.CalledProcessError:
        print("!! 1.21.2 build FAILED!")

    # Restore all files back to 1.21.1 state before 1.21.4 patches
    print("\n--- Restoring files for 1.21.4 build ---")
    for src, bak in BACKUP_FILES:
        shutil.copy(bak, src)

    # ================================================================
    # 1.21.4 BUILD
    # Same as 1.21.2 patches + WorldRenderer loses LightmapTextureManager
    # ================================================================
    print("\n--- Applying 1.21.4-specific patches ---")

    # 1.21.4: crosshair color via drawGuiTexture color param (same as 1.21.2)
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, cx, cy, 15, 15);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);""",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    int crossColor = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
                    context.drawGuiTexture(RenderLayer::getGuiTextured, CROSSHAIR_TEXTURE, cx, cy, 15, 15, crossColor);""",
        "1.21.4 InGameHudMixin crosshair color param"
    )

    # 1.21.4: WorldRenderer.render() has ObjectAllocator AND loses LightmapTextureManager
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {""",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            net.minecraft.client.util.ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {""",
        "1.21.4 WorldRendererMixin ObjectAllocator + no LightmapTextureManager"
    )

    # Remove unused LightmapTextureManager import
    remove_line_containing(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "import net.minecraft.client.render.LightmapTextureManager;",
        "1.21.4 Remove LightmapTextureManager import"
    )

    try:
        build_version("1.21.4", "1.21.4+build.8", "0.119.4+1.21.4",
                       "IWantToHitFromThreeBlocks-v1.0.0-1.21.4.jar")
    except subprocess.CalledProcessError:
        print("!! 1.21.4 build FAILED!")

    # Restore all files back to 1.21.1 state before 1.21.5 patches
    print("\n--- Restoring files for 1.21.5 build ---")
    for src, bak in BACKUP_FILES:
        shutil.copy(bak, src)

    # ================================================================
    # 1.21.5 BUILD
    # Same as 1.21.4 + getTickDelta->getTickProgress + enableBlend/defaultBlendFunc removed
    # ================================================================
    print("\n--- Applying 1.21.5-specific patches ---")

    # 1.21.5: crosshair color via drawGuiTexture color param (same as 1.21.2+)
    # Also remove enableBlend/defaultBlendFunc which no longer exist
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, cx, cy, 15, 15);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);""",
        """                    int crossColor = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
                    context.drawGuiTexture(RenderLayer::getGuiTextured, CROSSHAIR_TEXTURE, cx, cy, 15, 15, crossColor);""",
        "1.21.5 InGameHudMixin crosshair color + remove blend"
    )

    # 1.21.5: WorldRenderer.render() same as 1.21.4 (ObjectAllocator, no LightmapTextureManager)
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {""",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            net.minecraft.client.util.ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {""",
        "1.21.5 WorldRendererMixin ObjectAllocator + no LightmapTextureManager"
    )

    # Remove unused LightmapTextureManager import
    remove_line_containing(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "import net.minecraft.client.render.LightmapTextureManager;",
        "1.21.5 Remove LightmapTextureManager import"
    )

    # 1.21.5: RenderTickCounter.getTickDelta() -> getTickProgress()
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "tickCounter.getTickDelta(true)",
        "tickCounter.getTickProgress(true)",
        "1.21.5 WorldRendererMixin getTickProgress"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/PvpModClient.java",
        "tickCounter.getTickDelta(true)",
        "tickCounter.getTickProgress(true)",
        "1.21.5 PvpModClient getTickProgress"
    )

    # 1.21.5: RenderSystem.enableBlend() and defaultBlendFunc() removed
    # Remove from HitDistanceEditScreen
    replace_exact(
        "src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java",
        """        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
""",
        "",
        "1.21.5 HitDistanceEditScreen remove enableBlend"
    )
    # Remove from ReachOverlayConfigScreen


    try:
        build_version("1.21.5", "1.21.5+build.1", "0.128.2+1.21.5",
                       "IWantToHitFromThreeBlocks-v1.0.0-1.21.5.jar")
    except subprocess.CalledProcessError:
        print("!! 1.21.5 build FAILED!")

    # Restore all files back to 1.21.1 state before 1.21.6 patches
    print("\n--- Restoring files for 1.21.6 build ---")
    for src, bak in BACKUP_FILES:
        shutil.copy(bak, src)

    # ================================================================
    # 1.21.6 BUILD
    # Major rendering pipeline overhaul:
    # - Matrix3x2fStack: push->pushMatrix, pop->popMatrix, translate/scale 2D
    # - drawGuiTexture: Function<Id,RenderLayer> -> RenderPipeline
    # - RenderSystem.enableBlend/defaultBlendFunc/setShaderColor removed
    # - WorldRenderer.render: completely new signature
    # - getTickDelta -> getTickProgress (same as 1.21.5)
    # ================================================================
    print("\n--- Applying 1.21.6-specific patches ---")

    # --- 1. Matrix3x2fStack: push/pop -> pushMatrix/popMatrix ---
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "context.getMatrices().push();",
        "context.getMatrices().pushMatrix();",
        "1.21.6 InGameHudMixin pushMatrix"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "context.getMatrices().pop();",
        "context.getMatrices().popMatrix();",
        "1.21.6 InGameHudMixin popMatrix"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java",
        "ctx.getMatrices().push();",
        "ctx.getMatrices().pushMatrix();",
        "1.21.6 HitDistanceEditScreen pushMatrix",
        replace_all=True
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java",
        "ctx.getMatrices().pop();",
        "ctx.getMatrices().popMatrix();",
        "1.21.6 HitDistanceEditScreen popMatrix",
        replace_all=True
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/hud/HitDistanceHud.java",
        "ctx.getMatrices().push();",
        "ctx.getMatrices().pushMatrix();",
        "1.21.6 HitDistanceHud pushMatrix"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/hud/HitDistanceHud.java",
        "ctx.getMatrices().pop();",
        "ctx.getMatrices().popMatrix();",
        "1.21.6 HitDistanceHud popMatrix"
    )

    # --- 2. translate/scale: 3D -> 2D (remove z component) ---
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "context.getMatrices().translate(centerX, crosshairTop - 2, 0);",
        "context.getMatrices().translate(centerX, crosshairTop - 2);",
        "1.21.6 InGameHudMixin translate 2D"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "context.getMatrices().scale(HIT_TEXT_SCALE, HIT_TEXT_SCALE, 1.0f);",
        "context.getMatrices().scale(HIT_TEXT_SCALE, HIT_TEXT_SCALE);",
        "1.21.6 InGameHudMixin scale 2D"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java",
        "ctx.getMatrices().translate(hudPixelX, hudPixelY, 0);",
        "ctx.getMatrices().translate(hudPixelX, hudPixelY);",
        "1.21.6 HitDistanceEditScreen translate HUD 2D"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java",
        "ctx.getMatrices().scale(hudScale, hudScale, 1.0f);",
        "ctx.getMatrices().scale(hudScale, hudScale);",
        "1.21.6 HitDistanceEditScreen scale 2D"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java",
        "ctx.getMatrices().translate(0, 0, 200);",
        "ctx.getMatrices().translate(0, 0);",
        "1.21.6 HitDistanceEditScreen translate context menu 2D"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/hud/HitDistanceHud.java",
        "ctx.getMatrices().translate(posX, posY, 0);",
        "ctx.getMatrices().translate(posX, posY);",
        "1.21.6 HitDistanceHud translate 2D"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/hud/HitDistanceHud.java",
        "ctx.getMatrices().scale(scale, scale, 1.0f);",
        "ctx.getMatrices().scale(scale, scale);",
        "1.21.6 HitDistanceHud scale 2D"
    )

    # --- 3. Crosshair rendering: remove blend + setShaderColor, use RenderPipeline + color param ---
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, cx, cy, 15, 15);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);""",
        """                    int crossColor = ((int)(a * 255) << 24) | ((int)(r * 255) << 16) | ((int)(g * 255) << 8) | (int)(b * 255);
                    context.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, CROSSHAIR_TEXTURE, cx, cy, 15, 15, crossColor);""",
        "1.21.6 InGameHudMixin crosshair RenderPipeline + color"
    )

    # --- 4. enableBlend/defaultBlendFunc removed ---
    replace_exact(
        "src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java",
        """        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
""",
        "",
        "1.21.6 HitDistanceEditScreen remove enableBlend"
    )



    # --- 6. WorldRenderer.render(): completely new signature ---
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {""",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            net.minecraft.client.util.ObjectAllocator allocator,
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            com.mojang.blaze3d.buffers.GpuBufferSlice lightData,
            org.joml.Vector4f fogColor,
            boolean fogEnabled,
            CallbackInfo ci) {""",
        "1.21.6 WorldRendererMixin new render signature"
    )

    # Remove unused imports
    remove_line_containing(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "import net.minecraft.client.render.LightmapTextureManager;",
        "1.21.6 Remove LightmapTextureManager import"
    )
    remove_line_containing(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "import net.minecraft.client.render.GameRenderer;",
        "1.21.6 Remove GameRenderer import"
    )

    # --- 7. getTickDelta -> getTickProgress (same as 1.21.5) ---
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "tickCounter.getTickDelta(true)",
        "tickCounter.getTickProgress(true)",
        "1.21.6 WorldRendererMixin getTickProgress"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/PvpModClient.java",
        "tickCounter.getTickDelta(true)",
        "tickCounter.getTickProgress(true)",
        "1.21.6 PvpModClient getTickProgress"
    )

    # --- 8. Fix alpha channels for text rendering in 1.21.6 ---
    with open("src/client/java/com/pvpmod/client/config/ReachOverlayConfigScreen.java", "r", encoding="utf-8") as f:
        content = f.read()
    content = content.replace("0xFFFFFF", "0xFFFFFFFF").replace("0xAAAAAA", "0xFFAAAAAA").replace("0x666666", "0xFF666666").replace("0xAABBFF", "0xFFAABBFF").replace("0x888888", "0xFF888888")
    with open("src/client/java/com/pvpmod/client/config/ReachOverlayConfigScreen.java", "w", encoding="utf-8") as f:
        f.write(content)

    try:
        build_version("1.21.6", "1.21.6+build.1", "0.128.2+1.21.6",
                       "IWantToHitFromThreeBlocks-v1.0.0-1.21.6.jar")
    except subprocess.CalledProcessError:
        print("!! 1.21.6 build FAILED!")

    # Restore all files back to 1.21.1 state before 1.20.x patches
    print("\n--- Restoring files for 1.20.x builds ---")
    for src, bak in BACKUP_FILES:
        shutil.copy(bak, src)

    # ================================================================
    # COMMON PATCHES for all 1.20.x versions
    # ================================================================
    print("\n--- Applying COMMON patches ---")

    # 1. InGameHudMixin: Remove RenderTickCounter import
    remove_line_containing(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "import net.minecraft.client.render.RenderTickCounter;",
        "Remove RenderTickCounter import"
    )

    # 2. InGameHudMixin: Remove RenderTickCounter from method signature
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "pvpReachOverlay$afterCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci)",
        "pvpReachOverlay$afterCrosshair(DrawContext context, CallbackInfo ci)",
        "Remove RenderTickCounter param"
    )

    # 3. PvpModClient: HudRenderCallback uses float tickDelta in 1.20.x
    replace_exact(
        "src/client/java/com/pvpmod/client/PvpModClient.java",
        "HudRenderCallback.EVENT.register((ctx, tickCounter) -> {\n            hitHud.render(ctx, tickCounter.getTickDelta(true));\n        });",
        "HudRenderCallback.EVENT.register((ctx, tickDelta) -> { hitHud.render(ctx, tickDelta); });",
        "HudRenderCallback tickDelta"
    )

    # 4. WorldRendererMixin: Change 1.21 render signature to 1.20.x
    # Remove RenderTickCounter import
    remove_line_containing(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "import net.minecraft.client.render.RenderTickCounter;",
        "Remove RenderTickCounter import from WorldRendererMixin"
    )

    # Replace the entire method signature and body
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            RenderTickCounter tickCounter,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            Matrix4f positionMatrix,
            Matrix4f projectionMatrix,
            CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        float tickDelta = tickCounter.getTickDelta(true);

        // Create our own MatrixStack and apply the camera position matrix
        MatrixStack matrices = new MatrixStack();
        matrices.multiplyPositionMatrix(positionMatrix);""",
        """    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(
            net.minecraft.client.util.math.MatrixStack matrices,
            float tickDelta,
            long limitTime,
            boolean renderBlockOutline,
            Camera camera,
            GameRenderer gameRenderer,
            LightmapTextureManager lightmapTextureManager,
            org.joml.Matrix4f projectionMatrix,
            CallbackInfo ci) {

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;

        // Create our own MatrixStack from the camera-transformed one
        MatrixStack renderMatrices = new MatrixStack();
        renderMatrices.multiplyPositionMatrix(matrices.peek().getPositionMatrix());""",
        "WorldRendererMixin method signature"
    )

    # Replace references to 'matrices' with 'renderMatrices' in the renderer calls
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "PvpModClient.getRenderer3D().render(matrices, consumers, camera.getPos(), tickDelta);",
        "PvpModClient.getRenderer3D().render(renderMatrices, consumers, camera.getPos(), tickDelta);",
        "WorldRendererMixin 3D renderer call"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/WorldRendererMixin.java",
        "PvpModClient.getRenderer().render(matrices, consumers, camera.getPos(), tickDelta);",
        "PvpModClient.getRenderer().render(renderMatrices, consumers, camera.getPos(), tickDelta);",
        "WorldRendererMixin 2D renderer call"
    )

    # 5. ReachOverlayRenderer: getLerpedPos doesn't exist in 1.20.x
    replace_exact(
        "src/client/java/com/pvpmod/client/render/ReachOverlayRenderer.java",
        """        // Interpolated positions using getLerpedPos
        Vec3d lerpedPos = player.getLerpedPos(td);
        double px = lerpedPos.x;
        double py = lerpedPos.y;
        double pz = lerpedPos.z;""",
        """        // Interpolated positions (manual lerp for 1.20.x compat)
        double px = net.minecraft.util.math.MathHelper.lerp(td, player.prevX, player.getX());
        double py = net.minecraft.util.math.MathHelper.lerp(td, player.prevY, player.getY());
        double pz = net.minecraft.util.math.MathHelper.lerp(td, player.prevZ, player.getZ());""",
        "ReachOverlayRenderer getLerpedPos"
    )

    # 6. ReachOverlay3DRenderer: getLerpedPos doesn't exist in 1.20.x
    replace_exact(
        "src/client/java/com/pvpmod/client/render/ReachOverlay3DRenderer.java",
        """        // Interpolated positions using getLerpedPos
        Vec3d lerpedPos = player.getLerpedPos(td);
        double px = lerpedPos.x;
        double py = lerpedPos.y;
        double pz = lerpedPos.z;""",
        """        // Interpolated positions (manual lerp for 1.20.x compat)
        double px = net.minecraft.util.math.MathHelper.lerp(td, player.prevX, player.getX());
        double py = net.minecraft.util.math.MathHelper.lerp(td, player.prevY, player.getY());
        double pz = net.minecraft.util.math.MathHelper.lerp(td, player.prevZ, player.getZ());""",
        "ReachOverlay3DRenderer getLerpedPos"
    )

    # 7. ReachOverlayRenderer: Add .next() to vertex calls (required in 1.20.x, removed in 1.21)
    replace_exact(
        "src/client/java/com/pvpmod/client/render/ReachOverlayRenderer.java",
        ".color(r, g, b, a);\n",
        ".color(r, g, b, a).next();\n",
        "ReachOverlayRenderer quad .next()",
        replace_all=True
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/render/ReachOverlayRenderer.java",
        ".normal(0, 1, 0);\n",
        ".normal(0, 1, 0).next();\n",
        "ReachOverlayRenderer line .next()",
        replace_all=True
    )

    # 8. ReachOverlay3DRenderer: Add .next() to vertex calls
    replace_exact(
        "src/client/java/com/pvpmod/client/render/ReachOverlay3DRenderer.java",
        ".color(r, g, b, a);\n",
        ".color(r, g, b, a).next();\n",
        "ReachOverlay3DRenderer quad .next()",
        replace_all=True
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/render/ReachOverlay3DRenderer.java",
        ".normal(0, 1, 0);\n",
        ".normal(0, 1, 0).next();\n",
        "ReachOverlay3DRenderer line .next()",
        replace_all=True
    )

    # 9. Reach: getEntityInteractionRange() doesn't exist in 1.20.x, use hardcoded fallback
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "player.getEntityInteractionRange()",
        "(player.isCreative() ? 5.0 : 3.0)",
        "1.20.x InGameHudMixin hardcoded reach"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/render/ReachOverlayRenderer.java",
        "player.getEntityInteractionRange()",
        "(player.isCreative() ? 5.0 : 3.0)",
        "1.20.x ReachOverlayRenderer hardcoded reach"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/render/ReachOverlay3DRenderer.java",
        "player.getEntityInteractionRange()",
        "(player.isCreative() ? 5.0 : 3.0)",
        "1.20.x ReachOverlay3DRenderer hardcoded reach"
    )

    # 10. HitDistanceEditScreen: No dirt texture patches needed (removed in favor of solid dark bg)

    # ================================================================
    # 1.20.2 BUILD
    # ================================================================
    print("\n--- Applying 1.20.2-specific patches ---")

    # 1.20.2: Identifier.of -> new Identifier (drawGuiTexture EXISTS in 1.20.2)
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        'Identifier.of("hud/crosshair")',
        'new Identifier("hud/crosshair")',
        "1.20.2 Identifier.of -> new Identifier"
    )

    # 1.20.2: Add context.draw() flushes to fix black square
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, cx, cy, 15, 15);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);""",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    context.draw();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, cx, cy, 15, 15);
                    context.draw();
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);""",
        "1.20.2 crosshair flush"
    )



    try:
        build_version("1.20.2", "1.20.2+build.4", "0.91.6+1.20.2",
                       "IWantToHitFromThreeBlocks-v1.0.0-1.20.2.jar")
    except subprocess.CalledProcessError:
        print("!! 1.20.2 build FAILED!")

    # ================================================================
    # 1.20.1 BUILD
    # Revert InGameHudMixin and re-apply common + 1.20.1 patches
    # ================================================================
    print("\n--- Reverting InGameHudMixin for 1.20.1 ---")
    shutil.copy("InGameHudMixin.java.bak", "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java")

    # Re-apply common InGameHudMixin patches
    remove_line_containing(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "import net.minecraft.client.render.RenderTickCounter;",
        "Re-apply: Remove RenderTickCounter import"
    )
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "pvpReachOverlay$afterCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci)",
        "pvpReachOverlay$afterCrosshair(DrawContext context, CallbackInfo ci)",
        "Re-apply: Remove RenderTickCounter param"
    )

    # Re-apply reach downgrade (backup has 1.21.1 source with getEntityInteractionRange)
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        "player.getEntityInteractionRange()",
        "(player.isCreative() ? 5.0 : 3.0)",
        "Re-apply: 1.20.1 InGameHudMixin hardcoded reach"
    )

    print("\n--- Applying 1.20.1-specific patches ---")

    # 1.20.1: drawGuiTexture does NOT exist, use drawTexture with icons.png
    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        'Identifier.of("hud/crosshair")',
        'new Identifier("minecraft", "textures/gui/icons.png")',
        "1.20.1 Identifier -> icons.png"
    )

    replace_exact(
        "src/client/java/com/pvpmod/client/mixin/InGameHudMixin.java",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawGuiTexture(CROSSHAIR_TEXTURE, cx, cy, 15, 15);
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);""",
        """                    RenderSystem.enableBlend();
                    RenderSystem.defaultBlendFunc();
                    context.draw();
                    RenderSystem.setShaderColor(r, g, b, a);
                    context.drawTexture(CROSSHAIR_TEXTURE, cx, cy, 0, 0, 15, 15, 256, 256);
                    context.draw();
                    RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);""",
        "1.20.1 crosshair drawTexture"
    )

    # 1.20.1: renderBackground override signature change (4 params -> 1 param)
    replace_exact(
        "src/client/java/com/pvpmod/client/config/HitDistanceEditScreen.java",
        "public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {",
        "public void renderBackground(DrawContext context) {",
        "1.20.1 HitDistanceEditScreen renderBackground override sig"
    )
    # 1.20.1: UpdateNotificationScreen renderBackground override signature (4 params -> 1 param)
    replace_exact(
        "src/client/java/com/pvpmod/client/update/UpdateNotificationScreen.java",
        "public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {",
        "public void renderBackground(DrawContext ctx) {",
        "1.20.1 UpdateNotificationScreen renderBackground sig"
    )



    # 1.20.1: client.getDebugHud().shouldShowDebugHud() -> client.options.debugEnabled
    replace_exact(
        "src/client/java/com/pvpmod/client/hud/HitDistanceHud.java",
        "client.getDebugHud().shouldShowDebugHud()",
        "client.options.debugEnabled",
        "1.20.1 debugEnabled"
    )

    try:
        build_version("1.20.1", "1.20.1+build.10", "0.92.8+1.20.1",
                       "IWantToHitFromThreeBlocks-v1.0.0-1.20+1.20.1.jar")
    except subprocess.CalledProcessError:
        print("!! 1.20.1 build FAILED!")

finally:
    # Restore all original files
    for src, bak in BACKUP_FILES:
        if os.path.exists(bak):
            shutil.move(bak, src)

    # Restore gradle.properties for 1.21.1
    with open("gradle.properties", "w", encoding="utf-8") as f:
        f.write("""# Done to increase the memory available to gradle.
org.gradle.jvmargs=-Xmx1G
org.gradle.parallel=true

# Fabric Properties
minecraft_version=1.21.1
yarn_mappings=1.21.1+build.3
loader_version=0.16.14
loom_version=1.11-SNAPSHOT

# Mod Properties
mod_version=1.0.0
maven_group=com.pvpmod
archives_base_name=IWantToHitFromThreeBlocks

# Dependencies
fabric_api_version=0.102.0+1.21.1
""")

print("\n" + "="*60)
print("  All builds completed! Files restored to 1.21.1 state.")
print("="*60)


