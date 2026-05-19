package local.parser.api;

import java.util.List;

public record ConvertResponse(
        String sourceFile,
        int rowsCount,
        int packetsCount,
        List<PacketFile> packets
) {
}
