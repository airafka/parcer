package local.parser.api;

import local.parser.etran.EtranWaybillMapper;
import local.parser.excel.ExcelReader;
import local.parser.json.JsonPacketWriter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
public class ConvertController {
    private final ExcelReader excelReader;
    private final EtranWaybillMapper etranWaybillMapper;
    private final JsonPacketWriter jsonPacketWriter;

    public ConvertController() {
        this.excelReader = new ExcelReader();
        this.etranWaybillMapper = new EtranWaybillMapper();
        this.jsonPacketWriter = new JsonPacketWriter();
    }

    @PostMapping(value = "/api/convert", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ConvertResponse convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sheetName", required = false) String sheetName,
            @RequestParam(value = "packetSize", defaultValue = "100") int packetSize
    ) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Excel file is empty.");
        }
        if (packetSize <= 0) {
            throw new IllegalArgumentException("Packet size must be greater than zero.");
        }

        Path tempFile = Files.createTempFile("excel-upload-", ".xlsx");
        try {
            file.transferTo(tempFile);
            String normalizedSheetName = normalize(sheetName);
            boolean isEtranTemplate = normalizedSheetName == null && excelReader.isEtranTemplate(tempFile);
            List<Map<String, Object>> rows = isEtranTemplate
                    ? etranWaybillMapper.map(excelReader.readEtranTemplate(tempFile))
                    : excelReader.read(tempFile, normalizedSheetName);
            if (isEtranTemplate) {
                List<PacketFile> packets = List.of(new PacketFile("waybill-packages.json", rows));
                return new ConvertResponse(file.getOriginalFilename(), rows.size(), packets.size(), packets);
            }

            List<PacketFile> packets = jsonPacketWriter.buildPackets(rows, packetSize).stream()
                    .map(packet -> new PacketFile(
                            "packet-%04d.json".formatted(packet.packetNumber()),
                            packet
                    ))
                    .toList();

            return new ConvertResponse(file.getOriginalFilename(), rows.size(), packets.size(), packets);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
