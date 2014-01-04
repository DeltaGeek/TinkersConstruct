package tconstruct.library.crafting;

import java.util.*;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;
import tconstruct.TConstruct;
import tconstruct.library.tools.ToolCore;

/** Melting and hacking, churn and burn */
public class Smeltery
{
    public static Smeltery instance = new Smeltery();

    private final HashMap<List<Integer>, FluidStack> smeltingList = new HashMap<List<Integer>, FluidStack>();
    private final HashMap<List<Integer>, Integer> temperatureList = new HashMap<List<Integer>, Integer>();
    private final HashMap<List<Integer>, ItemStack> renderIndex = new HashMap<List<Integer>, ItemStack>();
    private final ArrayList<AlloyMix> alloys = new ArrayList<AlloyMix>();

    /** Adds mappings between an itemstack and an output liquid
     * Example: Smeltery.addMelting(Block.oreIron, 0, 600, new FluidStack(liquidMetalStill.blockID, TConstruct.ingotLiquidValue * 2, 0));
     * 
     * @param stack The itemstack to liquify
     * @param temperature How hot the block should be before liquifying. Max temp in the Smeltery is 800, other structures may vary
     * @param output The result of the process in liquid form
     */
    public static void addMelting (ItemStack stack, int temperature, FluidStack output)
    {
        addMelting(stack, stack.itemID, stack.getItemDamage(), temperature, output);
    }

    /** Adds mappings between a block and its liquid
     * Example: Smeltery.addMelting(Block.oreIron, 0, 600, new FluidStack(liquidMetalStill.blockID, TConstruct.ingotLiquidValue * 2, 0));
     * 
     * @param blockID The ID of the block to liquify and render
     * @param metadata The metadata of the block to liquify and render
     * @param temperature How hot the block should be before liquifying. Max temp in the Smeltery is 800, other structures may vary
     * @param output The result of the process in liquid form
     */
    public static void addMelting (Block block, int metadata, int temperature, FluidStack output)
    {
        addMelting(new ItemStack(block, 1, metadata), block.blockID, metadata, temperature, output);
    }

    /** Adds mappings between an input and its liquid.
     * Renders with the given input's block ID and metadata
     * Example: Smeltery.addMelting(Block.oreIron, 0, 600, new FluidStack(liquidMetalStill.blockID, TConstruct.ingotLiquidValue * 2, 0));
     * 
     * @param input The item to liquify
     * @param blockID The ID of the block to render
     * @param metadata The metadata of the block to render
     * @param temperature How hot the block should be before liquifying
     * @param liquid The result of the process
     */
    public static void addMelting (ItemStack input, int blockID, int metadata, int temperature, FluidStack liquid)
    {
        instance.smeltingList.put(Arrays.asList(input.itemID, input.getItemDamage()), liquid);
        instance.temperatureList.put(Arrays.asList(input.itemID, input.getItemDamage()), temperature);
        instance.renderIndex.put(Arrays.asList(input.itemID, input.getItemDamage()), new ItemStack(blockID, input.stackSize, metadata));
    }

    /** Adds an alloy mixing recipe.
     * Example: Smeltery.addAlloyMixing(new FluidStack(bronzeID, 2, 0), new FluidStack(copperID, 3, 0), new FluidStack(tinID, 1, 0));
     * The example mixes 3 copper with 1 tin to make 2 bronze
     * 
     * @param result The output of the combination of mixers. The quantity is used for amount of a successful mix
     * @param mixers the liquids to be mixed. Quantities are used as ratios
     */
    public static void addAlloyMixing (FluidStack result, FluidStack... mixers)
    {
        ArrayList inputs = new ArrayList();
        for (FluidStack liquid : mixers)
            inputs.add(liquid);

        instance.alloys.add(new AlloyMix(result, inputs));
    }

    /**
     * Used to get the resulting temperature from a source ItemStack
     * @param item The Source ItemStack
     * @return The result temperature
     */
    public static Integer getLiquifyTemperature (ItemStack item)
    {
        if (item == null)
            return 20;

        if(item.getItem() instanceof ToolCore)
        {
            ToolCore tool = (ToolCore) item.getItem();

            ItemStack headStack = tool.getHeadItemStack(item);
            ItemStack handleStack = tool.getHandleItemStack(item);
            ItemStack accessoryStack = tool.getAccessoryItemStack(item);
            ItemStack extraStack = tool.getExtraItemStack(item);
            ItemStack[] stacks = new ItemStack[]{headStack,handleStack,accessoryStack,extraStack};

            int maxTemp = 20;
            for(ItemStack stack : stacks)
            {
                if(stack != null)
                {
                    int partTemp = getLiquifyTemperature(stack);
                    maxTemp = Math.max(maxTemp, partTemp);
                }
            }

            return maxTemp;
        }

        Integer temp = instance.temperatureList.get(Arrays.asList(item.itemID, item.getItemDamage()));
        if (temp == null)
            return 20;
        else
            return temp;
    }

    /**
     * Used to get the resulting temperature from a source Block
     * @param item The Source ItemStack
     * @return The result ItemStack
     */
    public static Integer getLiquifyTemperature (int blockID, int metadata)
    {
        return instance.temperatureList.get(Arrays.asList(blockID, metadata));
    }

    /**
     * Used to get the resulting ItemStack from a source ItemStack
     * @param item The Source ItemStack
     * @return The result ItemStack
     */
    public static FluidStack[] getSmelteryResult (ItemStack item)
    {
        if (item == null)
            return null;

        if(item.getItem() instanceof ToolCore)
        {
            ToolCore tool = (ToolCore) item.getItem();

            Map<FluidStack, FluidStack> results = new HashMap<FluidStack, FluidStack>();
            FluidStack[] headResult = getSmelteryResult(tool.getHeadItemStack(item));
            FluidStack[] handleResult = getSmelteryResult(tool.getHandleItemStack(item));
            FluidStack[] accessoryResult = getSmelteryResult(tool.getAccessoryItemStack(item));
            FluidStack[] extraResult = getSmelteryResult(tool.getExtraItemStack(item));

            NBTTagCompound infiToolTags = item.getTagCompound().getCompoundTag("InfiTool");

            final int damage = infiToolTags.getInteger("Damage");
            final int maxDurability = infiToolTags.getInteger("TotalDurability");
            final int availableDurability = maxDurability - damage;
            final double percent = availableDurability / (double) maxDurability;

            if(headResult != null && headResult[0] != null)
            {
                results.put(headResult[0], headResult[0]);
            }
            if(handleResult != null && handleResult[0] != null)
            {
                putOrUpdateFluidStack(results, handleResult[0]);
            }
            if(accessoryResult != null && accessoryResult[0] != null)
            {
                putOrUpdateFluidStack(results, accessoryResult[0]);
            }
            if(extraResult != null && extraResult[0] != null)
            {
                putOrUpdateFluidStack(results, extraResult[0]);
            }

            for(FluidStack stack : results.values())
                stack.amount = damagePercentInNuggets(stack.amount, percent);

            return results.values().toArray(new FluidStack[0]);
        }

        FluidStack stack = instance.smeltingList.get(Arrays.asList(item.itemID, item.getItemDamage()));
        if (stack == null)
            return null;
        return new FluidStack[] {stack.copy()};
    }

    /**
     * Adds a FluidStack F to the provided Map<FluidStack, FluidStack> if it does not already exist in the Map
     * Otherwise, adds F.amount to the existing FluidStack
     * @param results
     * @param key
     */
    private static void putOrUpdateFluidStack(Map<FluidStack, FluidStack> results, FluidStack key) {
        if(results.containsKey(key))
            results.get(key).amount += key.amount;
        else
            results.put(key, key);
    }

    /**
     * Returns a fluid amount multiplied by a percentage then rounded down to the nearest nugget fluid value
     * @param amount The base amount of fluid
     * @param percent The percentage of fluid we want to keep
     * @return amount * percent floored to the nearest nugget fluid value
     */
    private static int damagePercentInNuggets(int amount, double percent)
    {
        int amountInNuggets = amount / TConstruct.nuggetLiquidValue;
        int percentInNuggets = (int) (amountInNuggets * percent);
        int result = percentInNuggets * TConstruct.nuggetLiquidValue;

        return result;
    }

    /**
     * Used to get the resulting ItemStack from a source Block
     * @param item The Source ItemStack
     * @return The result ItemStack
     */
    public static FluidStack getSmelteryResult (int blockID, int metadata)
    {
        FluidStack stack = instance.smeltingList.get(Arrays.asList(blockID, metadata));
        if (stack == null)
            return null;
        return stack.copy();
    }

    public static ItemStack getRenderIndex (ItemStack input)
    {
        return instance.renderIndex.get(Arrays.asList(input.itemID, input.getItemDamage()));
    }

    public static ArrayList mixMetals (ArrayList<FluidStack> moltenMetal)
    {
        ArrayList liquids = new ArrayList();
        for (AlloyMix alloy : instance.alloys)
        {
            FluidStack liquid = alloy.mix(moltenMetal);
            if (liquid != null)
                liquids.add(liquid);
        }
        return liquids;
    }

    public static HashMap<List<Integer>, FluidStack> getSmeltingList ()
    {
        return instance.smeltingList;
    }

    public static HashMap<List<Integer>, Integer> getTemperatureList ()
    {
        return instance.temperatureList;
    }

    public static HashMap<List<Integer>, ItemStack> getRenderIndex ()
    {
        return instance.renderIndex;
    }

    public static ArrayList<AlloyMix> getAlloyList ()
    {
        return instance.alloys;
    }

    /**
     * Adds a mapping between FluidType and ItemStack
     * 
     * @author samtrion
     * 
     * @param type Type of Fluid
     * @param input The item to liquify
     * @param temperatureDifference Difference between FluidType BaseTemperature
     * @param fluidAmount Amount of Fluid
     */
    public static void addMelting (FluidType type, ItemStack input, int temperatureDifference, int fluidAmount)
    {
        int temp = type.baseTemperature + temperatureDifference;
        if (temp <= 20)
            temp = type.baseTemperature;

        addMelting(input, type.renderBlockID, type.renderMeta, type.baseTemperature + temperatureDifference, new FluidStack(type.fluid, fluidAmount));
    }

    /**
     * Adds all Items to the Smeltery based on the oreDictionary Name
     * 
     * @author samtrion
     * 
     * @param oreName oreDictionary name e.g. oreIron
     * @param type Type of Fluid
     * @param temperatureDifference Difference between FluidType BaseTemperature
     * @param fluidAmount Amount of Fluid
     */
    public static void addDictionaryMelting (String oreName, FluidType type, int temperatureDifference, int fluidAmount)
    {
        for (ItemStack is : OreDictionary.getOres(oreName))
            addMelting(type, is, temperatureDifference, fluidAmount);
    }
}
