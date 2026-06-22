package floatingjacob.showoff;

import net.fabricmc.api.ClientModInitializer;

public class ShowoffClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[Showoff] loaded");
    }

}