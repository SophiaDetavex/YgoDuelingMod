package de.cas_ual_ty.ydm.duel.screen;

import java.util.List;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import de.cas_ual_ty.ydm.duel.screen.DuelingDuelScreen.InteractionWidget;
import de.cas_ual_ty.ydm.duelmanager.DuelManager;
import de.cas_ual_ty.ydm.duelmanager.playfield.DuelCard;
import de.cas_ual_ty.ydm.duelmanager.playfield.Zone;
import de.cas_ual_ty.ydm.duelmanager.playfield.ZoneInteraction;
import de.cas_ual_ty.ydm.duelmanager.playfield.ZoneOwner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class ZoneWidget extends Button
{
    public final Zone zone;
    public final IDuelScreenContext context;
    public boolean isFlipped;
    public DuelCard hoverCard;
    
    public ZoneWidget(Zone zone, IDuelScreenContext context, int width, int height, ITextComponent title, Consumer<ZoneWidget> onPress, ITooltip onTooltip)
    {
        super(0, 0, width, height, title, (w) -> onPress.accept((ZoneWidget)w), onTooltip);
        this.zone = zone;
        this.context = context;
        this.shift();
        this.hoverCard = null;
    }
    
    protected void shift()
    {
        this.x -= this.width / 2;
        this.y -= this.height / 2;
    }
    
    protected void unshift()
    {
        this.x += this.width / 2;
        this.y += this.height / 2;
    }
    
    public ZoneWidget flip(int guiWidth, int guiHeight)
    {
        guiWidth /= 2;
        guiHeight /= 2;
        
        this.unshift();
        
        this.x -= guiWidth;
        this.y -= guiHeight;
        
        this.x = -this.x;
        this.y = -this.y;
        
        this.x += guiWidth;
        this.y += guiHeight;
        
        this.shift();
        
        this.isFlipped = !this.isFlipped;
        
        return this;
    }
    
    public ZoneWidget setPositionRelative(int x, int y, int guiWidth, int guiHeight)
    {
        this.x = x + guiWidth / 2;
        this.y = y + guiHeight / 2;
        
        this.shift();
        
        this.isFlipped = false;
        
        return this;
    }
    
    public ZoneWidget setPositionRelativeFlipped(int x, int y, int guiWidth, int guiHeight)
    {
        this.x = guiWidth / 2 - x;
        this.y = guiHeight / 2 - y;
        
        this.shift();
        
        this.isFlipped = true;
        
        return this;
    }
    
    @Override
    public void renderButton(MatrixStack ms, int mouseX, int mouseY, float partialTicks)
    {
        Minecraft minecraft = Minecraft.getInstance();
        FontRenderer fontrenderer = minecraft.fontRenderer;
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        
        if(this.context.getClickedZone() == this.zone && this.context.getClickedDuelCard() == null)
        {
            DuelingDuelScreen.renderSelectedRect(ms, this.x, this.y, this.width, this.height);
        }
        
        this.hoverCard = this.renderCards(ms, mouseX, mouseY);
        //            ClientProxy.drawLineRect(ms, this.x, this.y, this.width, this.height, 1, 1, 0, 0, 1);
        
        int j = this.getFGColor();
        AbstractGui.drawCenteredString(ms, fontrenderer, this.getMessage(), this.x + this.width / 2, this.y + (this.height - 8) / 2, j | MathHelper.ceil(this.alpha * 255.0F) << 24);
        
        if(this.active)
        {
            if(this.isHovered())
            {
                if(this.zone.getCardsAmount() == 0)
                {
                    DuelingDuelScreen.renderHoverRect(ms, this.x, this.y, this.width, this.height);
                }
                
                this.renderToolTip(ms, mouseX, mouseY);
            }
        }
        else
        {
            DuelingDuelScreen.renderDisabledRect(ms, this.x, this.y, this.width, this.height);
        }
    }
    
    @Nullable
    public DuelCard renderCards(MatrixStack ms, int mouseX, int mouseY)
    {
        if(this.zone.getCardsAmount() <= 0)
        {
            return null;
        }
        
        boolean isOwner = this.zone.getOwner() == this.context.getZoneOwner();
        DuelCard c = this.zone.getTopCard();
        
        if(c != null)
        {
            if(this.drawCard(ms, c, this.x, this.y, this.width, this.height, mouseX, mouseY, this.x, this.y, this.width, this.height))
            {
                if(c.getCardPosition().isFaceUp || (isOwner && !this.zone.getType().getIsSecret()))
                {
                    this.context.renderCardInfo(ms, c);
                }
                
                if(this.active)
                {
                    DuelingDuelScreen.renderHoverRect(ms, this.x, this.y, this.width, this.height);
                    return c;
                }
            }
        }
        
        if(this.context.getClickedZone() == this.zone)
        {
            DuelingDuelScreen.renderSelectedRect(ms, this.x, this.y, this.width, this.height);
        }
        
        return null;
        
        /*
        int cardsWidth = DuelingDuelScreen.CARDS_WIDTH * this.height / DuelingDuelScreen.CARDS_HEIGHT;
        int cardsHeight = this.height;
        int cardsTextureSize = cardsHeight;
        
        DuelCard hoveredCard = null;
        int hoverX = this.x;
        int hoverY = this.y;
        int hoverWidth = this.width;
        int hoverHeight = this.height;
        
        boolean isOwner = this.zone.getOwner() == this.context.getZoneOwner();
        boolean isOpponentView = this.zone.getOwner() != this.context.getView();
        
        if(this.zone.type.getRenderCardsSpread())
        {
            DuelCard c = null;
            hoverWidth = cardsWidth;
            
            int totalW = this.zone.getCardsAmount() * cardsWidth;
            
            if(totalW <= this.width)
            {
                int x = this.x + (this.width - totalW) / 2;
                int renderX = x - (cardsTextureSize - cardsWidth) / 2; // Cards are 24x32, but the textures are still 32x32, so we must account for that
                int y = this.y;
                
                for(short i = 0; i < this.zone.getCardsAmount(); ++i)
                {
                    if(!isOpponentView)
                    {
                        c = this.zone.getCard(i);
                    }
                    else
                    {
                        c = this.zone.getCard((short)(this.zone.getCardsAmount() - i - 1));
                    }
                    
                    if(this.drawCard(ms, c, renderX, y, cardsTextureSize, cardsTextureSize, mouseX, mouseY, x, y, cardsWidth, cardsHeight))
                    {
                        hoveredCard = c;
                        hoverX = x;
                        hoverY = y;
                    }
                    
                    x += cardsWidth;
                    renderX += cardsWidth;
                }
            }
            else
            {
                int x = this.x;
                int y = this.y;
                
                int x1;
                int renderX1;
                
                float margin = cardsWidth - (this.zone.getCardsAmount() * cardsWidth - this.width) / (float)(this.zone.getCardsAmount() - 1);
                
                boolean renderLeftToRight = !this.zone.type.getRenderCardsReversed() && isOpponentView; // wenn true
                boolean renderFrontToBack = this.zone.type.getRenderCardsReversed() && isOpponentView; // flip
                
                if(!renderLeftToRight)
                {
                    margin *= -1F;
                    x += this.width - cardsWidth;
                }
                
                int renderX = x - (cardsTextureSize - cardsWidth) / 2; // Cards are 24x32, but the textures are still 32x32, so we must account for that
                
                for(short i = 0; i < this.zone.getCardsAmount(); ++i)
                {
                    if(renderFrontToBack)
                    {
                        c = this.zone.getCard(i);
                    }
                    else
                    {
                        c = this.zone.getCard((short)(this.zone.getCardsAmount() - i - 1));
                    }
                    
                    x1 = x + (int)(i * margin);
                    renderX1 = renderX + (int)(i * margin);
                    
                    // if this is the top rendered card
                    // and the card is sideways
                    // adjust the hover rect
                    // and also render it centered again
                    if(c == this.zone.getTopCardSafely() && !c.getCardPosition().isStraight)
                    {
                        int renX = this.x + (this.width - cardsTextureSize) / 2;
                        int renY = this.y + (this.height - cardsTextureSize) / 2;
                        
                        int offset = (cardsHeight - cardsWidth);
                        int hovX = renX;
                        int hovY = renY + offset / 2;
                        int hovW = cardsHeight;
                        int hovH = cardsWidth;
                        
                        if(this.drawCard(ms, c, renX, renY, cardsTextureSize, cardsTextureSize, mouseX, mouseY, hovX, hovY, hovW, hovH))
                        {
                            hoveredCard = c;
                            hoverX = hovX;
                            hoverY = hovY;
                            hoverWidth = hovW;
                            hoverHeight = hovH;
                        }
                    }
                    else if(this.drawCard(ms, c, renderX1, y, cardsTextureSize, cardsTextureSize, mouseX, mouseY, x1, y, cardsWidth, cardsHeight))
                    {
                        hoveredCard = c;
                        hoverX = x1;
                        hoverY = y;
                    }
                }
            }
        }
        else
        {
            DuelCard c = this.zone.getTopCardSafely();
            
            if(c != null && this.drawCard(ms, c, this.x, this.y, this.width, this.height, mouseX, mouseY, this.x, this.y, this.width, this.height))
            {
                hoveredCard = c;
            }
            
            // #drawCard only draws the top card here
            // so only if the top card is selected, this zone is marked
            // so we gotta mark the zone in case this zone is selected
            if(this.context.getClickedDuelCard() != c && this.context.getClickedZone() == this.zone)
            {
                DuelingDuelScreen.renderSelectedRect(ms, hoverX, hoverY, hoverWidth, hoverHeight);
            }
        }
        
        if(hoveredCard != null)
        {
            if(hoveredCard.getCardPosition().isFaceUp || (isOwner && !this.zone.getType().getIsSecret()))
            {
                this.context.renderCardInfo(ms, hoveredCard);
            }
            
            if(this.active)
            {
                DuelingDuelScreen.renderHoverRect(ms, hoverX, hoverY, hoverWidth, hoverHeight);
            }
        }
        
        if(!this.active)
        {
            return null;
        }
        else
        {
            return hoveredCard;
        }
        */
    }
    
    protected boolean drawCard(MatrixStack ms, DuelCard duelCard, int renderX, int renderY, int renderWidth, int renderHeight, int mouseX, int mouseY, int cardsWidth, int cardsHeight)
    {
        int offset = cardsHeight - cardsWidth;
        
        int hoverX = renderX;
        int hoverY = renderY;
        int hoverWidth;
        int hoverHeight;
        
        if(duelCard.getCardPosition().isStraight)
        {
            hoverX += offset;
            hoverWidth = cardsWidth;
            hoverHeight = cardsHeight;
        }
        else
        {
            hoverY += offset;
            hoverWidth = cardsHeight;
            hoverHeight = cardsWidth;
        }
        
        return this.drawCard(ms, duelCard, renderX, renderY, renderWidth, renderHeight, mouseX, mouseY, hoverX, hoverY, hoverWidth, hoverHeight);
    }
    
    protected boolean drawCard(MatrixStack ms, DuelCard duelCard, int renderX, int renderY, int renderWidth, int renderHeight, int mouseX, int mouseY, int hoverX, int hoverY, int hoverWidth, int hoverHeight)
    {
        boolean isOwner = this.zone.getOwner() == this.context.getZoneOwner();
        boolean faceUp = this.zone.getType().getShowFaceDownCardsToOwner() && isOwner;
        boolean isOpponentView = this.zone.getOwner() != this.context.getView();
        
        if(duelCard == this.context.getClickedDuelCard())
        {
            DuelingDuelScreen.renderSelectedRect(ms, hoverX, hoverY, hoverWidth, hoverHeight);
        }
        
        if(!isOpponentView)
        {
            DuelingDuelScreen.renderCardCentered(ms, renderX, renderY, renderWidth, renderHeight, duelCard, faceUp);
        }
        else
        {
            DuelingDuelScreen.renderCardReversedCentered(ms, renderX, renderY, renderWidth, renderHeight, duelCard, faceUp);
        }
        
        if(this.isHovered() && mouseX >= hoverX && mouseX < hoverX + hoverWidth && mouseY >= hoverY && mouseY < hoverY + hoverHeight)
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    public void addInteractionWidgets(ZoneOwner player, Zone interactor, DuelCard interactorCard, DuelManager m, List<InteractionWidget> list, Consumer<InteractionWidget> onPress, ITooltip onTooltip)
    {
        List<ZoneInteraction> interactions = m.getActionsFor(player, interactor, interactorCard, this.zone);
        
        if(interactions.size() == 0)
        {
            return;
        }
        
        if(interactions.size() == 1)
        {
            list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
        }
        else if(interactions.size() == 2)
        {
            if(this.width <= this.height)
            {
                // Split them horizontally (1 action on top, 1 on bottom)
                list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(1), this.context, this.x, this.y + this.height / 2, this.width, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
            }
            else
            {
                // Split them vertically (1 left, 1 right)
                list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width / 2, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(1), this.context, this.x + this.width / 2, this.y, this.width / 2, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
            }
        }
        else if(interactions.size() == 3)
        {
            if(this.width == this.height)
            {
                // 1 on top half, 1 bottom left, 1 bottom right
                list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(1), this.context, this.x, this.y + this.height / 2, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(2), this.context, this.x + this.width / 2, this.y + this.height / 2, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
            }
            else if(this.width < this.height)
            {
                // Horizontally split
                list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width, this.height / 3, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(1), this.context, this.x, this.y + this.height / 3, this.width, this.height / 3, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(2), this.context, this.x, this.y + this.height * 2 / 3, this.width, this.height / 3, StringTextComponent.EMPTY, onPress, onTooltip));
            }
            else //if(this.width > this.height)
            {
                // Vertically split
                list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width / 3, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(1), this.context, this.x + this.width / 3, this.y, this.width / 3, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                list.add(new InteractionWidget(interactions.get(2), this.context, this.x + this.width * 2 / 3, this.y, this.width / 3, this.height, StringTextComponent.EMPTY, onPress, onTooltip));
            }
        }
        else if(interactions.size() == 4 && this.width == this.height)
        {
            // 1 on top left, 1 top right, 1 bottom left, 1 bottom right
            list.add(new InteractionWidget(interactions.get(0), this.context, this.x, this.y, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
            list.add(new InteractionWidget(interactions.get(1), this.context, this.x + this.width / 2, this.y, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
            list.add(new InteractionWidget(interactions.get(2), this.context, this.x, this.y + this.height / 2, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
            list.add(new InteractionWidget(interactions.get(3), this.context, this.x + this.width / 2, this.y + this.height / 2, this.width / 2, this.height / 2, StringTextComponent.EMPTY, onPress, onTooltip));
        }
        else
        {
            if(this.width < this.height)
            {
                // Horizontally split
                for(int i = 0; i < interactions.size(); ++i)
                {
                    list.add(new InteractionWidget(interactions.get(i), this.context, this.x, this.y + this.height * i / interactions.size(), this.width, this.height / interactions.size(), StringTextComponent.EMPTY, onPress, onTooltip));
                }
            }
            else //if(this.width > this.height)
            {
                // Vertically split
                for(int i = 0; i < interactions.size(); ++i)
                {
                    list.add(new InteractionWidget(interactions.get(i), this.context, this.x + this.width * i / interactions.size(), this.y, this.width / interactions.size(), this.height, StringTextComponent.EMPTY, onPress, onTooltip));
                }
            }
        }
    }
}