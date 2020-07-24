package de.cas_ual_ty.ydm;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.cas_ual_ty.ydm.binder.BinderCardInventoryManager;
import de.cas_ual_ty.ydm.util.YdmIOUtil;
import de.cas_ual_ty.ydm.util.YdmUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

@Mod(YDM.MOD_ID)
public class YDM
{
    public static final String MOD_ID = "ydm";
    public static final String PROTOCOL_VERSION = "1";
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static YDM instance;
    public static ISidedProxy proxy;
    public static YdmItemGroup ydmItemGroup;
    public static boolean itemsUseCardImagesActive;
    
    public static File mainFolder;
    public static File cardsFolder;
    public static File setsFolder;
    public static File distributionsFolder;
    public static File imagesParentFolder;
    public static File rawImagesFolder;
    public static File cardInfoImagesFolder;
    public static File cardItemImagesFolder;
    public static File bindersFolder;
    
    public static int activeInfoImageSize;
    public static int activeItemImageSize;
    public static boolean keepCachedImages;
    public static boolean itemsUseCardImages;
    public static String dbSourceUrl;
    
    public static SimpleChannel channel;
    
    @CapabilityInject(BinderCardInventoryManager.class)
    public static Capability<BinderCardInventoryManager> BINDER_INVENTORY_CAPABILITY = null;
    
    public YDM()
    {
        YDM.instance = this;
        YDM.proxy = DistExecutor.runForDist(
            () -> de.cas_ual_ty.ydm.client.ClientProxy::new,
            () -> () -> new ISidedProxy()
            {
            });
        YDM.ydmItemGroup = new YdmItemGroup(YDM.MOD_ID);
        YDM.itemsUseCardImagesActive = false;
        
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        bus.addListener(this::init);
        YDM.proxy.registerModEventListeners(bus);
        
        bus = MinecraftForge.EVENT_BUS;
        bus.addListener(this::attachItemStackCapabilities);
        YDM.proxy.registerForgeEventListeners(bus);
        
        YDM.proxy.preInit();
    }
    
    private void init(FMLCommonSetupEvent event)
    {
        YDM.channel = NetworkRegistry.newSimpleChannel(new ResourceLocation(YDM.MOD_ID, "main"),
            () -> YDM.PROTOCOL_VERSION,
            YDM.PROTOCOL_VERSION::equals,
            YDM.PROTOCOL_VERSION::equals);
        
        CapabilityManager.INSTANCE.<BinderCardInventoryManager>register(BinderCardInventoryManager.class, new BinderCardInventoryManager.Storage(), BinderCardInventoryManager::new);
        
        this.initFiles();
        
        YDM.proxy.init();
    }
    
    private void initFiles()
    {
        YDM.mainFolder = new File("ydm_db");
        
        if(!YDM.mainFolder.exists())
        {
            try
            {
                Database.downloadDatabase();
                YDM.mainFolder = new File("ydm_db");
            }
            catch (IOException e)
            {
                YDM.log("Failed downloading cards database.");
                e.printStackTrace();
                return;
            }
        }
        
        YDM.cardsFolder = new File(YDM.mainFolder, "cards");
        YDM.setsFolder = new File(YDM.mainFolder, "sets");
        YDM.distributionsFolder = new File(YDM.mainFolder, "distributions");
        
        YDM.imagesParentFolder = new File("ydm_db_images");
        YDM.rawImagesFolder = new File(YDM.imagesParentFolder, "cards_raw");
        
        // change this depending on resolution (64/128/256) and anime (yes/no) settings
        YDM.cardInfoImagesFolder = new File(YDM.imagesParentFolder, "cards_" + YDM.activeInfoImageSize);
        YDM.cardItemImagesFolder = new File(YDM.imagesParentFolder, "cards_" + YDM.activeItemImageSize);
        
        YDM.bindersFolder = new File("ydm_binders");
        
        YdmIOUtil.createDirIfNonExistant(YDM.imagesParentFolder);
        YdmIOUtil.createDirIfNonExistant(YDM.rawImagesFolder);
        YdmIOUtil.createDirIfNonExistant(YDM.cardInfoImagesFolder);
        YdmIOUtil.createDirIfNonExistant(YDM.cardItemImagesFolder);
        YdmIOUtil.createDirIfNonExistant(YDM.bindersFolder);
        
        YdmIOUtil.setAgent();
        Database.readFiles();
    }
    
    private void attachItemStackCapabilities(AttachCapabilitiesEvent<ItemStack> event)
    {
        if(event.getObject() instanceof ItemStack && event.getObject().getItem() == YdmItems.CARD_BINDER)
        {
            final LazyOptional<BinderCardInventoryManager> instance = LazyOptional.of(BinderCardInventoryManager::new);
            final ICapabilitySerializable<INBT> provider = new ICapabilitySerializable<INBT>()
            {
                @Override
                public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side)
                {
                    return YDM.BINDER_INVENTORY_CAPABILITY.orEmpty(cap, instance);
                }
                
                @Override
                public INBT serializeNBT()
                {
                    return YDM.BINDER_INVENTORY_CAPABILITY.writeNBT(instance.orElseThrow(YdmUtil.throwNullCapabilityException()), null);
                }
                
                @Override
                public void deserializeNBT(INBT nbt)
                {
                    YDM.BINDER_INVENTORY_CAPABILITY.readNBT(instance.orElseThrow(YdmUtil.throwNullCapabilityException()), null, nbt);
                }
            };
            event.addCapability(new ResourceLocation(YDM.MOD_ID, "card_inventory_manager"), provider);
            event.addListener(instance::invalidate);
        }
    }
    
    public static void log(String s)
    {
        YDM.LOGGER.info("[" + YDM.MOD_ID + "] " + s);
    }
    
    public static void debug(String s)
    {
        YDM.LOGGER.debug(s);
    }
    
    public static void debug(Object s)
    {
        YDM.debug(s.toString());
    }
}
