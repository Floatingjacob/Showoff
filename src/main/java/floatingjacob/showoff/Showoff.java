package floatingjacob.showoff;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.message.v1.ServerMessageDecoratorEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public class Showoff implements ModInitializer {

    private static final String ITEM_MARKER = " ";
    private static final String ICON_SPACE = "   "; // Gives 3 spaces of room for the 16px client icon

    @Override
    public void onInitialize() {
        System.out.println("[Showoff] Loaded");

        ServerMessageDecoratorEvent.EVENT.register(ServerMessageDecoratorEvent.CONTENT_PHASE, (sender, message) -> {
            if (sender == null) return message;

            String msg = message.getString();
            ItemStack stack = sender.getMainHandItem();

            if (!msg.contains("[i]") || stack.isEmpty()) {
                return message;
            }

            return buildMessage(msg, stack);
        });
    }

    private Component buildMessage(String original, ItemStack stack) {
        MutableComponent finalMessage = Component.empty();

        String[] split = original.split("\\[i\\]", 2);
        String before = split.length > 0 ? split[0] : "";
        String after = split.length > 1 ? split[1] : "";

        if (!before.isEmpty()) {
            finalMessage.append(Component.literal(before));
        }

        // 1. The explicit hidden marker holding the HoverEvent that your Mixin looks for
        HoverEvent hover = new HoverEvent.ShowItem(
                ItemStackTemplate.fromNonEmptyStack(stack)
        );

        MutableComponent hiddenMarker = Component.literal(ITEM_MARKER);
        hiddenMarker.setStyle(
                Style.EMPTY
                        .withColor(ChatFormatting.WHITE)
                        .withHoverEvent(hover)
        );
        finalMessage.append(hiddenMarker);

        // 2. Spacing padding for the icon
        finalMessage.append(Component.literal(ICON_SPACE));

        if (!after.isEmpty()) {
            finalMessage.append(Component.literal(after));
        }

        return finalMessage;
    }
}