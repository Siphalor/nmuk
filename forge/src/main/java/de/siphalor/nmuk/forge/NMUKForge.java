package de.siphalor.nmuk.forge;

import de.siphalor.nmuk.NMUK;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.Level;

@Mod("nmuk")
public class NMUKForge {
    public NMUKForge() {
        NMUK.log(Level.INFO, "NMUK Forge loaded");
    }
}
