package de.teamlapen.vampirism.modcompat.guide;

import amerifrance.guideapi.api.GuideAPI;
import amerifrance.guideapi.api.IGuideBook;
import amerifrance.guideapi.api.IPage;
import amerifrance.guideapi.api.impl.Book;
import amerifrance.guideapi.api.impl.abstraction.EntryAbstract;
import amerifrance.guideapi.api.util.PageHelper;
import amerifrance.guideapi.category.CategoryItemStack;
import amerifrance.guideapi.page.PageImage;
import amerifrance.guideapi.page.PageText;
import amerifrance.guideapi.page.PageTextImage;
import com.google.common.collect.Maps;
import de.teamlapen.lib.VampLib;
import de.teamlapen.lib.lib.util.UtilLib;
import de.teamlapen.vampirism.VampirismMod;
import de.teamlapen.vampirism.blocks.BlockAltarPillar;
import de.teamlapen.vampirism.blocks.BlockGarlicBeacon;
import de.teamlapen.vampirism.client.core.ModKeys;
import de.teamlapen.vampirism.config.Balance;
import de.teamlapen.vampirism.core.ModBlocks;
import de.teamlapen.vampirism.core.ModEntities;
import de.teamlapen.vampirism.core.ModItems;
import de.teamlapen.vampirism.items.ItemBloodBottle;
import de.teamlapen.vampirism.items.ItemCrossbowArrow;
import de.teamlapen.vampirism.items.ItemInjection;
import de.teamlapen.vampirism.modcompat.guide.pages.PageHolderWithLinks;
import de.teamlapen.vampirism.modcompat.guide.pages.PageTable;
import de.teamlapen.vampirism.player.hunter.HunterLevelingConf;
import de.teamlapen.vampirism.player.hunter.actions.HunterActions;
import de.teamlapen.vampirism.player.vampire.VampireLevelingConf;
import de.teamlapen.vampirism.util.REFERENCE;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.awt.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static de.teamlapen.vampirism.modcompat.guide.GuideHelper.RECIPE_TYPE.*;
import static de.teamlapen.vampirism.modcompat.guide.GuideHelper.addArmorWithTier;
import static de.teamlapen.vampirism.modcompat.guide.GuideHelper.addItemWithTier;


@amerifrance.guideapi.api.GuideBook
public class GuideBook implements IGuideBook {

    public final static String TAG = "GuideBook";
    private final static String IMAGE_BASE = "vampirismguide:textures/images/";
    private static Book guideBook;
    private static Map<ResourceLocation, EntryAbstract> links = Maps.newHashMap();


    static void buildCategories() {
        VampirismMod.log.d(TAG, "Building categories");
        long start = System.currentTimeMillis();
        guideBook.addCategory(new CategoryItemStack(buildOverview(), "guide.vampirism.overview.title", new ItemStack(ModItems.vampireFang)));
        guideBook.addCategory(new CategoryItemStack(buildVampire(), "guide.vampirism.vampire.title", new ItemStack(ModItems.bloodBottle, 1, ItemBloodBottle.AMOUNT)));
        guideBook.addCategory(new CategoryItemStack(buildHunter(), "guide.vampirism.hunter.title", new ItemStack(ModItems.humanHeart)));
        guideBook.addCategory(new CategoryItemStack(buildCreatures(), "guide.vampirism.entity.title", new ItemStack(Items.SKULL)));
        guideBook.addCategory(new CategoryItemStack(buildWorld(), "guide.vampirism.world.title", new ItemStack(ModBlocks.cursedEarth)));
        guideBook.addCategory(new CategoryItemStack(buildItems(), "guide.vampirism.items.title", new ItemStack(Items.APPLE)));
        guideBook.addCategory(new CategoryItemStack(buildBlocks(), "guide.vampirism.blocks.title", new ItemStack(ModBlocks.castleBlock)));
        VampirismMod.log.d(TAG, "Finished building categories after %d ms", System.currentTimeMillis() - start);
    }

    /**
     * @return The entry registered with the given location. Can be null
     */
    public static
    @Nullable
    EntryAbstract getLinkedEntry(ResourceLocation location) {
        return links.get(location);
    }

    private static Map<ResourceLocation, EntryAbstract> buildOverview() {
        Map<ResourceLocation, EntryAbstract> entries = new LinkedHashMap<>();
        String base = "guide.vampirism.overview.";

        List<IPage> introPages = new ArrayList<>();
        introPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "intro.text")));
        PageHelper.setPagesToUnicode(introPages);
        entries.put(new ResourceLocation(base + "intro"), new EntryText(introPages, UtilLib.translate(base + "intro")));

        List<IPage> gettingStartedPages = new ArrayList<>();
        IPage p = new PageText(UtilLib.translate(base + "getting_started.text"));
        p = new PageHolderWithLinks(p).addLink("guide.vampirism.vampire.getting_started").addLink("guide.vampirism.hunter.getting_started");
        gettingStartedPages.add(p);
        PageHelper.setPagesToUnicode(gettingStartedPages);
        entries.put(new ResourceLocation(base + "gettingStarted"), new EntryText(gettingStartedPages, UtilLib.translate(base + "getting_started")));

        List<IPage> configPages = new ArrayList<>();
        configPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "config.text")));
        configPages.addAll(GuideHelper.pagesForLongText(GuideHelper.append(base + "config.general.text", base + "config.general.examples")));
        configPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "config.balance.text")));
        PageHelper.setPagesToUnicode(configPages);
        entries.put(new ResourceLocation(base + "config"), new EntryText(configPages, UtilLib.translate(base + "config")));

        List<IPage> troublePages = new ArrayList<>();
        troublePages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "trouble.text")));
        PageHelper.setPagesToUnicode(troublePages);
        GuideHelper.addLinks(troublePages, new PageHolderWithLinks.URLLink(UtilLib.translate(base + "trouble"), URI.create("https://github.com/TeamLapen/Vampirism/wiki/Troubleshooting")));
        entries.put(new ResourceLocation(base + "trouble"), new EntryText(troublePages, UtilLib.translate(base + "trouble")));

        List<IPage> devPages = new ArrayList<>();
        PageHolderWithLinks.URLLink helpLink = new PageHolderWithLinks.URLLink("How to help", URI.create("https://github.com/TeamLapen/Vampirism/wiki#how-you-can-help"));
        devPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(UtilLib.translate(base + "dev.text")), helpLink));
        PageHelper.setPagesToUnicode(devPages);
        entries.put(new ResourceLocation(base + "dev"), new EntryText(devPages, UtilLib.translate(base + "dev")));

        List<IPage> supportPages = new ArrayList<>();
        supportPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "support.text")));
        PageHolderWithLinks.URLLink linkPatreon = new PageHolderWithLinks.URLLink("Patreon", URI.create(REFERENCE.PATREON_LINK));
        PageHolderWithLinks.URLLink linkCurseForge = new PageHolderWithLinks.URLLink("CurseForge", URI.create(REFERENCE.CURSEFORGE_LINK));

        GuideHelper.addLinks(supportPages, linkPatreon, linkCurseForge, new ResourceLocation(base + "dev"));
        PageHelper.setPagesToUnicode(supportPages);
        entries.put(new ResourceLocation(base + "support"), new EntryText(supportPages, UtilLib.translate(base + "support")));

        List<IPage> creditsPages = new ArrayList<>();
        String lang = VampLib.proxy.getActiveLanguage();
        String credits = "§lDeveloper:§r\nMaxanier\n\n§lInactive Developer:§r\nMistadon\nwildbill22\n\n§lTranslators:§r\n§b" + lang + "§r\n" + UtilLib.translate("text.vampirism.translators");
        creditsPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(credits)));
        entries.put(new ResourceLocation(base + "credits"), new EntryText(creditsPages, UtilLib.translate(base + "credits")));
        links.putAll(entries);
        return entries;
    }

    private static Map<ResourceLocation, EntryAbstract> buildVampire() {
        Map<ResourceLocation, EntryAbstract> entries = new LinkedHashMap<>();
        String base = "guide.vampirism.vampire.";

        List<IPage> gettingStarted = new ArrayList<>();
        gettingStarted.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "getting_started.become")));
        gettingStarted.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "getting_started.as_vampire")));
        gettingStarted.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "getting_started.blood", Keyboard.getKeyName(ModKeys.getKeyCode(ModKeys.KEY.SUCK)))));
        gettingStarted.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "getting_started.level") + "\n" + UtilLib.translate(base + "getting_started.level2")));

        entries.put(new ResourceLocation(base + "getting_started"), new EntryText(gettingStarted, UtilLib.translate(base + "getting_started")));

        List<IPage> bloodPages = new ArrayList<>();
        bloodPages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "blood.text", UtilLib.translate(ModItems.bloodBottle.getUnlocalizedName() + ".name"), UtilLib.translate(Items.GLASS_BOTTLE.getUnlocalizedName() + ".name"))));
        bloodPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "blood.storage", ModBlocks.bloodContainer.getLocalizedName())), new ResourceLocation("guide.vampirism.blocks.blood_container")));
        bloodPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(UtilLib.translate(base + "blood.biteable_creatures")), new PageHolderWithLinks.URLLink("Biteable Creatures", URI.create("https://github.com/TeamLapen/Vampirism/wiki/Biteable-Creatures"))));
        entries.put(new ResourceLocation(base + "blood"), new EntryText(bloodPages, UtilLib.translate(base + "blood")));

        VampireLevelingConf levelingConf = VampireLevelingConf.getInstance();
        List<IPage> levelingPages = new ArrayList<>();
        levelingPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "leveling.intro")));
        String altarOfInspiration = "§l" + ModBlocks.altarInspiration.getLocalizedName() + "§r\n§o" + UtilLib.translate(base + "leveling.inspiration.reach") + "§r\n";
        altarOfInspiration += UtilLib.translate(base + "leveling.inspiration.text") + "\n";
        altarOfInspiration += UtilLib.translateFormatted(base + "leveling.inspiration.requirements", levelingConf.getRequiredBloodForAltarInspiration(2), levelingConf.getRequiredBloodForAltarInspiration(3), levelingConf.getRequiredBloodForAltarInspiration(4));
        levelingPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(altarOfInspiration), new ResourceLocation("guide.vampirism.blocks.altar_inspiration")));

        String altarOfInfusion = "§l" + ModBlocks.altarInfusion.getLocalizedName() + "§r\n§o" + UtilLib.translate(base + "leveling.infusion.reach") + "§r\n";
        altarOfInfusion += UtilLib.translateFormatted(base + "leveling.infusion.intro", ModBlocks.altarInfusion.getLocalizedName(), ModBlocks.altarPillar.getLocalizedName(), ModBlocks.altarTip.getLocalizedName());
        levelingPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(altarOfInfusion), new ResourceLocation("guide.vampirism.blocks.altar_infusion")));
        String blocks = "";
        for (BlockAltarPillar.EnumPillarType t : BlockAltarPillar.EnumPillarType.values()) {
            if (t == BlockAltarPillar.EnumPillarType.NONE) continue;
            blocks += t.fillerBlock.getLocalizedName() + "(" + t.getValue() + "),";
        }
        levelingPages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "leveling.infusion.structure", blocks)));
        String items = UtilLib.translate(ModItems.humanHeart.getUnlocalizedName() + ".name") + ", " + UtilLib.translate(ModItems.pureBlood.getUnlocalizedName() + ".name") + ", " + UtilLib.translate(ModItems.vampireBook.getUnlocalizedName() + ".name");
        levelingPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "leveling.infusion.items", items)), new ResourceLocation("guide.vampirism.items.human_heart"), new ResourceLocation("guide.vampirism.items.pure_blood"), new ResourceLocation("guide.vampirism.items.vampire_book")));
        PageTable.Builder requirementsBuilder = new PageTable.Builder(5);
        requirementsBuilder.addUnlocLine("text.vampirism.level", base + "leveling.infusion.req.structure_points", ModItems.pureBlood.getUnlocalizedName() + ".name", base + "leveling.infusion.req.heart", base + "leveling.infusion.req.book");
        requirementsBuilder.addLine("5", "8", "0", "5", "1");
        requirementsBuilder.addLine("6", "17", "1 Purity(1)", "5", "1");
        requirementsBuilder.addLine("7", "21", "1 Purity(1)", "10", "1");
        requirementsBuilder.addLine("8", "26", "1 Purity(2)", "10", "1");
        requirementsBuilder.addLine("9", "35", "1 Purity(2)", "10", "1");
        requirementsBuilder.addLine("10", "44", "1 Purity(3)", "15", "1");
        requirementsBuilder.addLine("11", "54", "1 Purity(3)", "15", "1");
        requirementsBuilder.addLine("12", "63", "1 Purity(4)", "20", "1");
        requirementsBuilder.addLine("13", "72", "2 Purity(4)", "20", "1");
        requirementsBuilder.addLine("14", "92", "2 Purity(5)", "25", "1");
        requirementsBuilder.setHeadline(UtilLib.translate(base + "leveling.infusion.req"));
        PageHolderWithLinks requirementTable = new PageHolderWithLinks(requirementsBuilder.build());
        requirementTable.addLink(new ResourceLocation("guide.vampirism.items.human_heart"));
        requirementTable.addLink(new ResourceLocation("guide.vampirism.items.vampire_book"));
        requirementTable.addLink(new ResourceLocation("guide.vampirism.items.pure_blood"));
        levelingPages.add(requirementTable);

        levelingPages.add(new PageTextImage(UtilLib.translate(base + "leveling.infusion.image1"), new ResourceLocation(IMAGE_BASE + "infusion1.png"), false));
        levelingPages.add(new PageTextImage(UtilLib.translate(base + "leveling.infusion.image2"), new ResourceLocation(IMAGE_BASE + "infusion2.png"), false));
        levelingPages.add(new PageTextImage(UtilLib.translate(base + "leveling.infusion.image3"), new ResourceLocation(IMAGE_BASE + "infusion3.png"), false));
        levelingPages.add(new PageTextImage(UtilLib.translate(base + "leveling.infusion.image4"), new ResourceLocation(IMAGE_BASE + "infusion4.png"), false));
        levelingPages.add(new PageTextImage(UtilLib.translate(base + "leveling.infusion.image5"), new ResourceLocation(IMAGE_BASE + "infusion5.png"), false));

        entries.put(new ResourceLocation(base + "leveling"), new EntryText(levelingPages, base + "leveling"));


        List<IPage> skillPages = new ArrayList<>();
        skillPages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "skills.text", Keyboard.getKeyName(ModKeys.getKeyCode(ModKeys.KEY.SKILL)))));
        skillPages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "skills.actions", Keyboard.getKeyName(ModKeys.getKeyCode(ModKeys.KEY.ACTION)))));
        skillPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "skills.actions2")));

        entries.put(new ResourceLocation(base + "skills"), new EntryText(skillPages, base + "skills"));

        List<IPage> unvampirePages = new ArrayList<>();
        unvampirePages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "unvampire.text", ModBlocks.churchAltar.getLocalizedName())));
        entries.put(new ResourceLocation(base + "unvampire"), new EntryText(unvampirePages, base + "unvampire"));

        links.putAll(entries);
        return entries;
    }

    private static Map<ResourceLocation, EntryAbstract> buildHunter() {
        Map<ResourceLocation, EntryAbstract> entries = new LinkedHashMap<>();
        String base = "guide.vampirism.hunter.";

        List<IPage> gettingStarted = new ArrayList<>();
        String become = UtilLib.translateFormatted(base + "getting_started.become", UtilLib.translate("entity." + ModEntities.HUNTER_TRAINER + ".name"), new ItemStack(ModItems.injection, 1, ItemInjection.META_GARLIC).getDisplayName());
        gettingStarted.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(become), new ResourceLocation("guide.vampirism.items.injection")));
        gettingStarted.add(new PageImage(new ResourceLocation(IMAGE_BASE + "hunter_trainer.png")));
        gettingStarted.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "getting_started.asHunter")));
        entries.put(new ResourceLocation(base + "getting_started"), new EntryText(gettingStarted, base + "getting_started"));

        HunterLevelingConf levelingConf = HunterLevelingConf.instance();
        List<IPage> levelingPages = new ArrayList<>();
        levelingPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "leveling.intro")));
        String train1 = "§l" + UtilLib.translateFormatted(base + "leveling.toReach", "2-4") + "§r\n";
        train1 += UtilLib.translateFormatted(base + "leveling.train1.text", levelingConf.getVampireBloodCountForBasicHunter(2), levelingConf.getVampireBloodCountForBasicHunter(3), levelingConf.getVampireBloodCountForBasicHunter(4));
        levelingPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(train1), new ResourceLocation("guide.vampirism.items.stake"), new ResourceLocation("guide.vampirism.items.vampire_blood_bottle")));

        String train2 = "§l" + UtilLib.translateFormatted(base + "leveling.to_reach", "5+") + "§r\n";
        train2 += UtilLib.translateFormatted(base + "leveling.train2.text", ModBlocks.hunterTable.getLocalizedName());
        levelingPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(train2), new ResourceLocation("guide.vampirism.blocks.hunter_table")));
        PageTable.Builder builder = new PageTable.Builder(4);
        builder.addUnlocLine("text.vampirism.level", base + "leveling.train2.fang", ModItems.pureBlood.getLocalizedName(), ModItems.vampireBook.getLocalizedName());
        for (int i = levelingConf.TABLE_MIN_LEVEL; i <= levelingConf.TABLE_MAX_LEVEL; i++) {
            int[] req = levelingConf.getItemRequirementsForTable(i);
            String pure = "";
            if (req[1] > 0) {
                pure = "" + req[1] + " Purity(" + (req[2] + 1) + ")";
            }
            builder.addLine(i, req[0], pure, req[3]);
        }

        builder.setHeadline(UtilLib.translate(base + "leveling.train2.req"));
        PageHolderWithLinks requirementsTable = new PageHolderWithLinks(builder.build());
        requirementsTable.addLink(new ResourceLocation("guide.vampirism.items.vampire_fang"));
        requirementsTable.addLink(new ResourceLocation("guide.vampirism.items.pure_blood"));
        requirementsTable.addLink(new ResourceLocation("guide.vampirism.items.vampire_book"));
        levelingPages.add(requirementsTable);

        entries.put(new ResourceLocation(base + "leveling"), new EntryText(levelingPages, base + "leveling"));

        List<IPage> skillPages = new ArrayList<>();
        skillPages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "skills.intro", Keyboard.getKeyName(ModKeys.getKeyCode(ModKeys.KEY.SKILL)))));
        String disguise = String.format("§l%s§r\n", UtilLib.translate(HunterActions.disguiseAction.getUnlocalizedName()));
        disguise += UtilLib.translateFormatted(base + "skills.disguise.text", Keyboard.getKeyName(ModKeys.getKeyCode(ModKeys.KEY.ACTION)));
        skillPages.addAll(GuideHelper.pagesForLongText(disguise));
        String bloodPotion = String.format("§l%s§r\n", ModBlocks.bloodPotionTable.getLocalizedName());
        bloodPotion += UtilLib.translateFormatted(base + "skills.blood_potion.text", Keyboard.getKeyName(ModKeys.getKeyCode(ModKeys.KEY.BLOOD_POTION)));
        skillPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(bloodPotion), new ResourceLocation("guide.vampirism.blocks.blood_potion_table")));
        String weaponTable = String.format("§l%s§r\n", ModBlocks.weaponTable.getLocalizedName());
        weaponTable += UtilLib.translate(base + "skills.weapon_table.text");
        skillPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(weaponTable), new ResourceLocation("guide.vampirism.blocks.weapon_table")));
        entries.put(new ResourceLocation(base + "skills"), new EntryText(skillPages, base + "skills"));

        List<IPage> vampSlayerPages = new ArrayList<>();
        vampSlayerPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "vamp_slayer.intro")));
        String garlic = String.format("§l%s§r\n", ModItems.itemGarlic.getLocalizedName());
        garlic += UtilLib.translate(base + "vamp_slayer.garlic") + "\n" + UtilLib.translate(base + "vamp_slayer.garlic2") + "\n" + UtilLib.translate(base + "vamp_slayer.garlic.diffusor");
        vampSlayerPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(garlic), new ResourceLocation("guide.vampirism.blocks.garlic_beacon")));
        String holyWater = String.format("§l%s§r\n", ModItems.holyWaterBottle.getLocalizedName());
        holyWater += UtilLib.translate(base + "vamp_slayer.holy_water");
        vampSlayerPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(holyWater), new ResourceLocation("guide.vampirism.items.holy_water_bottle")));
        String fire = String.format("§l%s§r\n", Blocks.FIRE.getLocalizedName());
        fire += UtilLib.translate(base + "vamp_slayer.fire");
        vampSlayerPages.addAll(GuideHelper.addLinks(GuideHelper.pagesForLongText(fire), new ResourceLocation("guide.vampirism.items.item_alchemical_fire"), new ResourceLocation("guide.vampirism.items.crossbow_arrow")));
        entries.put(new ResourceLocation(base + "vamp_slayer"), new EntryText(vampSlayerPages, base + "vamp_slayer"));

        List<IPage> unHunterPages = new ArrayList<>();
        unHunterPages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "unhunter.text", new ItemStack(ModItems.injection, 1, ItemInjection.META_SANGUINARE).getDisplayName())));
        entries.put(new ResourceLocation(base + "unhunter"), new EntryText(unHunterPages, base + "unhunter"));

        links.putAll(entries);
        return entries;
    }

    private static Map<ResourceLocation, EntryAbstract> buildCreatures() {
        Map<ResourceLocation, EntryAbstract> entries = new LinkedHashMap<>();
        String base = "guide.vampirism.entity.";

        ArrayList<IPage> generalPages = new ArrayList<>();
        generalPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "general.text")));
        entries.put(new ResourceLocation(base + "general"), new EntryText(generalPages, base + "general"));

        ArrayList<IPage> hunterPages = new ArrayList<>();
        hunterPages.add(new PageImage(new ResourceLocation(IMAGE_BASE + "hunter.png")));
        hunterPages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "hunter.text", ModItems.humanHeart.getLocalizedName())));
        entries.put(new ResourceLocation(base + "hunter"), new EntryText(hunterPages, "entity.vampirism." + ModEntities.BASIC_HUNTER_NAME + ".name"));

        ArrayList<IPage> vampirePages = new ArrayList<>();
        vampirePages.add(new PageImage(new ResourceLocation(IMAGE_BASE + "vampire.png")));
        vampirePages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "vampire.text", ModItems.vampireFang.getLocalizedName(), ModItems.vampireBlood.getLocalizedName(), ModItems.stake.getLocalizedName())));
        entries.put(new ResourceLocation(base + "vampire"), new EntryText(vampirePages, "entity.vampirism." + ModEntities.BASIC_VAMPIRE_NAME + ".name"));

        ArrayList<IPage> advancedHunterPages = new ArrayList<>();
        advancedHunterPages.add(new PageImage(new ResourceLocation(IMAGE_BASE + "advanced_hunter.png")));
        advancedHunterPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "advanced_hunter.text")));
        entries.put(new ResourceLocation(base + "advanced_hunter"), new EntryText(advancedHunterPages, "entity.vampirism." + ModEntities.ADVANCED_HUNTER + ".name"));

        ArrayList<IPage> advancedVampirePages = new ArrayList<>();
        advancedVampirePages.add(new PageImage(new ResourceLocation(IMAGE_BASE + "advanced_vampire.png")));
        advancedVampirePages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "advancedVampire.text", ModItems.bloodBottle.getLocalizedName(), ModItems.vampireBlood.getLocalizedName())));
        entries.put(new ResourceLocation(base + "advanced_vampire"), new EntryText(advancedVampirePages, "entity.vampirism." + ModEntities.ADVANCED_VAMPIRE + ".name"));

        ArrayList<IPage> vampireBaronPages = new ArrayList<>();
        vampireBaronPages.add(new PageImage(new ResourceLocation(IMAGE_BASE + "vampire_baron.png")));
        vampireBaronPages.addAll(GuideHelper.pagesForLongText(UtilLib.translateFormatted(base + "vampire_baron.text", ModItems.pureBlood.getLocalizedName())));
        GuideHelper.addLinks(vampireBaronPages, new ResourceLocation("guide.vampirism.world.vampire_forest"));
        entries.put(new ResourceLocation(base + "vampireBaron"), new EntryText(vampireBaronPages, "entity.vampirism." + ModEntities.VAMPIRE_BARON + ".name"));

        ArrayList<IPage> minionPages = new ArrayList<>();
        minionPages.add(new PageImage(new ResourceLocation(IMAGE_BASE + "minion.png")));
        minionPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "minion.text")));
        entries.put(new ResourceLocation(base + "minion"), new EntryText(minionPages, "entity.vampirism." + ModEntities.VAMPIRE_MINION_SAVEABLE_NAME + ".name"));

        ArrayList<IPage> ghostPages = new ArrayList<>();
        ghostPages.add(new PageImage(new ResourceLocation(IMAGE_BASE + "ghost.png")));
        ghostPages.addAll(GuideHelper.pagesForLongText(UtilLib.translate(base + "ghost.text")));
        entries.put(new ResourceLocation(base + "ghost"), new EntryText(ghostPages, "entity.vampirism." + ModEntities.GHOST_NAME + ".name"));

        links.putAll(entries);
        return entries;
    }

    private static Map<ResourceLocation, EntryAbstract> buildWorld() {
        Map<ResourceLocation, EntryAbstract> entries = new LinkedHashMap<>();
        String base = "guide.vampirism.world.";

        List<IPage> vampireForestPages = new ArrayList<>();
        vampireForestPages.addAll(GuideHelper.pagesForLongText(base + "vampire_forest.text"));
        entries.put(new ResourceLocation(base + "vampire_forest"), new EntryText(vampireForestPages, base + "vampire_forest"));

        List<IPage> wipPages = new ArrayList<>();
        wipPages.addAll(GuideHelper.pagesForLongText(base + "wip.text"));
        entries.put(new ResourceLocation(base + "wip"), new EntryText(wipPages, base + "wip"));

        links.putAll(entries);
        return entries;
    }

    private static Map<ResourceLocation, EntryAbstract> buildItems() {
        Map<ResourceLocation, EntryAbstract> entries = new LinkedHashMap<>();
        String base = "guide.vampirism.items.";
        //General
        new ItemInfoBuilder(ModItems.vampireFang).build(entries);
        new ItemInfoBuilder(ModItems.humanHeart).build(entries);
        new ItemInfoBuilder(ModItems.pureBlood).setFormats(UtilLib.translate("entity.vampirism." + ModEntities.VAMPIRE_BARON + ".name")).build(entries);
        new ItemInfoBuilder(ModItems.vampireBlood).setFormats(UtilLib.translate("entity.vampirism." + ModEntities.BASIC_VAMPIRE_NAME + ".name"), ModItems.stake.getLocalizedName(), UtilLib.translate("entity.vampirism." + ModEntities.ADVANCED_VAMPIRE + ".name")).build(entries);
        new ItemInfoBuilder(ModItems.vampireBook).build(entries);
        //Vampire
        new ItemInfoBuilder(new ItemStack(ModItems.bloodBottle, 1, ItemBloodBottle.AMOUNT), false).build(entries);
        //Hunter
        new ItemInfoBuilder(ModItems.injection).craftableStacks(new ItemStack(ModItems.injection, 1, 0), WORKBENCH, new ItemStack(ModItems.injection, 1, ItemInjection.META_GARLIC), WORKBENCH, new ItemStack(ModItems.injection, 1, ItemInjection.META_SANGUINARE), WORKBENCH).build(entries);
        new ItemInfoBuilder(ModItems.hunterIntel).setLinks(new ResourceLocation("guide.vampirism.blocks.hunter_table")).setFormats(ModBlocks.hunterTable.getLocalizedName()).build(entries);
        new ItemInfoBuilder(ModItems.itemGarlic).build(entries);
        new ItemInfoBuilder(ModItems.purifiedGarlic).setFormats(ModBlocks.garlicBeacon.getLocalizedName()).setLinks(new ResourceLocation("guide.vampirism.blocks.garlic_beacon")).craftable(ALCHEMICAL_CAULDRON).build(entries);
        new ItemInfoBuilder(ModItems.pitchfork).craftable(WEAPON_TABLE).build(entries);
        new ItemInfoBuilder(ModItems.stake).setFormats(((int) (Balance.hps.INSTANT_KILL_SKILL_1_MAX_HEALTH_PERC * 100)) + "%").craftable(WORKBENCH).build(entries);
        new ItemInfoBuilder(ModItems.basicCrossbow).setFormats(ModItems.crossbowArrow.getLocalizedName(), ModItems.techCrossbowAmmoPackage.getLocalizedName()).setLinks(new ResourceLocation("guide.vampirism.items.crossbow_arrow")).craftableStacks(ModItems.basicCrossbow, WEAPON_TABLE, ModItems.basicDoubleCrossbow, WEAPON_TABLE, ModItems.enhancedCrossbow, WEAPON_TABLE, ModItems.enhancedDoubleCrossbow, WEAPON_TABLE, ModItems.basicTechCrossbow, WEAPON_TABLE, ModItems.techCrossbowAmmoPackage, WEAPON_TABLE).setName("crossbows").customName().build(entries);
        new ItemInfoBuilder(ModItems.crossbowArrow).craftableStacks(ModItems.crossbowArrow.getStack(ItemCrossbowArrow.EnumArrowType.NORMAL), WORKBENCH, ModItems.crossbowArrow.getStack(ItemCrossbowArrow.EnumArrowType.VAMPIRE_KILLER), WEAPON_TABLE, ModItems.crossbowArrow.getStack(ItemCrossbowArrow.EnumArrowType.SPITFIRE), WEAPON_TABLE).build(entries);
        new ItemInfoBuilder(ModItems.holyWaterBottle).setLinks(new ResourceLocation("guide.vampirism.hunter.vamp_slayer"), new ResourceLocation("guide.vampirism.items.holy_salt")).setFormats(ModItems.holySaltWater.getLocalizedName(), ModItems.holySaltWater.getLocalizedName(), ModItems.holySalt.getLocalizedName()).craftableStacks(ModItems.holySaltWater, WORKBENCH).build(entries);
        new ItemInfoBuilder(ModItems.holySalt).setLinks(new ResourceLocation("guide.vampirism.items.holy_water_bottle")).setFormats(ModItems.pureSalt.getLocalizedName(), ModItems.pureSalt.getLocalizedName(), ModBlocks.alchemicalCauldron.getLocalizedName()).craftableStacks(ModItems.pureSalt, ALCHEMICAL_CAULDRON).build(entries);
        new ItemInfoBuilder(ModItems.itemAlchemicalFire).setLinks(new ResourceLocation("guide.vampirism.items.crossbow_arrow")).craftable(ALCHEMICAL_CAULDRON).build(entries);

        addArmorWithTier(entries, "armor_of_swiftness", ModItems.armorOfSwiftness_helmet, ModItems.armorOfSwiftness_chest, ModItems.armorOfSwiftness_legs, ModItems.armorOfSwiftness_boots, WEAPON_TABLE);
        addArmorWithTier(entries, "hunter_coat", ModItems.hunterCoat_helmet, ModItems.hunterCoat_chest, ModItems.hunterCoat_legs, ModItems.hunterCoat_boots, WEAPON_TABLE);
        addArmorWithTier(entries, "obsidian_armor", ModItems.obsidianArmor_helmet, ModItems.obsidianArmor_chest, ModItems.obsidianArmor_legs, ModItems.obsidianArmor_boots, WEAPON_TABLE);
        addItemWithTier(entries, ModItems.hunterAxe, WEAPON_TABLE);
        links.putAll(entries);
        return entries;
    }

    private static Map<ResourceLocation, EntryAbstract> buildBlocks() {
        Map<ResourceLocation, EntryAbstract> entries = new LinkedHashMap<>();
        String base = "guide.vampirism.blocks.";
        //General
        new ItemInfoBuilder(ModBlocks.castleBlock).craftableStacks(new ItemStack(ModBlocks.castleBlock, 1, 3), WORKBENCH, new ItemStack(ModBlocks.castleBlock, 1, 0), WORKBENCH, new ItemStack(ModBlocks.castleBlock, 1, 1), WORKBENCH, new ItemStack(ModBlocks.castleBlock, 1, 4), WORKBENCH).build(entries);
        new ItemInfoBuilder(ModBlocks.vampirismFlower).build(entries);
        //Vampire
        new ItemInfoBuilder(ModBlocks.bloodContainer).craftable(WORKBENCH).build(entries);
        new ItemInfoBuilder(ModBlocks.altarInspiration).setLinks(new ResourceLocation("guide.vampirism.vampire.leveling")).craftable(WORKBENCH).build(entries);
        new ItemInfoBuilder(ModBlocks.altarInfusion).setLinks(new ResourceLocation("guide.vampirism.vampire.leveling")).craftableStacks(new ItemStack(ModBlocks.altarInfusion), WORKBENCH, new ItemStack(ModBlocks.altarPillar), WORKBENCH, new ItemStack(ModBlocks.altarTip), WORKBENCH).build(entries);
        new ItemInfoBuilder(new ItemStack(ModItems.itemCoffin), true).craftable(WORKBENCH).build(entries);
        new ItemInfoBuilder(ModBlocks.churchAltar).build(entries);
        //Hunter
        new ItemInfoBuilder(new ItemStack(ModItems.itemMedChair), true).setFormats((new ItemStack(ModItems.injection, 1, 1)).getDisplayName(), (new ItemStack(ModItems.injection, 1, 2)).getDisplayName()).craftable(WORKBENCH).build(entries);
        new ItemInfoBuilder(ModBlocks.hunterTable).setFormats(ModItems.hunterIntel.getLocalizedName()).setLinks(new ResourceLocation("guide.vampirism.hunter.leveling"), new ResourceLocation("guide.vampirism.items.hunter_intel")).craftable(WORKBENCH).build(entries);
        new ItemInfoBuilder(ModBlocks.weaponTable).craftable(WORKBENCH).build(entries);
        new ItemInfoBuilder(ModBlocks.bloodPotionTable).craftable(WORKBENCH).build(entries);
        new ItemInfoBuilder(ModBlocks.alchemicalCauldron).craftable(WORKBENCH).build(entries);
        int cn = Balance.hps.GARLIC_DIFFUSOR_NORMAL_DISTANCE * 2 + 1;
        int ce = Balance.hps.GARLIC_DIFFUSOR_ENHANCED_DISTANCE * 2 + 1;
        new ItemInfoBuilder(ModBlocks.garlicBeacon).setFormats(cn, cn, ce, ce, ModItems.purifiedGarlic.getLocalizedName()).setLinks(new ResourceLocation("guide.vampirism.items.item_garlic"), new ResourceLocation("guide.vampirism.items.purified_garlic"), new ResourceLocation("guide.vampirism.items.holy_water_bottle")).craftableStacks(ModBlocks.garlicBeacon, WORKBENCH, new ItemStack(ModBlocks.garlicBeacon, 1, BlockGarlicBeacon.Type.IMPROVED.getId()), WORKBENCH, ModItems.garlicBeaconCore, ALCHEMICAL_CAULDRON, ModItems.garlicBeaconCoreImproved, ALCHEMICAL_CAULDRON).build(entries);

        links.putAll(entries);
        return entries;
    }


    @Nullable
    @Override
    public Book buildBook() {
        guideBook = new Book();
        guideBook.setTitle("guide.vampirism.title");
        guideBook.setDisplayName("guide.vampirism.name");
        guideBook.setWelcomeMessage("guide.vampirism.welcome");
        guideBook.setAuthor("Maxanier");
        guideBook.setColor(Color.getHSBColor(0.5f, 0.2f, 0.5f));
        guideBook.setRegistryName(new ResourceLocation(REFERENCE.MODID, "guide"));
        guideBook.setOutlineTexture(new ResourceLocation("vampirismguide", "textures/gui/book_violet_border.png"));
        guideBook.setSpawnWithBook(true);

        VampirismMod.log.i(TAG, "Finished Building Guide Book");
        return guideBook;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void handleModel(ItemStack bookStack) {
        GuideAPI.setModel(guideBook);
    }

    @Override
    public void handlePost(ItemStack bookStack) {
        GameRegistry.addShapelessRecipe(bookStack, new ItemStack(Items.BOOK), new ItemStack(ModItems.vampireFang));
        GameRegistry.addShapelessRecipe(bookStack, new ItemStack(Items.BOOK), new ItemStack(ModItems.humanHeart));

        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            GuideBook.buildCategories();
        }
    }
}
