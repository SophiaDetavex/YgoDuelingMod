package de.cas_ual_ty.ydm.card.network;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import de.cas_ual_ty.ydm.Database;
import de.cas_ual_ty.ydm.YDM;
import de.cas_ual_ty.ydm.binder.BinderContainer;
import de.cas_ual_ty.ydm.card.CardHolder;
import de.cas_ual_ty.ydm.card.Rarity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkEvent.Context;

public class CardBinderMessages
{
    public static void doForBinderContainer(PlayerEntity player, Consumer<BinderContainer> consumer)
    {
        if(player != null && player.openContainer instanceof BinderContainer)
        {
            consumer.accept((BinderContainer)player.openContainer);
        }
    }
    
    // client changes page, tells server
    public static class ChangePage
    {
        public boolean nextPage;
        
        public ChangePage(boolean nextPage)
        {
            this.nextPage = nextPage;
        }
        
        public ChangePage()
        {
        }
        
        public static void encode(ChangePage msg, PacketBuffer buf)
        {
            buf.writeBoolean(msg.nextPage);
        }
        
        public static ChangePage decode(PacketBuffer buf)
        {
            return new ChangePage(buf.readBoolean());
        }
        
        public static void handle(ChangePage msg, Supplier<NetworkEvent.Context> ctx)
        {
            Context context = ctx.get();
            context.enqueueWork(() ->
            {
                CardBinderMessages.doForBinderContainer(context.getSender(), (container) ->
                {
                    if(msg.nextPage)
                    {
                        container.nextPage();
                    }
                    else
                    {
                        container.prevPage();
                    }
                });
            });
            
            context.setPacketHandled(true);
        }
    }
    
    // update pages to client
    public static class UpdatePage
    {
        public int page;
        public int maxPage;
        
        public UpdatePage(int page, int maxPage)
        {
            this.page = page;
            this.maxPage = maxPage;
        }
        
        public UpdatePage()
        {
        }
        
        public static void encode(UpdatePage msg, PacketBuffer buf)
        {
            buf.writeInt(msg.page);
            buf.writeInt(msg.maxPage);
        }
        
        public static UpdatePage decode(PacketBuffer buf)
        {
            return new UpdatePage(buf.readInt(), buf.readInt());
        }
        
        public static void handle(UpdatePage msg, Supplier<NetworkEvent.Context> ctx)
        {
            Context context = ctx.get();
            context.enqueueWork(() ->
            {
                CardBinderMessages.doForBinderContainer(YDM.proxy.getClientPlayer(), (container) ->
                {
                    container.setClientPage(msg.page);
                    container.setClientMaxPage(msg.maxPage);
                });
            });
            
            context.setPacketHandled(true);
        }
    }
    
    // update cards list to client
    public static class UpdateList
    {
        public List<CardHolder> list;
        
        public UpdateList(List<CardHolder> list)
        {
            this.list = list;
        }
        
        public UpdateList()
        {
        }
        
        public static void encode(UpdateList msg, PacketBuffer buf)
        {
            buf.writeInt(msg.list.size());
            
            for(CardHolder cardHolder : msg.list)
            {
                buf.writeString(cardHolder.getCard().getSetId());
                buf.writeByte(cardHolder.getOverriddenImageIndex());
                buf.writeString(cardHolder.getOverriddenRarity() != null ? cardHolder.getOverriddenRarity().name : "", 0x100);
            }
        }
        
        public static UpdateList decode(PacketBuffer buf)
        {
            int size = buf.readInt();
            List<CardHolder> list = new ArrayList<>(size);
            
            for(int i = 0; i < size; ++i)
            {
                list.add(new CardHolder(Database.CARDS_LIST.get(buf.readString()), buf.readByte(), Rarity.fromString(buf.readString())));
            }
            
            return new UpdateList(list);
        }
        
        public static void handle(UpdateList msg, Supplier<NetworkEvent.Context> ctx)
        {
            Context context = ctx.get();
            context.enqueueWork(() ->
            {
                CardBinderMessages.doForBinderContainer(YDM.proxy.getClientPlayer(), (container) ->
                {
                    container.setClientList(msg.list);
                });
            });
            
            context.setPacketHandled(true);
        }
    }
    
    // client clicks index, tells server
    public static class IndexClicked
    {
        public int index;
        
        public IndexClicked(int index)
        {
            this.index = index;
        }
        
        public IndexClicked()
        {
        }
        
        public static void encode(IndexClicked msg, PacketBuffer buf)
        {
            buf.writeInt(msg.index);
        }
        
        public static IndexClicked decode(PacketBuffer buf)
        {
            return new IndexClicked(buf.readInt());
        }
        
        public static void handle(IndexClicked msg, Supplier<NetworkEvent.Context> ctx)
        {
            Context context = ctx.get();
            context.enqueueWork(() ->
            {
                CardBinderMessages.doForBinderContainer(context.getSender(), (container) ->
                {
                    container.indexClicked(msg.index);
                });
            });
            
            context.setPacketHandled(true);
        }
    }
}
