package floatingjacob.showoff.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow protected EditBox input;

    @Unique
    private static final String MARKER = "[i]";

    @Unique
    private static final String REPLACEMENT = "    "; // 4 whitespaces

    @Unique
    private static final int ICON_SIZE = 16;

    @Inject(method = "init", at = @At("TAIL"))
    private void showoff$addFormatter(CallbackInfo ci) {
        if (input == null) return;

        input.addFormatter((text, offset) -> {
            if (text == null || text.isEmpty()) {
                return null;
            }

            // only replaces the first occurrence
            int first = text.indexOf(MARKER);

            String rendered;
            if (first >= 0) {
                rendered =
                        text.substring(0, first)
                        + REPLACEMENT
                        + text.substring(first + MARKER.length());
            } else {
                rendered = text;
            }

            return Minecraft.getInstance()
                    .font
                    .split(FormattedText.of(rendered), Integer.MAX_VALUE)
                    .get(0);
        });
    }

    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
            at = @At("TAIL")
    )
    private void showoff$renderHeldItemPreview(
            GuiGraphicsExtractor graphics,
            int mouseX,
            int mouseY,
            float partialTick,
            CallbackInfo ci
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || input == null) return;

        String text = input.getValue();
        if (text == null) return;

        ItemStack held = minecraft.player.getMainHandItem();
        if (held.isEmpty()) return;

        Font font = minecraft.font;

        int baseX = input.getX();
        int baseY = input.getY() - 7;

        int idx = text.indexOf(MARKER);
        if (idx < 0) return;

        String before = text.substring(0, idx);

        int first = before.indexOf(MARKER);
        String renderedBefore;

        if (first >= 0) {
            renderedBefore =
                    before.substring(0, first)
                    + REPLACEMENT
                    + before.substring(first + MARKER.length());
        } else {
            renderedBefore = before;
        }

        int iconX = baseX + font.width(renderedBefore);
        int iconY = baseY;

        graphics.item(held, iconX, iconY);
        graphics.itemDecorations(font, held, iconX, iconY);

        if (mouseX >= iconX && mouseX < iconX + ICON_SIZE
                && mouseY >= iconY && mouseY < iconY + ICON_SIZE) {
            graphics.setTooltipForNextFrame(font, held, mouseX, mouseY);
        }
    }
}