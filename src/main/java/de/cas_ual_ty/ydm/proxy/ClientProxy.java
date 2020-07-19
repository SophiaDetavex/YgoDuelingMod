package de.cas_ual_ty.ydm.proxy;

import de.cas_ual_ty.ydm.util.YdmResourcePackFinder;
import net.minecraft.client.Minecraft;

public class ClientProxy implements ISidedProxy
{
    @Override
    public void preInit()
    {
        Minecraft.getInstance().getResourcePackList().addPackFinder(new YdmResourcePackFinder());
    }
}
