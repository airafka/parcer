package local.parser.json;

import java.util.List;
import java.util.Map;

public record JsonPacket(
        int packetNumber,
        int itemsCount,
        List<Map<String, Object>> items
) {
}
