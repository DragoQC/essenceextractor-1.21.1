package com.essenceextractor.essenceextractormod.fluid;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/**
 * Thin wrappers for NeoForge flowing/source fluid variants.
 */
public class ExperienceFluid {
    public static class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }
    }

    public static class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }
    }
}
