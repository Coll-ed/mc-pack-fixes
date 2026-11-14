package fuzs.easymagic.rerollmemory;

/**
 * Mixin 挂钩点的描述符 —— **单一来源**。
 *
 * 这些是 {@code static final String} 编译期常量，javac 会把它们直接内联进注解里，
 * 所以运行时不依赖本类、也不会往目标类里塞字段。
 *
 * 名字空间说明：本实例运行时是 **Mojang 官方名**，本 mixin 配置不声明 refmap，
 * 所以这里全部用 Mojang 名。
 */
public final class RerollMemoryTargets {

    /** 附魔生成点所在类（原版）。 */
    public static final String ENCHANTMENT_MENU = "net.minecraft.world.inventory.EnchantmentMenu";

    /** Easy Magic 自己的附魔台菜单（继承上面那个）。 */
    public static final String MOD_ENCHANTMENT_MENU = "fuzs.easymagic.world.inventory.ModEnchantmentMenu";

    /**
     * {@code EnchantmentMenu#getEnchantmentList(RegistryAccess, ItemStack, int, int)}。
     * 全游戏唯一的附魔抽取入口 —— Easy Magic 的刷新与真正附魔都走它。
     */
    public static final String GET_ENCHANTMENT_LIST =
            "getEnchantmentList(Lnet/minecraft/core/RegistryAccess;"
                    + "Lnet/minecraft/world/item/ItemStack;II)Ljava/util/List;";

    /**
     * {@code EnchantmentHelper#selectEnchantment(RandomSource, ItemStack, int, Stream)}。
     * 第 4 个参数（index=3）是候选附魔流（**整张附魔表 tag**，不是物品适用的那些）。
     */
    public static final String SELECT_ENCHANTMENT =
            "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;"
                    + "selectEnchantment(Lnet/minecraft/util/RandomSource;"
                    + "Lnet/minecraft/world/item/ItemStack;I"
                    + "Ljava/util/stream/Stream;)Ljava/util/List;";

    /** 候选流参数在 selectEnchantment 里的下标。 */
    public static final int SELECT_ENCHANTMENT_CANDIDATES_INDEX = 3;

    /**
     * {@code EnchantmentMenu#clickMenuButton(Player, int)}。
     * 只有真正附上附魔时才返回 true；Easy Magic 的刷新是 buttonId 4，
     * 在它自己的重写里处理掉、不会走 super ⇒ 钩在这里只对应"附魔完成"。
     */
    public static final String CLICK_MENU_BUTTON =
            "clickMenuButton(Lnet/minecraft/world/entity/player/Player;I)Z";

    /**
     * {@code ModEnchantmentMenu#updateLevelsAndClues(RegistryAccess, ItemStack, int)}（private）。
     *
     * 这是**界面提示唯一被算出来的地方**：循环 3 个槽位 → getEnchantmentCost →
     * getEnchantmentList → createClue → 存进 {@code clues}。
     * v2 在这里记录"玩家到底看到了什么"，替代旧的在 getEnchantmentList 记整份候选。
     *
     * 第三个参数是书架数量（传给 getEnchantmentCost 用），不是槽位下标。
     */
    public static final String UPDATE_LEVELS_AND_CLUES =
            "updateLevelsAndClues(Lnet/minecraft/core/RegistryAccess;"
                    + "Lnet/minecraft/world/item/ItemStack;I)V";

    /** 提示列表字段 {@code ModEnchantmentMenu#clues}（public final，元素是每槽位的提示）。 */
    public static final String FIELD_CLUES = "clues";

    /** 泛型擦除后的字段描述符。 */
    public static final String DESC_CLUES = "Ljava/util/List;";

    /** {@code ModEnchantmentMenu#player}（public final）——服务端玩家，用来定位池子。 */
    public static final String FIELD_PLAYER = "player";

    /** 玩家字段描述符。 */
    public static final String DESC_PLAYER = "Lnet/minecraft/world/entity/player/Player;";

    /**
     * Easy Magic 自带的 accessor 界面（它的 mixin 配置已挂到 EnchantmentMenu 上）。
     * 我们用它的 {@code @Invoker} 来"用原版方法本身重跑一次" —— 这样空池子保护
     * 不需要复刻原版逻辑（种子、书的额外剔除等都自动一致）。
     *
     * 依赖是显式的：VerifyEasyMagic 会校验该类里确实有下面这个方法。
     */
    public static final String ENCHANTMENT_MENU_ACCESSOR =
            "fuzs.easymagic.mixin.accessor.EnchantmentMenuAccessor";

    /** accessor 里 {@code @Invoker} 的方法名；描述符与 {@link #GET_ENCHANTMENT_LIST} 相同。 */
    public static final String ACCESSOR_GET_ENCHANTMENT_LIST = "callGetEnchantmentList";

    /**
     * {@code EnchantmentMenu#enchantSlots} —— 附魔台 3 格容器（0=待附魔物品）。
     * 用途：附魔按钮按下时读第 0 格，拿到**附魔前**的物品，决定清哪个类别的池子。
     */
    public static final String FIELD_ENCHANT_SLOTS = "enchantSlots";

    /** 注意：是 {@code net.minecraft.world.Container}（不是 world.container.Container）——
     *  这个常量一开始写错过，被离线校验器比对目标类真实字段描述符时抓了出来。 */
    public static final String DESC_ENCHANT_SLOTS = "Lnet/minecraft/world/Container;";

    private RerollMemoryTargets() {
    }
}
