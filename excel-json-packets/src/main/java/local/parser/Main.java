package local.parser;

import local.parser.excel.ExcelReader;
import local.parser.json.JsonPacketWriter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class Main {
    private static final int DEFAULT_PACKET_SIZE = 100;

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 4) {
            printUsage();
            System.exit(1);
        }

        Path inputFile = Path.of(args[0]);
        Path outputDirectory = Path.of(args[1]);
        String sheetName = args.length >= 3 && !args[2].isBlank() ? args[2] : null;
        int packetSize = args.length == 4 ? parsePacketSize(args[3]) : DEFAULT_PACKET_SIZE;

        try {
            ExcelReader excelReader = new ExcelReader();
            List<Map<String, Object>> rows = excelReader.read(inputFile, sheetName);

            JsonPacketWriter jsonPacketWriter = new JsonPacketWriter();
            int packetsCount = jsonPacketWriter.write(outputDirectory, rows, packetSize);

            System.out.printf("Done. Rows: %d. JSON packets: %d. Output: %s%n",
                    rows.size(), packetsCount, outputDirectory.toAbsolutePath());
        } catch (Exception exception) {
            System.err.println("Failed: " + exception.getMessage());
            System.exit(2);
        }
    }

    private static int parsePacketSize(String value) {
        try {
            int packetSize = Integer.parseInt(value);
            if (packetSize <= 0) {
                throw new IllegalArgumentException("Packet size must be greater than zero.");
            }
            return packetSize;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Packet size must be a number: " + value);
        }
    }

    private static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  java -jar excel-json-packets-1.0.0.jar <input.xlsx> <output-dir> [sheet-name] [packet-size]");
    }
}
