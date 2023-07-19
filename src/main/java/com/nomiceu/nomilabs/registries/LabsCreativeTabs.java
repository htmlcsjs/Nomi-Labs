package com.nomiceu.nomilabs.registries;

import com.nomiceu.nomilabs.LabsValues;
import net.minecraft.creativetab.CreativeTabs;

public class LabsCreativeTabs {
    public static CreativeTabs TAB_NOMI_LABS;

    public static void preInit() {
        TAB_NOMI_LABS = new BaseCreativeTab(LabsValues.LABS_MODID + ".main", () -> ModItems.HEART_OF_THE_UNIVERSE, true);
    }
}