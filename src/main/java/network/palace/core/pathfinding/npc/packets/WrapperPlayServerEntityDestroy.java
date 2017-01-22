package network.palace.core.pathfinding.npc.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.google.common.primitives.Ints;

import java.util.List;

/**
 * @author Innectic
 * @since 1/22/2017
 */
public class WrapperPlayServerEntityDestroy extends AbstractPacket {
    public static final PacketType TYPE = PacketType.Play.Server.ENTITY_DESTROY;

    public WrapperPlayServerEntityDestroy() {
        super(new PacketContainer(TYPE), TYPE);
        getContainer().getModifier().writeDefaults();
    }

    public WrapperPlayServerEntityDestroy(PacketContainer container) {
        super(container, TYPE);
    }

    public List<Integer> getEntities() {
        return Ints.asList(getContainer().getIntegerArrays().read(0));
    }

    public void setEntities(int[] entities) {
        getContainer().getIntegerArrays().write(0, entities);
    }

    public void setEntities(List<Integer> entities) {
        setEntities(Ints.toArray(entities));
    }
}
