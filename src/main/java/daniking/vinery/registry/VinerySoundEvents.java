package daniking.vinery.registry;

import daniking.vinery.VineryIdentifier;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.LinkedHashMap;
import java.util.Map;

public class VinerySoundEvents {

    private static final Map<Identifier, SoundEvent> SOUND_EVENTS = new LinkedHashMap<>();

    public static final SoundEvent BLOCK_GRAPEVINE_POT_SQUEEZE = create("block.grapevine_pot.squeeze");
    public static final SoundEvent BLOCK_COOKING_POT_JUICE_BOILING = create("block.cooking_pot.juice_boiling");

    private static SoundEvent create(String name) {
        final Identifier id = new VineryIdentifier(name);
        final SoundEvent event = new SoundEvent(id);
        SOUND_EVENTS.put(id, event);
        return event;
    }

    public static void init() {
        SOUND_EVENTS.keySet().forEach(soundEvent -> Registry.register(Registry.SOUND_EVENT, soundEvent, SOUND_EVENTS.get(soundEvent)));
    }
}
