/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.Nonnull
 *  javax.annotation.Nullable
 *  net.minecraft.client.Minecraft
 *  net.minecraft.client.multiplayer.ClientLevel
 *  net.minecraft.client.renderer.ItemBlockRenderTypes
 *  net.minecraft.client.renderer.RenderType
 *  net.minecraft.client.renderer.block.BlockModelShaper
 *  net.minecraft.client.renderer.block.model.BakedQuad
 *  net.minecraft.client.renderer.block.model.BlockModel
 *  net.minecraft.client.renderer.texture.SpriteContents
 *  net.minecraft.client.renderer.texture.TextureAtlasSprite
 *  net.minecraft.client.resources.model.BakedModel
 *  net.minecraft.client.resources.model.ModelResourceLocation
 *  net.minecraft.client.resources.model.UnbakedModel
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.BlockPos$MutableBlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.core.Direction$Plane
 *  net.minecraft.core.Holder
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraft.tags.BlockTags
 *  net.minecraft.util.Mth
 *  net.minecraft.util.RandomSource
 *  net.minecraft.world.inventory.InventoryMenu
 *  net.minecraft.world.level.BlockAndTintGetter
 *  net.minecraft.world.level.BlockGetter
 *  net.minecraft.world.level.EmptyBlockGetter
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.LightLayer
 *  net.minecraft.world.level.biome.Biome
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.Blocks
 *  net.minecraft.world.level.block.DirtPathBlock
 *  net.minecraft.world.level.block.FarmBlock
 *  net.minecraft.world.level.block.FenceBlock
 *  net.minecraft.world.level.block.FenceGateBlock
 *  net.minecraft.world.level.block.HalfTransparentBlock
 *  net.minecraft.world.level.block.IronBarsBlock
 *  net.minecraft.world.level.block.LeavesBlock
 *  net.minecraft.world.level.block.RenderShape
 *  net.minecraft.world.level.block.SlabBlock
 *  net.minecraft.world.level.block.SnowLayerBlock
 *  net.minecraft.world.level.block.SnowyDirtBlock
 *  net.minecraft.world.level.block.StairBlock
 *  net.minecraft.world.level.block.VineBlock
 *  net.minecraft.world.level.block.state.BlockState
 *  net.minecraft.world.level.block.state.properties.BlockStateProperties
 *  net.minecraft.world.level.block.state.properties.Half
 *  net.minecraft.world.level.block.state.properties.Property
 *  net.minecraft.world.level.block.state.properties.SlabType
 *  net.minecraft.world.level.block.state.properties.StairsShape
 *  net.minecraft.world.level.levelgen.Heightmap$Types
 *  net.minecraft.world.phys.shapes.VoxelShape
 *  net.neoforged.neoforge.client.ChunkRenderTypeSet
 *  net.neoforged.neoforge.client.model.data.ModelData
 */
package com.teamtea.eclipticseasons.client.core;

import com.teamtea.eclipticseasons.EclipticSeasons;
import com.teamtea.eclipticseasons.api.constant.tag.EclipticBlockTags;
import com.teamtea.eclipticseasons.api.data.client.model.ESModelLoadedJson;
import com.teamtea.eclipticseasons.api.data.client.model.ModelResolver;
import com.teamtea.eclipticseasons.api.data.client.model.ModelTester;
import com.teamtea.eclipticseasons.api.data.client.model.seasonal.SeasonBlockDefinition;
import com.teamtea.eclipticseasons.api.data.client.model.seasonal.SeasonalTexture;
import com.teamtea.eclipticseasons.api.data.season.SnowDefinition;
import com.teamtea.eclipticseasons.api.misc.client.IExtraRendererContextOwner;
import com.teamtea.eclipticseasons.api.misc.client.IMapSlice;
import com.teamtea.eclipticseasons.api.misc.client.IMapSliceProvider;
import com.teamtea.eclipticseasons.api.misc.client.ISnowyBlockState;
import com.teamtea.eclipticseasons.api.util.EclipticUtil;
import com.teamtea.eclipticseasons.client.core.ExtraRendererContext;
import com.teamtea.eclipticseasons.client.model.IESReplaceModel;
import com.teamtea.eclipticseasons.client.model.ISnowyReplaceModel;
import com.teamtea.eclipticseasons.client.model.SnowyBakedModelWrapper;
import com.teamtea.eclipticseasons.client.model.SnowySeasonBakeModel;
import com.teamtea.eclipticseasons.client.model.TempReplaceModelWrapper;
import com.teamtea.eclipticseasons.client.model.bakequad.BakedQuadRetexturedAndReUV;
import com.teamtea.eclipticseasons.client.model.bakequad.DirectionMask;
import com.teamtea.eclipticseasons.client.model.bakequad.QuadFixer;
import com.teamtea.eclipticseasons.client.model.unbake.SolarBlockModel;
import com.teamtea.eclipticseasons.client.reload.ClientJsonCacheListener;
import com.teamtea.eclipticseasons.client.util.ClientCon;
import com.teamtea.eclipticseasons.client.util.ClientRef;
import com.teamtea.eclipticseasons.common.core.biome.WeatherManager;
import com.teamtea.eclipticseasons.common.core.map.MapChecker;
import com.teamtea.eclipticseasons.common.core.map.SnowyRemover;
import com.teamtea.eclipticseasons.common.core.snow.SnowChecker;
import com.teamtea.eclipticseasons.common.registry.BlockRegistry;
import com.teamtea.eclipticseasons.compat.CompatModule;
import com.teamtea.eclipticseasons.compat.Platform;
import com.teamtea.eclipticseasons.compat.ctm.CtmLoader;
import com.teamtea.eclipticseasons.compat.ctm.CtmProperties;
import com.teamtea.eclipticseasons.config.ClientConfig;
import com.teamtea.eclipticseasons.config.CommonConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirtPathBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.SnowyDirtBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.StairsShape;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;

public class ExtraModelManager {
    public static HashMap<ResourceLocation, SpriteContents> blocksCache = new HashMap();
    public static List<BakedQuad> EMPTY_BAKED_QUAD_LIST = List.of();
    public static int loadVersion = 0;
    public static Map<ModelResourceLocation, BakedModel> models;
    public static ModelResourceLocation snowOverlayLeaves;
    public static ModelResourceLocation snowySlabBottom;
    public static ModelResourceLocation snowOverlayBlock;
    public static ModelResourceLocation ice;
    public static ModelResourceLocation snowy_custom;
    public static ModelResourceLocation snowy_custom_ao;
    public static ModelResourceLocation stairs_top;
    public static ModelResourceLocation snowy_leaves_attach;
    public static ModelResourceLocation snowy_leaves_top;
    public static ModelResourceLocation snowy_fern;
    public static ModelResourceLocation snowy_grass;
    public static ModelResourceLocation snowy_large_fern_bottom;
    public static ModelResourceLocation snowy_large_fern_top;
    public static ModelResourceLocation snowy_tall_grass_bottom;
    public static ModelResourceLocation snowy_tall_grass_top;
    public static ModelResourceLocation overlay_2;
    public static ModelResourceLocation snow_height2;
    public static ModelResourceLocation snow_height2_top;
    public static ModelResourceLocation grass_flower;
    public static List<ModelResourceLocation> flower_on_grass;
    public static List<ModelResourceLocation> snow_edge_overlays;
    public static List<ModelResourceLocation> fourleaf_clovers;
    public static ResourceLocation snow;
    public static ResourceLocation snow_overlay_half_left;
    public static ResourceLocation snow_overlay_half_right;
    public static ResourceLocation snow_overlay;
    public static ResourceLocation snow_overlay_leaves;
    public static ResourceLocation snow_overlay_tiny;
    public static ResourceLocation snow_spot_overlay_leaves;
    public static Map<Block, CtmProperties> ctmStates;
    public static Map<ResourceLocation, Void> ctmTiles;
    private static final List<BakedQuad> EMPTY;
    private static final List<Direction> HORIZONTAL_DIRECTIONS;
    private static final Direction[] SNOW_LAYER_DIRECTIONS_TO_CHECK;
    private static final BlockPos[] SNOW_LAYER_DIRECTIONS_TO_CHECK_4;
    private static final BlockPos[] SNOW_LAYER_DIRECTIONS_TO_CHECK_8;
    public static final Map<ResourceLocation, ESModelLoadedJson> extraSnowModels;
    public static final Map<ResourceLocation, ModelResolver> extraSnowModelBuilds;
    public static final Map<ResourceLocation, List<SeasonalTexture>> SEASONAL_TEXTURE_HASH_MAP;

    public static ModelResourceLocation mrl(String s) {
        return ModelResourceLocation.standalone((ResourceLocation)EclipticSeasons.rl(s));
    }

    public static ModelResourceLocation extra_mrl(ResourceLocation resourceLocation, String v) {
        return ModelResourceLocation.standalone((ResourceLocation)resourceLocation.withPrefix("extra/" + (String)(v.isEmpty() ? "" : v + "/")));
    }

    public static ResourceLocation textureRL(String s) {
        return EclipticSeasons.rl("block/" + s);
    }

    public static TextureAtlasSprite getSprite(ResourceLocation resourceLocation) {
        return (TextureAtlasSprite)Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(resourceLocation);
    }

    private static boolean shouldCutoutMipped(BlockState state) {
        Block onBlock;
        if (!(Minecraft.getInstance().level == null || (onBlock = state.getBlock()) instanceof FenceBlock || onBlock instanceof HalfTransparentBlock || onBlock instanceof IronBarsBlock)) {
            if (onBlock instanceof SlabBlock || onBlock instanceof FarmBlock || onBlock instanceof DirtPathBlock || onBlock instanceof StairBlock || state.isSolidRender((BlockGetter)EmptyBlockGetter.INSTANCE, BlockPos.ZERO)) {
                return true;
            }
            return true;
        }
        return false;
    }

    public static boolean isSpecialCTMBlock(BlockState blockState) {
        if (ctmStates.isEmpty()) {
            return false;
        }
        CtmProperties orDefault = ctmStates.getOrDefault(blockState.getBlock(), null);
        return orDefault != null && orDefault.matches(blockState);
    }

    public static boolean isSpecialCTMSprite(TextureAtlasSprite sprite) {
        if (ctmTiles.isEmpty()) {
            return false;
        }
        try {
            SpriteContents spriteContents = sprite.contents();
            return ctmTiles.containsKey(spriteContents.name());
        }
        catch (Exception exception) {
            EclipticSeasons.logger(exception);
            return false;
        }
    }

    public static void initCTMDetected() {
        long l = System.currentTimeMillis();
        ctmStates.clear();
        ctmTiles.clear();
        CtmLoader.CTMLoadingResult ctmLoadingResult = CtmLoader.loadAll(Minecraft.getInstance().getResourceManager());
        ctmStates = ctmLoadingResult.ctmStates;
        ctmTiles = ctmLoadingResult.ctmTiles;
        EclipticSeasons.logger("CTM detector cost %s ms".formatted(System.currentTimeMillis() - l));
    }

    public static BakedModel getSnowyModel(BlockState state, BlockState snowState, int flag, int offset) {
        BakedModel snowModel;
        ISnowyBlockState snowyBlockState = (ISnowyBlockState)state;
        boolean notSpecialLeaves = !MapChecker.leaveLike(flag) || snowState != null;
        BakedModel bakedModel = snowModel = notSpecialLeaves ? snowyBlockState.getSnowyModel(loadVersion) : snowyBlockState.getSnowyModel2(loadVersion);
        if (snowModel == null) {
            Block onBlock = state.getBlock();
            boolean forceReplace = false;
            List<SnowDefinition> snowDefinitions = ClientRef.snowClientDef.get(onBlock);
            if (snowDefinitions != null && !snowDefinitions.isEmpty()) {
                for (SnowDefinition snowDefinition : snowDefinitions) {
                    ModelTester mmrl;
                    ResourceLocation cinfo = notSpecialLeaves ? snowDefinition.getInfo().getMid() : snowDefinition.getInfo().getMid2();
                    ModelResolver smr = extraSnowModelBuilds.get(cinfo);
                    if (smr == null || (mmrl = smr.tryFind(state)) == null) continue;
                    snowModel = models.get(mmrl.modelResourceLocation());
                    forceReplace = mmrl.replace();
                    flag = snowDefinition.getInfo().getFlag();
                    break;
                }
            }
            if (snowModel == null) {
                if (flag == 1) {
                    snowModel = models.get(snowOverlayBlock);
                } else if (flag == 4) {
                    snowModel = (Boolean)CommonConfig.Snow.snowyTree.get() == false ? models.get(snowOverlayLeaves) : (notSpecialLeaves ? models.get(snowy_leaves_top) : models.get(snowy_leaves_attach));
                } else if (flag == 2) {
                    snowModel = models.get(snowySlabBottom);
                } else if (flag == 301) {
                    snowModel = models.get(stairs_top);
                } else if (flag == 3) {
                    if (snowState != null) {
                        snowModel = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(snowState);
                    }
                } else if (flag == 5) {
                    snowModel = onBlock == Blocks.SHORT_GRASS ? models.get(snowy_grass) : (onBlock == Blocks.FERN ? models.get(snowy_fern) : models.get(snowy_grass));
                } else if (flag == 501) {
                    snowModel = onBlock == Blocks.TALL_GRASS ? models.get(offset == 1 ? snowy_tall_grass_bottom : snowy_tall_grass_top) : (onBlock == Blocks.LARGE_FERN ? models.get(offset == 1 ? snowy_large_fern_bottom : snowy_large_fern_top) : models.get(offset == 1 ? snowy_tall_grass_bottom : snowy_tall_grass_top));
                } else if (flag == 7) {
                    if (snowState != null) {
                        snowModel = Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(snowState);
                    }
                } else if (flag == 6) {
                    snowModel = models.get(snow_height2_top);
                } else if (flag == 999) {
                    snowModel = models.get(snowy_custom);
                } else if (flag == 998) {
                    snowModel = models.get(snowy_custom_ao);
                } else if (flag == 1000 | flag == 1001 || flag == 1200 || flag == 1100 || flag == 1101) {
                    ModelTester mmrl;
                    SnowDefinition.Info uncacheSnow = SnowChecker.getUncacheSnow(state);
                    ResourceLocation cinfo = notSpecialLeaves ? uncacheSnow.getMid() : uncacheSnow.getMid2();
                    ModelResolver smr = extraSnowModelBuilds.get(cinfo);
                    if (smr != null && (mmrl = smr.tryFind(state)) != null) {
                        snowModel = models.get(mmrl.modelResourceLocation());
                        forceReplace = mmrl.replace();
                    }
                }
            }
            if (snowModel != null) {
                SnowyBakedModelWrapper<Object> bakedModel2 = snowModel instanceof SnowyBakedModelWrapper ? (SnowyBakedModelWrapper<Object>)snowModel : new SnowyBakedModelWrapper<BakedModel>(snowModel);
                bakedModel2.setReplace(forceReplace);
                if (ISnowyReplaceModel.isInvalid((BakedModel)bakedModel2)) {
                    bakedModel2.updateBlockType(flag);
                    bakedModel2.setLowLayer(!notSpecialLeaves);
                }
                if (notSpecialLeaves) {
                    snowyBlockState.setSnowyModel((BakedModel)bakedModel2, loadVersion);
                } else {
                    snowyBlockState.setSnowyModel2((BakedModel)bakedModel2, loadVersion);
                }
            }
        }
        return snowModel;
    }

    public static List<BakedQuad> cancelTop(@Nullable BakedModel bakedModel, @Nonnull BlockAndTintGetter blockAndTintGetter, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nullable Direction direction, @Nonnull RandomSource random, long seed, @Nonnull List<BakedQuad> original, @Nullable List<BakedQuad> cache) {
        return ExtraModelManager.cancelTop(IExtraRendererContextOwner.of(blockAndTintGetter), bakedModel, blockAndTintGetter, state, pos, direction, random, seed, original, cache);
    }

    /*
     * Enabled aggressive block sorting
     */
    public static List<BakedQuad> cancelTop(@Nonnull ExtraRendererContext rendererHolder, @Nullable BakedModel bakedModel, @Nonnull BlockAndTintGetter blockAndTintGetter, @Nonnull BlockState state, @Nonnull BlockPos pos, @Nullable Direction direction, @Nonnull RandomSource random, long seed, @Nonnull List<BakedQuad> original, @Nullable List<BakedQuad> cache) {
        block40: {
            int blockType;
            if (rendererHolder.getExtraModel() == null) {
                return original;
            }
            if (bakedModel != null && ClientConfig.isTopFaceCulling() && !original.isEmpty() && (direction == Direction.UP || direction == null) && !IESReplaceModel.isInvalid(bakedModel) && blockAndTintGetter instanceof IMapSliceProvider) {
                random.setSeed(seed);
                BakedModel snowModel = rendererHolder.getExtraModel();
                if (snowModel instanceof SnowyBakedModelWrapper) {
                    blockType = MapChecker.getDefaultBlockTypeFlag(state);
                    if (MapChecker.customBuiltin(blockType)) {
                        return original;
                    }
                    if (blockType == 4) return original;
                    if (blockType == 7) {
                        return original;
                    }
                    if (direction == Direction.UP && blockType == 1) {
                        return EMPTY;
                    }
                    if (original.size() == 1) {
                        if (original.getFirst().getDirection() == Direction.UP) {
                            return EMPTY;
                        }
                    } else {
                        original = new ArrayList<BakedQuad>(original);
                        for (int i = 0; i < original.size(); ++i) {
                            BakedQuad bakedQuad2 = original.get(i);
                            if (bakedQuad2.getDirection() != Direction.UP) continue;
                            original.remove(i);
                            --i;
                        }
                    }
                }
            }
            if (!(bakedModel instanceof SnowyBakedModelWrapper)) break block40;
            SnowyBakedModelWrapper snowyBakedModelWrapper = (SnowyBakedModelWrapper)bakedModel;
            int n = blockType = snowyBakedModelWrapper.getBindBlockType() > -1 ? snowyBakedModelWrapper.getBindBlockType() : MapChecker.getDefaultBlockTypeFlag(state);
            if (MapChecker.customBuiltin(blockType)) {
                original = new ArrayList<BakedQuad>();
            }
            boolean yuushyaBlock = false;
            yuushyaBlock |= cache != null && !cache.isEmpty();
            boolean sodiumStairs = (blockType == 3 || blockType == 301) && CompatModule.isSodium() && state.getBlock() instanceof StairBlock;
            sodiumStairs = false;
            if (!MapChecker.customBuiltin(blockType) && !(yuushyaBlock &= blockType != 4 && blockType != 7) && !sodiumStairs) break block40;
            if (yuushyaBlock && blockType == 3 && state.getBlock() instanceof StairBlock && direction == Direction.UP && cache != null && !cache.isEmpty()) {
                cache = new ArrayList<BakedQuad>(cache);
                cache.removeIf(bakedQuad -> bakedQuad.getDirection() != Direction.UP);
            }
            if (!(MapChecker.customBuiltin(blockType) || sodiumStairs || yuushyaBlock && (blockType == 3 || blockType == 301 && direction != Direction.UP || blockType == 2 || blockType == 1 && direction != Direction.UP)) && (direction == null || direction.ordinal() <= 1)) break block40;
            ArrayList<Object> quadsCTM = null;
            if (cache == null || cache.isEmpty()) {
                BakedModel bakedModelCTM = rendererHolder.getOriginalModel();
                if (bakedModelCTM != null) {
                    ModelData modelDataCTM = rendererHolder.getModelData();
                    random.setSeed(seed);
                    ChunkRenderTypeSet renderTypes = bakedModelCTM.getRenderTypes(state, random, modelDataCTM);
                    quadsCTM = new ArrayList();
                    for (RenderType renderType : renderTypes.asList()) {
                        random.setSeed(seed);
                        quadsCTM.addAll(bakedModelCTM.getQuads(state, direction, random, modelDataCTM, renderType));
                    }
                }
            } else {
                quadsCTM = direction != null ? new ArrayList<BakedQuad>(EMPTY) : new ArrayList<BakedQuad>(cache);
            }
            if (quadsCTM == null) break block40;
            boolean tooTiny = false;
            tooTiny |= state.getBlock() instanceof FenceBlock;
            tooTiny |= state.getBlock() instanceof FenceGateBlock;
            tooTiny |= state.getBlock() instanceof IronBarsBlock;
            if (!(tooTiny |= state.getBlock() instanceof StairBlock)) {
                quadsCTM = QuadFixer.fixQuadCTM(quadsCTM);
            }
            TextureAtlasSprite snow_overlay_sprite = ExtraModelManager.getSprite(snow_overlay);
            TextureAtlasSprite snow_overlay_tiny_sprite = ExtraModelManager.getSprite(snow_overlay_tiny);
            TextureAtlasSprite snow_sprite = ExtraModelManager.getSprite(snow);
            float offset = 0.5f;
            boolean isSlabDown = blockType == 2;
            original = new ArrayList<BakedQuad>(quadsCTM.size());
            for (BakedQuad bakedQuad3 : quadsCTM) {
                TextureAtlasSprite spriteUse;
                block42: {
                    block43: {
                        Direction stairFaceDirection;
                        StairsShape stairsShape;
                        Direction bakedQuadDirection;
                        block51: {
                            block49: {
                                block50: {
                                    block47: {
                                        block48: {
                                            block45: {
                                                block46: {
                                                    block44: {
                                                        Comparable comparable;
                                                        block41: {
                                                            bakedQuadDirection = bakedQuad3.getDirection();
                                                            if (bakedQuadDirection == Direction.DOWN) continue;
                                                            spriteUse = snow_overlay_sprite;
                                                            if (bakedQuadDirection != Direction.UP) {
                                                                isSlabDown = true;
                                                                float maxY = QuadFixer.getMaxY(bakedQuad3);
                                                                offset = 1.0f - maxY;
                                                                if (offset < 1.0E-5f) {
                                                                    offset = 0.0f;
                                                                    isSlabDown = false;
                                                                }
                                                            }
                                                            if (bakedQuadDirection != Direction.UP) break block41;
                                                            spriteUse = snow_sprite;
                                                            break block42;
                                                        }
                                                        if (!(state.getBlock() instanceof StairBlock) || !((comparable = state.getValue((Property)StairBlock.SHAPE)) instanceof StairsShape)) break block43;
                                                        stairsShape = (StairsShape)comparable;
                                                        comparable = state.getValue((Property)StairBlock.HALF);
                                                        if (!(comparable instanceof Half)) break block43;
                                                        Half half = (Half)comparable;
                                                        comparable = state.getValue((Property)StairBlock.FACING);
                                                        if (!(comparable instanceof Direction)) break block43;
                                                        stairFaceDirection = (Direction)comparable;
                                                        TextureAtlasSprite textureAtlasSprite = spriteUse = QuadFixer.getMaxY(bakedQuad3) - QuadFixer.getMinY(bakedQuad3) > 0.4002f ? snow_overlay_sprite : snow_overlay_tiny_sprite;
                                                        if (half != Half.TOP) break block44;
                                                        if (QuadFixer.getMaxY(bakedQuad3) < 0.500001f) {
                                                            continue;
                                                        }
                                                        break block42;
                                                    }
                                                    if (stairsShape != StairsShape.STRAIGHT) break block45;
                                                    if (bakedQuadDirection != stairFaceDirection) break block46;
                                                    if (QuadFixer.getMaxY(bakedQuad3) < 0.500001f) {
                                                        continue;
                                                    }
                                                    break block42;
                                                }
                                                if (!(QuadFixer.getMaxY(bakedQuad3) < 0.500001f)) break block42;
                                                if (bakedQuadDirection == stairFaceDirection.getClockWise()) {
                                                    spriteUse = ExtraModelManager.getSprite(snow_overlay_half_left);
                                                    break block42;
                                                } else if (bakedQuadDirection == stairFaceDirection.getCounterClockWise()) {
                                                    spriteUse = snow_overlay_sprite;
                                                }
                                                break block42;
                                            }
                                            if (stairsShape != StairsShape.INNER_LEFT) break block47;
                                            if (bakedQuadDirection != stairFaceDirection && bakedQuadDirection != stairFaceDirection.getCounterClockWise()) break block48;
                                            if (QuadFixer.getMaxY(bakedQuad3) < 0.500001f) {
                                                continue;
                                            }
                                            break block42;
                                        }
                                        if (!(QuadFixer.getMaxY(bakedQuad3) < 0.500001f)) break block42;
                                        if (bakedQuadDirection == stairFaceDirection.getClockWise()) {
                                            spriteUse = ExtraModelManager.getSprite(snow_overlay_half_left);
                                            break block42;
                                        } else if (bakedQuadDirection == stairFaceDirection.getOpposite()) {
                                            spriteUse = ExtraModelManager.getSprite(snow_overlay_half_right);
                                        }
                                        break block42;
                                    }
                                    if (stairsShape != StairsShape.INNER_RIGHT) break block49;
                                    if (bakedQuadDirection != stairFaceDirection && bakedQuadDirection != stairFaceDirection.getClockWise()) break block50;
                                    if (QuadFixer.getMaxY(bakedQuad3) < 0.500001f) {
                                        continue;
                                    }
                                    break block42;
                                }
                                if (!(QuadFixer.getMaxY(bakedQuad3) < 0.500001f)) break block42;
                                if (bakedQuadDirection == stairFaceDirection.getCounterClockWise()) {
                                    spriteUse = ExtraModelManager.getSprite(snow_overlay_half_right);
                                    break block42;
                                } else if (bakedQuadDirection == stairFaceDirection.getOpposite()) {
                                    spriteUse = ExtraModelManager.getSprite(snow_overlay_half_left);
                                }
                                break block42;
                            }
                            if (stairsShape != StairsShape.OUTER_LEFT) break block51;
                            if (QuadFixer.getMaxY(bakedQuad3) < 0.500001f) {
                                if (bakedQuadDirection == stairFaceDirection.getCounterClockWise()) {
                                    spriteUse = ExtraModelManager.getSprite(snow_overlay_half_right);
                                    break block42;
                                } else if (bakedQuadDirection == stairFaceDirection) {
                                    spriteUse = ExtraModelManager.getSprite(snow_overlay_half_left);
                                }
                            }
                            break block42;
                        }
                        if (stairsShape != StairsShape.OUTER_RIGHT || !(QuadFixer.getMaxY(bakedQuad3) < 0.500001f)) break block42;
                        if (bakedQuadDirection == stairFaceDirection.getClockWise()) {
                            spriteUse = ExtraModelManager.getSprite(snow_overlay_half_left);
                            break block42;
                        } else if (bakedQuadDirection == stairFaceDirection) {
                            spriteUse = ExtraModelManager.getSprite(snow_overlay_half_right);
                        }
                        break block42;
                    }
                    spriteUse = tooTiny ? snow_overlay_tiny_sprite : (QuadFixer.getMaxY(bakedQuad3) - QuadFixer.getMinY(bakedQuad3) > 0.4002f ? snow_overlay_sprite : snow_overlay_tiny_sprite);
                }
                BakedQuadRetexturedAndReUV retexturedBakedQuad = new BakedQuadRetexturedAndReUV(bakedQuad3, spriteUse, isSlabDown, offset);
                original.add(retexturedBakedQuad);
            }
        }
        if (original.isEmpty()) return original;
        if (direction == null) return original;
        if (MapChecker.getDefaultBlockTypeFlag(state) != 4) return original;
        int index = Math.abs((int)(seed + (long)pos.getX()) % 8);
        if (index <= 0) return original;
        if (!blockAndTintGetter.getBlockState(pos.relative(direction)).is(state.getBlock())) return original;
        return new ArrayList<BakedQuad>();
    }

    public static BlockPos.MutableBlockPos posToMutable(BlockPos pos) {
        return new BlockPos.MutableBlockPos(pos.getX(), pos.getY(), pos.getZ());
    }

    public static boolean canSnowy(BlockAndTintGetter blockAndTintGetter, BlockPos pos, BlockState state, long seed, @Nullable BlockPos.MutableBlockPos checkPos) {
        IMapSliceProvider ip;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        int flag = MapChecker.getDefaultBlockTypeFlag(state);
        Block onBlock = state.getBlock();
        List<SnowDefinition> snowDefClientOverlay = ClientRef.snowClientDef.get(onBlock);
        if (flag == 0 && (snowDefClientOverlay == null || snowDefClientOverlay.isEmpty() || snowDefClientOverlay.get(0).getInfo().getFlag() == 0)) {
            return false;
        }
        boolean leaveLike = MapChecker.leaveLike(flag);
        boolean leavesOrVine = leaveLike || MapChecker.vineLike(flag);
        IMapSlice mapSlice = null;
        if (blockAndTintGetter instanceof IMapSlice) {
            int cut;
            IMapSlice cmapSlice;
            mapSlice = cmapSlice = (IMapSlice)blockAndTintGetter;
            if (!leavesOrVine && (cut = mapSlice.getBlockHeight(pos) - pos.getY()) > 1 && !((Boolean)ClientConfig.Renderer.snowUnderFence.get()).booleanValue()) {
                return false;
            }
        }
        int offset = snowDefClientOverlay == null ? MapChecker.getSnowOffset(state, flag) : snowDefClientOverlay.get(0).getInfo().getOffset();
        boolean isLight = false;
        if (checkPos == null) {
            checkPos = ExtraModelManager.posToMutable(pos);
        } else {
            checkPos.set(pos.getX(), pos.getY(), pos.getZ());
        }
        if (((Boolean)ClientConfig.Renderer.useVanillaCheck.get()).booleanValue()) {
            checkPos.setY(pos.getY() + 1);
            isLight = blockAndTintGetter.getBrightness(LightLayer.BLOCK, (BlockPos)checkPos) >= 15;
        } else {
            int cacheHeight = mapSlice != null ? mapSlice.getBlockHeight((BlockPos)checkPos) : MapChecker.getHeightOrUpdate((Level)level, (BlockPos)checkPos, false);
            isLight = cacheHeight <= pos.getY() - offset;
        }
        boolean specialLeaves = false;
        if (!isLight && ((Boolean)ClientConfig.Renderer.snowUnderFence.get()).booleanValue()) {
            checkPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
            if (blockAndTintGetter.getBrightness(LightLayer.SKY, (BlockPos)checkPos) >= 9) {
                int n;
                if (blockAndTintGetter instanceof IMapSliceProvider) {
                    ip = (IMapSliceProvider)blockAndTintGetter;
                    n = ip.getSolidBlockHeight((BlockPos)checkPos);
                } else {
                    n = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - 1;
                }
                int y_real = n;
                checkPos.setY(y_real);
                BlockState getterBlockState = blockAndTintGetter.getBlockState((BlockPos)checkPos);
                if (getterBlockState.isEmpty()) {
                    try {
                        getterBlockState = level.getBlockState((BlockPos)checkPos);
                    }
                    catch (Exception e) {
                        EclipticSeasons.logger(e);
                    }
                }
                if (getterBlockState.getShadeBrightness((BlockGetter)blockAndTintGetter, (BlockPos)checkPos) < 0.5f) {
                    isLight = true;
                    if (leaveLike) {
                        if (((Boolean)CommonConfig.Snow.snowyTree.get()).booleanValue()) {
                            specialLeaves = true;
                        } else {
                            isLight = false;
                        }
                    }
                }
            }
        }
        if (isLight) {
            checkPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
            if (MapChecker.leaveLike(flag)) {
                if (!specialLeaves) {
                    BlockState aboveState = blockAndTintGetter.getBlockState((BlockPos)checkPos);
                    if (isLight) {
                        specialLeaves = blockAndTintGetter instanceof IMapSliceProvider ? (ip = (IMapSliceProvider)blockAndTintGetter).getSolidBlockHeight((BlockPos)checkPos) > pos.getY() : aboveState.is(state.getBlock());
                    }
                }
            } else if (MapChecker.extraSnowPassable(state)) {
                isLight = !MapChecker.extraSnowPassable(blockAndTintGetter.getBlockState((BlockPos)checkPos));
            } else if (!((Boolean)ClientConfig.Renderer.snowUnderFence.get()).booleanValue()) {
                boolean bl = isLight = !MapChecker.solidTest(blockAndTintGetter.getBlockState((BlockPos)checkPos));
            }
        }
        if (isLight && specialLeaves && !(isLight = ((Boolean)CommonConfig.Snow.snowyTree.get()).booleanValue())) {
            specialLeaves = false;
        }
        boolean isSnowy = false;
        if (isLight) {
            checkPos.setY(pos.getY());
            if (CommonConfig.isSnowyWinter() && onBlock != Blocks.SNOW_BLOCK && ExtraModelManager.maySnowyAt((Level)level, mapSlice, state, (BlockPos)checkPos, null, seed)) {
                isSnowy = true;
                checkPos.set(pos.getX(), pos.getY() + 1 - offset, pos.getZ());
                isSnowy = ExtraModelManager.notTooBright(blockAndTintGetter, mapSlice, (BlockPos)checkPos);
            }
        }
        return isSnowy;
    }

    public static boolean maySnowyAt(Level level, IMapSlice mapSlice, BlockState state, BlockPos checkPos, RandomSource random, long seed) {
        if (mapSlice != null) {
            int snowyStatus = mapSlice.getSnowyStatus(checkPos);
            if (snowyStatus == SnowyRemover.SnowyFlag.SNOWY_ALWAYS.ordinal()) {
                return true;
            }
            if (snowyStatus == SnowyRemover.SnowyFlag.NONE_SNOWY.ordinal()) {
                return false;
            }
            if (EclipticUtil.canSnowyBlockInteract() && MapChecker.notWater(state)) {
                return mapSlice.isSnowyBlock(checkPos);
            }
            return MapChecker.shouldSnowAt(level, checkPos, mapSlice.getSurfaceFaceBiomeId(checkPos), state, random, seed);
        }
        return MapChecker.shouldSnowAt(level, checkPos, state, random, seed);
    }

    public static boolean notTooBright(BlockAndTintGetter blockAndTintGetter, IMapSlice mapSlice, BlockPos checkPos) {
        boolean isSnowy = true;
        if (((Boolean)CommonConfig.Snow.notSnowyNearGlowingBlock.get()).booleanValue() && !EclipticUtil.canSnowyBlockInteract()) {
            if (mapSlice != null && mapSlice.getSnowyStatus(checkPos) == 0 && blockAndTintGetter.getBrightness(LightLayer.BLOCK, checkPos) >= CommonConfig.Snow.notSnowyNearGlowingBlockLevel.getAsInt()) {
                isSnowy = false;
            }
            if (mapSlice == null && blockAndTintGetter.getBrightness(LightLayer.BLOCK, checkPos) >= CommonConfig.Snow.notSnowyNearGlowingBlockLevel.getAsInt()) {
                isSnowy = false;
            }
        }
        return isSnowy;
    }

    public static BakedModel findModel(BlockAndTintGetter blockAndTintGetter, BlockPos pos, BlockState state, RandomSource random, long seed, @Nullable BlockPos.MutableBlockPos checkPos) {
        IESReplaceModel iesReplaceModel;
        IMapSliceProvider ip;
        boolean leaveLike;
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return null;
        }
        int flag = MapChecker.getDefaultBlockTypeFlag(state);
        List<SeasonBlockDefinition> seasonDefCache = null;
        List<SnowDefinition> snowDefClientOverlay = null;
        Block onBlock = state.getBlock();
        if (flag == 0) {
            seasonDefCache = ClientRef.seasonDef.get(onBlock);
            snowDefClientOverlay = ClientRef.snowClientDef.get(onBlock);
            if (snowDefClientOverlay != null && snowDefClientOverlay.isEmpty()) {
                snowDefClientOverlay = null;
            }
            if (seasonDefCache == null && snowDefClientOverlay == null) {
                return null;
            }
        }
        boolean extendCheck = false;
        if (checkPos == null) {
            checkPos = ExtraModelManager.posToMutable(pos);
        } else {
            checkPos.set(pos.getX(), pos.getY(), pos.getZ());
        }
        checkPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
        if (((Boolean)ClientConfig.Debug.smoothSnowyEdges.get()).booleanValue() && blockAndTintGetter.getBrightness(LightLayer.SKY, (BlockPos)checkPos) > 0) {
            extendCheck = true;
        }
        boolean leavesOrVine = (leaveLike = MapChecker.leaveLike(flag)) || MapChecker.vineLike(flag);
        Object replace = null;
        IMapSlice mapSlice = null;
        if (blockAndTintGetter instanceof IMapSlice) {
            int cut;
            IMapSlice cmapSlice;
            mapSlice = cmapSlice = (IMapSlice)blockAndTintGetter;
            if (!(leavesOrVine || (cut = mapSlice.getBlockHeight(pos) - pos.getY()) <= 1 || ((Boolean)ClientConfig.Renderer.snowUnderFence.get()).booleanValue() || extendCheck)) {
                return null;
            }
        }
        int offset = snowDefClientOverlay == null ? MapChecker.getSnowOffset(state, flag) : snowDefClientOverlay.get(0).getInfo().getOffset();
        boolean isLight = false;
        if (checkPos == null) {
            checkPos = ExtraModelManager.posToMutable(pos);
        } else {
            checkPos.set(pos.getX(), pos.getY(), pos.getZ());
        }
        if (((Boolean)ClientConfig.Renderer.useVanillaCheck.get()).booleanValue()) {
            checkPos.setY(pos.getY() + 1);
            isLight = blockAndTintGetter.getBrightness(LightLayer.BLOCK, (BlockPos)checkPos) >= 15;
        } else {
            int cacheHeight = mapSlice != null ? mapSlice.getBlockHeight((BlockPos)checkPos) : MapChecker.getHeightOrUpdate((Level)level, (BlockPos)checkPos, false);
            isLight = cacheHeight <= pos.getY() - offset;
        }
        boolean specialLeaves = false;
        if (!isLight && ((Boolean)ClientConfig.Renderer.snowUnderFence.get()).booleanValue()) {
            checkPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
            if (blockAndTintGetter.getBrightness(LightLayer.SKY, (BlockPos)checkPos) >= 9) {
                int n;
                if (blockAndTintGetter instanceof IMapSliceProvider) {
                    ip = (IMapSliceProvider)blockAndTintGetter;
                    n = ip.getSolidBlockHeight((BlockPos)checkPos);
                } else {
                    n = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - 1;
                }
                int y_real = n;
                checkPos.setY(y_real);
                BlockState getterBlockState = blockAndTintGetter.getBlockState((BlockPos)checkPos);
                if (getterBlockState.isEmpty()) {
                    try {
                        getterBlockState = level.getBlockState((BlockPos)checkPos);
                    }
                    catch (Exception e) {
                        EclipticSeasons.logger(e);
                    }
                }
                if (getterBlockState.getShadeBrightness((BlockGetter)blockAndTintGetter, (BlockPos)checkPos) < 0.5f) {
                    isLight = true;
                    if (leaveLike) {
                        if (((Boolean)CommonConfig.Snow.snowyTree.get()).booleanValue()) {
                            specialLeaves = true;
                        } else {
                            isLight = false;
                        }
                    }
                }
            }
        }
        if (isLight) {
            checkPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
            if (MapChecker.leaveLike(flag)) {
                if (!specialLeaves) {
                    BlockState aboveState = blockAndTintGetter.getBlockState((BlockPos)checkPos);
                    if (isLight) {
                        specialLeaves = blockAndTintGetter instanceof IMapSliceProvider ? (ip = (IMapSliceProvider)blockAndTintGetter).getSolidBlockHeight((BlockPos)checkPos) > pos.getY() : aboveState.is(state.getBlock());
                    }
                }
            } else if (MapChecker.extraSnowPassable(state)) {
                isLight = !MapChecker.extraSnowPassable(blockAndTintGetter.getBlockState((BlockPos)checkPos));
            } else if (!((Boolean)ClientConfig.Renderer.snowUnderFence.get()).booleanValue()) {
                boolean bl = isLight = !MapChecker.solidTest(blockAndTintGetter.getBlockState((BlockPos)checkPos));
            }
        }
        if (isLight && specialLeaves && !(isLight = ((Boolean)CommonConfig.Snow.snowyTree.get()).booleanValue())) {
            specialLeaves = false;
        }
        if (isLight || extendCheck) {
            boolean isSnowy = false;
            checkPos.setY(pos.getY());
            if (CommonConfig.isSnowyWinter() && isLight && onBlock != Blocks.SNOW_BLOCK && ExtraModelManager.maySnowyAt((Level)level, mapSlice, state, (BlockPos)checkPos, random, seed)) {
                isSnowy = true;
                checkPos.set(pos.getX(), pos.getY() + 1 - offset, pos.getZ());
                isSnowy = ExtraModelManager.notTooBright(blockAndTintGetter, mapSlice, (BlockPos)checkPos);
                if (isSnowy) {
                    BlockState snowState = null;
                    if (flag == 3) {
                        snowState = (BlockState)((BlockState)((BlockState)((Block)BlockRegistry.snowyStairs.get()).defaultBlockState().setValue((Property)StairBlock.FACING, (Comparable)((Direction)state.getValue((Property)StairBlock.FACING)))).setValue((Property)StairBlock.HALF, (Comparable)((Half)state.getValue((Property)StairBlock.HALF)))).setValue((Property)StairBlock.SHAPE, (Comparable)((StairsShape)state.getValue((Property)StairBlock.SHAPE)));
                    } else if (flag == 7) {
                        snowState = (BlockState)((BlockState)((BlockState)((BlockState)((BlockState)((Block)BlockRegistry.snowyVine.get()).defaultBlockState().setValue((Property)VineBlock.EAST, (Comparable)((Boolean)state.getValue((Property)VineBlock.EAST)))).setValue((Property)VineBlock.WEST, (Comparable)((Boolean)state.getValue((Property)VineBlock.WEST)))).setValue((Property)VineBlock.SOUTH, (Comparable)((Boolean)state.getValue((Property)VineBlock.SOUTH)))).setValue((Property)VineBlock.NORTH, (Comparable)((Boolean)state.getValue((Property)VineBlock.NORTH)))).setValue((Property)VineBlock.UP, (Comparable)((Boolean)state.getValue((Property)VineBlock.UP)));
                    } else if (leaveLike && !specialLeaves) {
                        snowState = ((Block)BlockRegistry.snowyLeaves.get()).defaultBlockState();
                    }
                    BakedModel snowModel = ExtraModelManager.getSnowyModel(state, snowState, flag, offset);
                    if (snowModel != null) {
                        replace = snowModel;
                    }
                }
            }
            if (!isSnowy && (flag == 1 || onBlock == Blocks.GRASS_BLOCK) && ((Boolean)ClientConfig.Debug.smoothSnowyEdges.get()).booleanValue()) {
                int index = -1;
                int ddLength = 0;
                int[][][] directions = DirectionMask.DIRECTIONS;
                int[] indexs = DirectionMask.INDICES;
                BlockPos.MutableBlockPos originalCache = null;
                int directionsLength = directions.length;
                block2: for (int i = 0; i < directionsLength; ++i) {
                    int[][] directionRequireGroup;
                    for (int[] direction : directionRequireGroup = directions[i]) {
                        checkPos.set(pos.getX() + direction[0], pos.getY() + 1, pos.getZ() + direction[1]);
                        if ((mapSlice != null ? mapSlice.getBlockHeight((BlockPos)checkPos) : MapChecker.getHeightOrUpdate((Level)level, (BlockPos)checkPos, false)) != pos.getY()) continue block2;
                        checkPos.set(pos.getX() + direction[0], pos.getY(), pos.getZ() + direction[1]);
                        BlockState neighSate = blockAndTintGetter.getBlockState((BlockPos)checkPos);
                        long neighSateSeed = neighSate.getSeed((BlockPos)checkPos);
                        if (!neighSate.is(Blocks.GRASS_BLOCK) && MapChecker.getDefaultBlockTypeFlag(neighSate) != 1) continue block2;
                        if (originalCache == null) {
                            originalCache = new BlockPos.MutableBlockPos(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                        } else {
                            originalCache.set(checkPos.getX(), checkPos.getY(), checkPos.getZ());
                        }
                        if (!ExtraModelManager.canSnowy(blockAndTintGetter, (BlockPos)originalCache, neighSate, neighSateSeed, checkPos)) continue block2;
                    }
                    if (directionRequireGroup.length <= ddLength) continue;
                    index = i;
                    ddLength = directionRequireGroup.length;
                }
                if (index > -1) {
                    index = indexs[index];
                    replace = models.get(snow_edge_overlays.get(index));
                    isSnowy = true;
                }
            }
        }
        if (replace == null || !(replace instanceof IESReplaceModel) || !(iesReplaceModel = (IESReplaceModel)replace).isReplace()) {
            if (seasonDefCache == null) {
                seasonDefCache = ClientRef.seasonDef.get(onBlock);
            }
            if (seasonDefCache != null) {
                for (SeasonBlockDefinition localSeasonStatus : seasonDefCache) {
                    List<SeasonBlockDefinition.FlatSliceHolder> flatSliceHolders = localSeasonStatus.getFlatSliceEnumMap().get(ClientCon.nowSolarTerm);
                    if (flatSliceHolders == null || flatSliceHolders.isEmpty()) continue;
                    checkPos.set(pos.getX(), pos.getY() + 1, pos.getZ());
                    for (SeasonBlockDefinition.FlatSliceHolder flatSliceHolder : flatSliceHolders) {
                        BakedModel to_replace;
                        ModelTester mmrl;
                        ResourceLocation cinfo;
                        ModelResolver smr;
                        SeasonBlockDefinition.FlatSlice flatSlice = flatSliceHolder.flatSlice();
                        if (flatSlice.emptyAbove() && !blockAndTintGetter.getBlockState((BlockPos)checkPos).isEmpty() || (mapSlice != null || !localSeasonStatus.getBiomes().contains(MapChecker.getSurfaceBiome((Level)level, (BlockPos)checkPos))) && (mapSlice == null || !localSeasonStatus.getBiomes().contains(MapChecker.idToBiome((Level)level, mapSlice.getSurfaceFaceBiomeId((BlockPos)checkPos)))) || (smr = extraSnowModelBuilds.get(cinfo = flatSlice.transitionModels() == null ? flatSlice.mid() : (Mth.abs((int)((int)(seed + (long)pos.getX()))) % 100 > ClientCon.progress ? (ResourceLocation)flatSlice.transitionModels().getFirst() : (ResourceLocation)flatSlice.transitionModels().getSecond()))) == null || (mmrl = smr.tryFind(state)) == null || (to_replace = models.get(mmrl.modelResourceLocation())) == null) continue;
                        if (replace != null) {
                            replace = new SnowySeasonBakeModel<BakedModel>(to_replace, (BakedModel)replace, ExtraModelManager.getRenderType(state));
                            if (!mmrl.replace() || !(replace instanceof SnowySeasonBakeModel)) continue;
                            SnowySeasonBakeModel snowyBakedModelWrapper = (SnowySeasonBakeModel)replace;
                            snowyBakedModelWrapper.setReplace(true);
                            continue;
                        }
                        replace = mmrl.replace() ? new TempReplaceModelWrapper<BakedModel>(to_replace) : to_replace;
                    }
                }
            }
        }
        return replace;
    }

    public static BakedModel shouldRenderedWithSnowInside(BlockAndTintGetter blockAndTintGetter, BlockPos pos, BlockState state, @Nullable BlockPos.MutableBlockPos checkPos) {
        int layers = ExtraModelManager.getRenderedLevelWithSnowInside(blockAndTintGetter, pos, state, checkPos);
        return layers > 0 ? ExtraModelManager.getSnowLayerModel(layers) : null;
    }

    public static int getRenderedLevelWithSnowInside(BlockAndTintGetter blockAndTintGetter, BlockPos pos, BlockState state, @Nullable BlockPos.MutableBlockPos checkPos) {
        BlockPos[] directions;
        IMapSlice mapSlice;
        Level useLevel;
        block19: {
            block18: {
                if (!((Boolean)ClientConfig.Renderer.snowInFence.get()).booleanValue()) {
                    return 0;
                }
                useLevel = ClientCon.getUseLevel();
                if (!(blockAndTintGetter instanceof IMapSlice)) break block18;
                mapSlice = (IMapSlice)blockAndTintGetter;
                if (useLevel != null) break block19;
            }
            return 0;
        }
        mapSlice.setLevelForFakeSnow(pos, 0);
        if (blockAndTintGetter.getBrightness(LightLayer.SKY, pos) == 0) {
            return 0;
        }
        if (checkPos == null) {
            checkPos = ExtraModelManager.posToMutable(pos);
        } else {
            checkPos.set(pos.getX(), pos.getY(), pos.getZ());
        }
        if (state.isAir() || !state.getFluidState().isEmpty() || state.is(EclipticBlockTags.SNOW_LAYER_CANNOT_SURVIVE_IN)) {
            return 0;
        }
        if (((Boolean)ClientConfig.Renderer.snowInFenceOnlySnowy.get()).booleanValue() && !ExtraModelManager.maySnowyAt(useLevel, mapSlice, state, pos, useLevel.getRandom(), state.getSeed(pos))) {
            return 0;
        }
        BlockState belowState = blockAndTintGetter.getBlockState((BlockPos)checkPos.setY(pos.getY() - 1));
        if (belowState.is(BlockTags.SNOW_LAYER_CANNOT_SURVIVE_ON)) {
            return 0;
        }
        if (!(belowState.is(BlockTags.SNOW_LAYER_CAN_SURVIVE_ON) || Block.isFaceFull((VoxelShape)belowState.getCollisionShape((BlockGetter)blockAndTintGetter, (BlockPos)checkPos), (Direction)Direction.UP) || belowState.is(Blocks.SNOW) && (Integer)belowState.getValue((Property)SnowLayerBlock.LAYERS) == 8)) {
            return 0;
        }
        int snowNearbyCount = 0;
        int airNeighborCount = 0;
        int minLayers = 8;
        for (BlockPos dir : directions = (Boolean)ClientConfig.Renderer.snowInFenceDirection.get() != false ? SNOW_LAYER_DIRECTIONS_TO_CHECK_8 : SNOW_LAYER_DIRECTIONS_TO_CHECK_4) {
            checkPos.set(pos.getX() + dir.getX(), pos.getY() + dir.getY(), pos.getZ() + dir.getZ());
            BlockState neighborState = blockAndTintGetter.getBlockState((BlockPos)checkPos);
            if (neighborState.isAir()) {
                checkPos.move(Direction.DOWN);
                if (blockAndTintGetter.getBlockState((BlockPos)checkPos).blocksMotion()) continue;
                ++airNeighborCount;
                continue;
            }
            int currentNeighborLayers = 0;
            if (neighborState.getBlock() == Blocks.SNOW) {
                currentNeighborLayers = (Integer)neighborState.getValue((Property)SnowLayerBlock.LAYERS);
            } else if (neighborState.getBlock() == Blocks.SNOW_BLOCK) {
                currentNeighborLayers = 8;
            }
            if (currentNeighborLayers <= 0) continue;
            ++snowNearbyCount;
            if (currentNeighborLayers >= minLayers) continue;
            minLayers = currentNeighborLayers;
        }
        int baseRequired = (Integer)ClientConfig.Renderer.snowInFenceCount.get();
        int dynamicRequired = Math.max(1, baseRequired - airNeighborCount);
        if (snowNearbyCount >= dynamicRequired) {
            if (state.isCollisionShapeFullBlock((BlockGetter)blockAndTintGetter, pos) || state.isFaceSturdy((BlockGetter)blockAndTintGetter, pos, Direction.DOWN)) {
                return 0;
            }
            mapSlice.setLevelForFakeSnow(pos, minLayers);
            return minLayers;
        }
        return 0;
    }

    public static BakedModel getExtraModel(ModelResourceLocation key) {
        return models.get(key);
    }

    public static BakedModel getSnowLayerModel(int layers) {
        int clampedLayers = Mth.clamp((int)layers, (int)1, (int)8);
        BlockState snowState = (BlockState)Blocks.SNOW.defaultBlockState().setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(clampedLayers));
        return Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(snowState);
    }

    public static BlockState shouldBlockAsSnowyState(BlockState state, BlockAndTintGetter blockAndTintGetter, BlockPos.MutableBlockPos mutableBlockPos) {
        if (!((Boolean)ClientConfig.Renderer.snowInFence.get()).booleanValue()) {
            return state;
        }
        if (!(state.getBlock() instanceof SnowyDirtBlock)) {
            return state;
        }
        int y = mutableBlockPos.getY();
        mutableBlockPos.setY(y + 1);
        BlockState blockState = blockAndTintGetter.getBlockState((BlockPos)mutableBlockPos);
        BakedModel bm = ExtraModelManager.shouldRenderedWithSnowInside(blockAndTintGetter, (BlockPos)mutableBlockPos, blockState, null);
        if (bm != null && state.hasProperty((Property)BlockStateProperties.SNOWY)) {
            state = (BlockState)state.setValue((Property)BlockStateProperties.SNOWY, (Comparable)Boolean.valueOf(true));
        }
        mutableBlockPos.setY(y);
        return state;
    }

    public static int getLayer(BlockAndTintGetter blockAndTintGetter, BlockPos.MutableBlockPos pos, BlockState state, BakedModel snowModel, long seed) {
        float targetLayers;
        IMapSlice mapSlice;
        block19: {
            block18: {
                if (!(blockAndTintGetter instanceof IMapSlice)) break block18;
                mapSlice = (IMapSlice)blockAndTintGetter;
                if (((Boolean)ClientConfig.Renderer.extraSnowLayer.get()).booleanValue()) break block19;
            }
            return 0;
        }
        if (mapSlice.getBlockHeight((BlockPos)pos) != pos.getY() && mapSlice.getSolidBlockHeight((BlockPos)pos) != pos.getY()) {
            return 0;
        }
        int realY = pos.getY() + 1;
        mapSlice.setLevelForFakeSnow(pos.getX(), realY, pos.getZ(), 0);
        if (MapChecker.getDefaultBlockTypeFlag(state) <= 0) {
            return 0;
        }
        if (mapSlice.getSnowyStatus((BlockPos)pos) == 1) {
            return 0;
        }
        Level useLevel = ClientCon.getUseLevel();
        if (useLevel == null) {
            return 0;
        }
        if (!state.isSolidRender((BlockGetter)blockAndTintGetter, (BlockPos)pos) && !(state.getBlock() instanceof LeavesBlock)) {
            return 0;
        }
        if (!state.getFluidState().isEmpty()) {
            return 0;
        }
        BlockPos.MutableBlockPos abovePos = pos.setY(pos.getY() + 1);
        BlockState aboveState = blockAndTintGetter.getBlockState((BlockPos)abovePos);
        if (!aboveState.getFluidState().isEmpty()) {
            return 0;
        }
        if (aboveState.getBlock() instanceof LeavesBlock || aboveState.isFaceSturdy((BlockGetter)blockAndTintGetter, (BlockPos)abovePos, Direction.DOWN) || aboveState.is(EclipticBlockTags.SNOW_LAYER_CANNOT_SURVIVE_IN)) {
            return 0;
        }
        if ((snowModel == null || ISnowyReplaceModel.isInvalid(snowModel)) && !ExtraModelManager.maySnowyAt(useLevel, mapSlice, state, (BlockPos)pos, null, seed)) {
            return 0;
        }
        if (!ExtraModelManager.notTooBright(blockAndTintGetter, mapSlice, (BlockPos)pos)) {
            return 0;
        }
        Holder<Biome> biome = MapChecker.idToBiome(useLevel, mapSlice.getSurfaceFaceBiomeId((BlockPos)pos));
        if (biome == null) {
            return 0;
        }
        int snowDepth = Mth.clamp((int)WeatherManager.getSnowDepthAtBiome(useLevel, (Biome)biome.value()), (int)0, (int)100);
        if (snowDepth <= 0) {
            return 0;
        }
        long posLong = pos.asLong();
        long h = (posLong ^ seed) * 25214903917L + 11L;
        h = (h ^ h >>> 16) * 668265261L;
        h ^= h >>> 15;
        int noiseInt = (int)(h & Integer.MAX_VALUE) % 100;
        int maxLayers = state.getBlock() instanceof LeavesBlock ? (Integer)ClientConfig.Renderer.extraSnowLayerMaxLayersOnLeaves.get() : (Integer)ClientConfig.Renderer.extraSnowLayerMaxLayers.get();
        if (snowDepth <= 50) {
            targetLayers = (float)snowDepth / 50.0f * 0.05f;
        } else {
            float progress = (float)(snowDepth - 50) / 50.0f;
            targetLayers = 0.05f + progress * progress * ((float)maxLayers - 0.05f);
        }
        int base = (int)Math.floor(targetLayers);
        int chance = (int)((targetLayers - (float)base) * 100.0f);
        if (noiseInt < chance) {
            ++base;
        }
        int minLayers = Mth.clamp((int)base, (int)0, (int)maxLayers);
        mapSlice.setLevelForFakeSnow(pos.getX(), realY, pos.getZ(), minLayers);
        return minLayers;
    }

    public static BlockState getFakeBlockState(BlockState original, BlockState selfState, BlockGetter view, BlockPos.MutableBlockPos otherPos, BlockPos selfPos, Direction facing) {
        boolean snowSelf;
        IMapSlice mapSlice;
        BlockAndTintGetter getter;
        block12: {
            block11: {
                if (facing == Direction.DOWN || !(view instanceof BlockAndTintGetter)) break block11;
                getter = (BlockAndTintGetter)view;
                if (!(view instanceof IMapSlice)) break block11;
                mapSlice = (IMapSlice)view;
                if (((Boolean)ClientConfig.Renderer.extraSnowLayerCulling.get()).booleanValue() && mapSlice.getBlockHeight(selfPos) <= selfPos.getY()) break block12;
            }
            return original;
        }
        boolean snowInFence = (Boolean)ClientConfig.Renderer.snowInFence.get();
        boolean extraSnowLayer = (Boolean)ClientConfig.Renderer.extraSnowLayer.get();
        if (!snowInFence && !extraSnowLayer) {
            return original;
        }
        boolean notUp = facing != Direction.UP;
        boolean bl = snowSelf = !selfState.is(Blocks.SNOW);
        if (snowSelf || notUp) {
            if (snowInFence && original.getRenderShape() == RenderShape.MODEL) {
                return original;
            }
            int cacheLevel = mapSlice.getLevelForFakeSnow((BlockPos)otherPos);
            if (cacheLevel > -1) {
                return cacheLevel == 0 ? original : (BlockState)Blocks.SNOW.defaultBlockState().setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(cacheLevel));
            }
            int y = otherPos.getY();
            if (snowInFence) {
                int x = otherPos.getX();
                int z = otherPos.getZ();
                int snowInsideLevel = ExtraModelManager.getRenderedLevelWithSnowInside(getter, (BlockPos)otherPos, original, null);
                otherPos.set(x, y, z);
                if (snowInsideLevel > 0) {
                    return (BlockState)Blocks.SNOW.defaultBlockState().setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(snowInsideLevel));
                }
                otherPos.set(x, y, z);
            }
            if (extraSnowLayer) {
                otherPos.setY(y - 1);
                BlockState belowState = !notUp ? selfState : view.getBlockState((BlockPos)otherPos);
                int layer = ExtraModelManager.getLayer(getter, otherPos, belowState, null, belowState.getSeed((BlockPos)otherPos));
                otherPos.setY(y);
                if (layer > 0) {
                    return (BlockState)Blocks.SNOW.defaultBlockState().setValue((Property)SnowLayerBlock.LAYERS, (Comparable)Integer.valueOf(layer));
                }
            }
        }
        return original;
    }

    public static RenderType getRenderType(BlockState state) {
        ChunkRenderTypeSet chunkRenderTypeSet = ItemBlockRenderTypes.getRenderLayers((BlockState)state);
        if (chunkRenderTypeSet.contains(RenderType.translucent())) {
            return RenderType.translucent();
        }
        if (chunkRenderTypeSet.contains(RenderType.cutout())) {
            return RenderType.cutout();
        }
        return RenderType.cutoutMipped();
    }

    @Deprecated(forRemoval=true)
    public static boolean isModelReplaceable(BlockState state, BlockAndTintGetter blockAndTintGetter, BlockPos pos) {
        return ExtraModelManager.isModelReplaceable(state, blockAndTintGetter, pos, null);
    }

    public static boolean isModelReplaceable(BlockState state, BlockAndTintGetter blockAndTintGetter, BlockPos pos, BakedModel bakedModel) {
        IESReplaceModel model;
        return bakedModel instanceof IESReplaceModel && (model = (IESReplaceModel)bakedModel).isReplace() || ExtraModelManager.isModelReplaceable(MapChecker.getDefaultBlockTypeFlag(state));
    }

    public static boolean isModelReplaceable(int flag) {
        return flag == 5 || flag == 501;
    }

    public static boolean renderAsSnowInShader(BlockState state, BlockGetter blockAndTintGetter, BlockPos pos) {
        int blockType = MapChecker.getDefaultBlockTypeFlag(state);
        return switch (blockType) {
            case 1, 2, 3, 6, 301, 998, 999, 1000, 1100 -> true;
            default -> false;
        };
    }

    public static void clearForRebaked(Map<ModelResourceLocation, BakedModel> modelRegistry) {
        models = modelRegistry;
        ++loadVersion;
        ExtraModelManager.initCTMDetected();
        if (ClientCon.getUseLevel() != null) {
            ClientRef.updateClientSide(ClientCon.getUseLevel().registryAccess());
        }
        snowOverlayLeaves = BlockModelShaper.stateToModelLocation((BlockState)((Block)BlockRegistry.snowyLeaves.get()).defaultBlockState());
        snowySlabBottom = BlockModelShaper.stateToModelLocation((BlockState)((BlockState)((BlockState)((Block)BlockRegistry.snowySlab.get()).defaultBlockState().setValue((Property)SlabBlock.TYPE, (Comparable)SlabType.BOTTOM)).setValue((Property)SlabBlock.WATERLOGGED, (Comparable)Boolean.valueOf(false))));
        snowOverlayBlock = BlockModelShaper.stateToModelLocation((BlockState)((Block)BlockRegistry.snowyBlock.get()).defaultBlockState());
    }

    public static void registerExtraSnowyModels(BiConsumer<ModelResourceLocation, UnbakedModel> registerModelAndDependenceMethod) {
        extraSnowModelBuilds.clear();
        Map<ResourceLocation, ESModelLoadedJson> snowModelLoadedJsonMap = ClientJsonCacheListener.modelDefCache.build(ESModelLoadedJson.CODEC);
        EclipticSeasons.logger("Try to register extra model definitions with size %s.".formatted(snowModelLoadedJsonMap.size()));
        snowModelLoadedJsonMap.forEach((resourceLocation, loadedJson) -> {
            if (!loadedJson.getRequire().isEmpty()) {
                for (String modid : loadedJson.getRequire()) {
                    if (Platform.isModLoaded(modid)) continue;
                    return;
                }
            }
            if (loadedJson.getMultiPartLike().isValid()) {
                ModelResourceLocation mrl = ExtraModelManager.extra_mrl(resourceLocation, "0");
                registerModelAndDependenceMethod.accept(mrl, (UnbakedModel)loadedJson.getMultiPartLike());
                extraSnowModelBuilds.put((ResourceLocation)resourceLocation, new ModelResolver(List.of(new ModelTester(mrl, loadedJson.isReplace(), List.of()))));
            } else {
                loadedJson.getVariants().forEach((va, multiVariant) -> {
                    ModelResourceLocation mrl = ExtraModelManager.extra_mrl(resourceLocation, va.replaceAll("=", "_").replace(",", "_"));
                    registerModelAndDependenceMethod.accept(mrl, (UnbakedModel)multiVariant);
                    extraSnowModelBuilds.compute((ResourceLocation)resourceLocation, (sss, solver) -> {
                        if (solver == null) {
                            solver = new ModelResolver(new ArrayList<ModelTester>());
                        }
                        ArrayList<SnowDefinition.PropertyTester> test = new ArrayList<SnowDefinition.PropertyTester>();
                        for (String s : va.split(",")) {
                            String[] split = s.split("=");
                            if (split.length != 2) continue;
                            test.add(SnowDefinition.PropertyTester.builder().name(split[0]).matcher(SnowDefinition.ExactMatcher.builder().value(split[1]).build()).build());
                        }
                        solver.modelTesters().add(new ModelTester(mrl, loadedJson.isReplace(), test));
                        return solver;
                    });
                });
            }
        });
    }

    public static BlockModel remappingSeasonTextures(ResourceLocation resourceLocation, BlockModel returnValue) {
        if (SEASONAL_TEXTURE_HASH_MAP.containsKey(resourceLocation)) {
            List<SeasonalTexture> seasonalTexture = SEASONAL_TEXTURE_HASH_MAP.get(resourceLocation);
            return SolarBlockModel.of(returnValue).setSeasonalTexture(seasonalTexture);
        }
        return null;
    }

    @Nullable
    public static ModelTester getSeasonalModel(BlockState state, ResourceLocation identifier) {
        ModelResolver resolver = extraSnowModelBuilds.get(identifier);
        return resolver == null ? null : resolver.tryFind(state);
    }

    static {
        snowOverlayLeaves = new ModelResourceLocation(BlockRegistry.snowyLeaves.getId(), "");
        snowySlabBottom = new ModelResourceLocation(BlockRegistry.snowySlab.getId(), "type=bottom,waterlogged=false");
        snowOverlayBlock = new ModelResourceLocation(BlockRegistry.snowyBlock.getId(), "");
        ice = ExtraModelManager.mrl("block/ice");
        snowy_custom = ExtraModelManager.mrl("block/snowy_custom");
        snowy_custom_ao = ExtraModelManager.mrl("block/snowy_custom_ao");
        stairs_top = ExtraModelManager.mrl("block/stairs_top");
        snowy_leaves_attach = ExtraModelManager.mrl("block/snowy_leaves_attach");
        snowy_leaves_top = ExtraModelManager.mrl("block/snowy_leaves_top");
        snowy_fern = ExtraModelManager.mrl("block/snowy_fern");
        snowy_grass = ExtraModelManager.mrl("block/snowy_grass");
        snowy_large_fern_bottom = ExtraModelManager.mrl("block/snowy_large_fern_bottom");
        snowy_large_fern_top = ExtraModelManager.mrl("block/snowy_large_fern_top");
        snowy_tall_grass_bottom = ExtraModelManager.mrl("block/snowy_tall_grass_bottom");
        snowy_tall_grass_top = ExtraModelManager.mrl("block/snowy_tall_grass_top");
        overlay_2 = ExtraModelManager.mrl("block/overlay_2");
        snow_height2 = ExtraModelManager.mrl("block/snow_height2");
        snow_height2_top = ExtraModelManager.mrl("block/snow_height2_top");
        grass_flower = ExtraModelManager.mrl("block/grass_flower");
        flower_on_grass = Stream.of(1, 2, 3, 4, 5, 6).map(i -> ExtraModelManager.mrl("block/flower_%s".formatted(i))).collect(Collectors.toCollection(ArrayList::new));
        snow_edge_overlays = IntStream.rangeClosed(0, 18).mapToObj(i -> ExtraModelManager.mrl("block/snow_edge/snow_edge_overlay_%s".formatted(i))).collect(Collectors.toCollection(ArrayList::new));
        fourleaf_clovers = IntStream.rangeClosed(0, 6).mapToObj(i -> ExtraModelManager.mrl("block/fourleaf_clover/fourleaf_clover_%s".formatted(i))).collect(Collectors.toCollection(ArrayList::new));
        snow = ResourceLocation.withDefaultNamespace((String)"block/snow");
        snow_overlay_half_left = ExtraModelManager.textureRL("snow_overlay_half_left");
        snow_overlay_half_right = ExtraModelManager.textureRL("snow_overlay_half_right");
        snow_overlay = ExtraModelManager.textureRL("snow_overlay");
        snow_overlay_leaves = ExtraModelManager.textureRL("snow_overlay_leaves");
        snow_overlay_tiny = ExtraModelManager.textureRL("snow_overlay_tiny");
        snow_spot_overlay_leaves = ExtraModelManager.textureRL("snow_spot_overlay_leaves");
        ctmStates = new IdentityHashMap<Block, CtmProperties>();
        ctmTiles = new HashMap<ResourceLocation, Void>();
        EMPTY = List.of();
        HORIZONTAL_DIRECTIONS = Direction.Plane.HORIZONTAL.stream().toList();
        SNOW_LAYER_DIRECTIONS_TO_CHECK = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        SNOW_LAYER_DIRECTIONS_TO_CHECK_4 = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)};
        SNOW_LAYER_DIRECTIONS_TO_CHECK_8 = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1), new BlockPos(1, 0, 1), new BlockPos(1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(-1, 0, -1)};
        extraSnowModels = HashMap.newHashMap(1024);
        extraSnowModelBuilds = HashMap.newHashMap(1024);
        SEASONAL_TEXTURE_HASH_MAP = new HashMap<ResourceLocation, List<SeasonalTexture>>();
    }
}

