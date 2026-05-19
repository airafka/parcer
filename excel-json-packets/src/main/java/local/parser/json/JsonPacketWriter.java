package local.parser.json;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class JsonPacketWriter {
    private final ObjectMapper objectMapper;

    public JsonPacketWriter() {
        this.objectMapper = new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public int write(Path outputDirectory, List<Map<String, Object>> rows, int packetSize) throws IOException {
        Files.createDirectories(outputDirectory);

        List<JsonPacket> packets = buildPackets(rows, packetSize);
        for (JsonPacket packet : packets) {
            Path packetFile = outputDirectory.resolve("packet-%04d.json".formatted(packet.packetNumber()));
            objectMapper.writeValue(packetFile.toFile(), packet);
        }

        return packets.size();
    }

    public List<JsonPacket> buildPackets(List<Map<String, Object>> rows, int packetSize) {
        if (packetSize <= 0) {
            throw new IllegalArgumentException("Packet size must be greater than zero.");
        }

        java.util.ArrayList<JsonPacket> packets = new java.util.ArrayList<>();
        for (int fromIndex = 0; fromIndex < rows.size(); fromIndex += packetSize) {
            int toIndex = Math.min(fromIndex + packetSize, rows.size());
            List<Map<String, Object>> items = rows.subList(fromIndex, toIndex);
            packets.add(new JsonPacket(packets.size() + 1, items.size(), items));
        }
        return packets;
    }
}
