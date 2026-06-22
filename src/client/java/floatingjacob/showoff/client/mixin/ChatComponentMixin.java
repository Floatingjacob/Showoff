package floatingjacob.showoff.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow public abstract int getLinesPerPage();
    @Shadow @Final private Minecraft minecraft;
    @Shadow protected abstract double getScale();
    @Shadow protected abstract int getWidth();

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
            at = @At("TAIL")
    )
    private void renderIcons(
            GuiGraphicsExtractor graphics,
            Font font,
            int ticks,
            int mouseX,
            int mouseY,
            ChatComponent.DisplayMode displayMode,
            boolean changeCursor,
            CallbackInfo ci
    ) {
        int totalLines = this.trimmedMessages.size();
        if (totalLines == 0) return;

        int perPage = this.getLinesPerPage();
        float scale = (float) this.getScale();
        int screenHeight = graphics.guiHeight();

        int chatBottom = Mth.floor((float) (screenHeight - 40) / scale);
        int messageHeight = 9;
        double chatLineSpacing = (Double) this.minecraft.options.chatLineSpacing().get();
        int entryHeight = (int) ((double) messageHeight * (chatLineSpacing + 1.0D));
        int maxWidth = Mth.ceil((float) this.getWidth() / scale);

        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        graphics.pose().translate(4.0F, 0.0F);

        Set<GuiMessage> renderedMessages = new HashSet<>();
        List<IconRenderInfo> iconsToRender = new ArrayList<>();
        IconRenderInfo hoveredIcon = null;

        int startIndex = Math.min(totalLines - this.chatScrollbarPos, perPage) - 1;

        for (int i = startIndex; i >= 0; --i) {
            int lineIndex = i + this.chatScrollbarPos;
            if (lineIndex < 0 || lineIndex >= this.trimmedMessages.size()) continue;

            GuiMessage.Line trimmedLine = this.trimmedMessages.get(lineIndex);
            GuiMessage parentMessage = trimmedLine.parent();
            if (parentMessage == null) continue;

            if (renderedMessages.contains(parentMessage)) continue;

            ItemStack hoverItem = findHoverItem(parentMessage.content());
            if (hoverItem == null || hoverItem.isEmpty()) continue;

            int segmentStart = lineIndex;
            while (segmentStart > 0 && this.trimmedMessages.get(segmentStart - 1).parent() == parentMessage) {
                segmentStart--;
            }

            int segmentEnd = lineIndex;
            while (segmentEnd + 1 < this.trimmedMessages.size() && this.trimmedMessages.get(segmentEnd + 1).parent() == parentMessage) {
                segmentEnd++;
            }

            int wrappedIndex = segmentEnd - lineIndex;

            List<FormattedCharSequence> wrappedLines = parentMessage.splitLines(font, maxWidth);
            if (wrappedIndex < 0 || wrappedIndex >= wrappedLines.size()) continue;

            int markerX = findHiddenMarkerX(font, parentMessage.content());
            if (markerX < 0) continue;

            int lineStartX = 0;
            for (int line = 0; line < wrappedIndex; line++) {
                lineStartX += font.width(wrappedLines.get(line));
            }

            int lineWidth = font.width(wrappedLines.get(wrappedIndex));

            if (markerX < lineStartX || markerX > lineStartX + lineWidth) {
                continue;
            }

            int entryBottom = chatBottom - i * entryHeight;
            int y = entryBottom - messageHeight;

            int iconSize = 16;
            int iconX = markerX - lineStartX;
            int iconY = y - 5;

            if (displayMode == ChatComponent.DisplayMode.BACKGROUND && ticks - parentMessage.addedTime() >= 180)
                continue;
            
            int screenIconX = Mth.floor((iconX + 4.0f) * scale);
            int screenIconY = Mth.floor(iconY * scale);
            int screenIconSize = Mth.ceil(iconSize * scale);

            boolean isHovered = mouseX >= screenIconX
                    && mouseX < screenIconX + screenIconSize
                    && mouseY >= screenIconY
                    && mouseY < screenIconY + screenIconSize;

            IconRenderInfo info = new IconRenderInfo(hoverItem, iconX, iconY, displayMode);
            iconsToRender.add(info);

            if (isHovered) {
                hoveredIcon = info;
            }

            renderedMessages.add(parentMessage);
        }

        // Pass 1: Render all background (non-hovered) elements
        for (IconRenderInfo info : iconsToRender) {
            if (info == hoveredIcon) continue;
            drawIcon(graphics, font, info);
        }

        // Pass 2: Render the single hovered icon last so it stacks perfectly on top
        if (hoveredIcon != null) {
            drawIcon(graphics, font, hoveredIcon);
            graphics.setTooltipForNextFrame(font, hoveredIcon.stack(), mouseX, mouseY);
        }

        graphics.pose().popMatrix();
    }

    private void drawIcon(GuiGraphicsExtractor graphics, Font font, IconRenderInfo info) {
        graphics.item(info.stack(), info.x(), info.y());
        graphics.itemDecorations(font, info.stack(), info.x(), info.y());
    }

private int findHiddenMarkerX(Font font, Component component) {
    int x = 0;

    for (Component part : component.toFlatList()) {
        // If this exact text run holds our item hover event, we found our layout boundary!
        if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowItem) {
            // If it's the item name itself, we want the icon to render right after it.
            // If it's a spacer, it will render inside the gap.
            if (!part.getString().equals(" ")) {
                x += font.width(part.getVisualOrderText());
            }
            return x;
        }
        x += font.width(part.getVisualOrderText());
    }

    return -1;
}

private ItemStack findHoverItem(Component component) {
    // Check the root component first
    if (component.getStyle().getHoverEvent() instanceof HoverEvent.ShowItem showItem) {
        return showItem.item().create();
    }

    // Deep scanning using flat list to prevent nested child hierarchy skip issues
    for (Component part : component.toFlatList()) {
        if (part.getStyle().getHoverEvent() instanceof HoverEvent.ShowItem showItem) {
            return showItem.item().create();
        }
    }

    return null;
}

    // Lightweight record to hold lazy layout coordinates for the drawing execution passes
    private record IconRenderInfo(
            ItemStack stack,
            int x,
            int y,
            ChatComponent.DisplayMode displayMode
    ) {}
}