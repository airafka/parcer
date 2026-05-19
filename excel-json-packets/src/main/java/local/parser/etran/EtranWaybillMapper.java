package local.parser.etran;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EtranWaybillMapper {
    public List<Map<String, Object>> map(List<Map<String, Object>> etranRows) {
        Map<String, List<Map<String, Object>>> groups = new LinkedHashMap<>();
        for (Map<String, Object> etranRow : etranRows) {
            groups.computeIfAbsent(waybillGroupKey(etranRow), ignored -> new ArrayList<>()).add(etranRow);
        }

        List<Map<String, Object>> packages = new ArrayList<>();
        for (List<Map<String, Object>> rows : groups.values()) {
            packages.add(mapGroup(rows, hasMultipleContainers(rows)));
        }
        return packages;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapGroup(List<Map<String, Object>> etranRows, boolean hasMultipleContainers) {
        Map<String, Object> etranRow = etranRows.get(0);
        Map<String, Object> invoice = safeMap((Map<String, Object>) etranRow.get("Накладная"));
        Map<String, Object> summary = safeMap((Map<String, Object>) etranRow.get("Свод данных"));

        Map<String, Object> waybill = new LinkedHashMap<>();
        waybill.put("waybill_status", "В пути");
        waybill.put("waybill_type", null);
        waybill.put("waybill_number", value(invoice.get("Номер")));
        waybill.put("waybill_identifier", value(invoice.get("Идентификатор")));
        waybill.put("shipment_type", null);
        waybill.put("shipment_speed", null);
        waybill.put("form_type", null);
        waybill.put("delivery_deadline", "2026-04-16T00:00:00Z");
        waybill.put("waybill_created_at", "2026-03-06T11:29:57Z");
        waybill.put("submitted_at", "2026-03-30T02:55:27Z");
        waybill.put("approved_at", "2026-03-06T11:35:58Z");
        waybill.put("accepted_at", "2026-03-30T11:39:55Z");
        waybill.put("cargo_accepted_at", "2026-03-30T11:39:55Z");
        waybill.put("departure_at", "2026-03-30T11:57:50Z");
        waybill.put("departure_country", null);
        waybill.put("departure_station", null);
        waybill.put("departure_station_code", value(invoice.get("ЕСР станции отправления")));
        waybill.put("destination_country", null);
        waybill.put("destination_station", null);
        waybill.put("destination_station_code", value(invoice.get("ЕСР станции назначения")));
        waybill.put("shipper_name", null);
        waybill.put("shipper_address", null);
        waybill.put("shipper_tgnl", null);
        waybill.put("shipper_okpo", null);
        waybill.put("consignee_name", null);
        waybill.put("consignee_address", null);
        waybill.put("consignee_tgnl", null);
        waybill.put("consignee_okpo", null);
        waybill.put("payer", value(invoice.get("Наименование плательщика")));
        waybill.put("payer_code", value(invoice.get("Код плательщика")));
        waybill.put("payment_form", null);
        waybill.put("payment_place", null);
        waybill.put("wagon_ownership_type", null);
        waybill.put("planned_wagons", null);
        waybill.put("responsible_person", null);
        waybill.put("departure_route", null);
        waybill.put("destination_route", null);
        waybill.put("waybill_product", products(etranRows, hasMultipleContainers));
        waybill.put("waybill_railway_carriage", railwayCarriages(etranRows));
        waybill.put("waybill_container", containers(etranRows));
        waybill.put("waybill_zpu", zpu(etranRows));
        waybill.put("waybill_special_marks", List.of());
        waybill.put("waybill_tariff_marks", List.of());

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("waybill", waybill);
        root.put("Version", null);
        root.put("MessageId", null);
        return root;
    }

    @SuppressWarnings("unchecked")
    private String waybillGroupKey(Map<String, Object> etranRow) {
        Map<String, Object> invoice = safeMap((Map<String, Object>) etranRow.get("Накладная"));
        Map<String, Object> summary = safeMap((Map<String, Object>) etranRow.get("Свод данных"));
        return nullToEmpty(waybillNumber(invoice, summary));
    }

    @SuppressWarnings("unchecked")
    private boolean hasMultipleContainers(List<Map<String, Object>> etranRows) {
        Set<String> containers = new LinkedHashSet<>();
        for (Map<String, Object> etranRow : etranRows) {
            Map<String, Object> summary = safeMap((Map<String, Object>) etranRow.get("Свод данных"));
            String containerNumber = stringValue(summary.get("Номер КТК"));
            if (containerNumber != null && !containerNumber.isBlank()) {
                containers.add(containerNumber);
            }
        }
        return containers.size() > 1;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> railwayCarriages(List<Map<String, Object>> etranRows) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> etranRow : etranRows) {
            Map<String, Object> summary = safeMap((Map<String, Object>) etranRow.get("Свод данных"));
            String railwayNumber = stringValue(summary.get("Номер вагона"));
            result.putIfAbsent(nullToEmpty(railwayNumber), railwayCarriage(summary));
        }
        return new ArrayList<>(result.values());
    }

    private Map<String, Object> railwayCarriage(Map<String, Object> summary) {
        Map<String, Object> carriage = new LinkedHashMap<>();
        carriage.put("railway_number", value(summary.get("Номер вагона")));
        carriage.put("sort", mapCarriageSort(summary.get("Род вагона")));
        carriage.put("railway_lifting_capacity", divideByThousand(summary.get("Грузоподъемность вагона, кг")));
        carriage.put("railway_volume", null);
        carriage.put("axles_count", null);
        carriage.put("ownership", null);
        carriage.put("renter", null);
        carriage.put("previously_transported", null);
        carriage.put("railway_weight_gross", null);
        carriage.put("railway_weight_net", null);
        carriage.put("place_count", null);
        carriage.put("railway_length", null);
        carriage.put("model", null);
        carriage.put("date_of_next_repair", null);
        carriage.put("railway_weight", null);
        return carriage;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> products(List<Map<String, Object>> etranRows, boolean hasMultipleContainers) {
        List<Map<String, Object>> result = new ArrayList<>();
        int rowsLimit = hasMultipleContainers ? 1 : etranRows.size();
        for (int index = 0; index < rowsLimit; index++) {
            Map<String, Object> etranRow = etranRows.get(index);
            Map<String, Object> summary = safeMap((Map<String, Object>) etranRow.get("Свод данных"));
            result.add(product(summary));
        }
        return result;
    }

    private Map<String, Object> product(Map<String, Object> summary) {
        Map<String, Object> product = new LinkedHashMap<>();
        product.put("etsng_name", null);
        product.put("etsng_code", value(summary.get("Код груза ЕТСНГ")));
        product.put("gng_code", null);
        product.put("cargo_full_name", null);
        product.put("additional", null);
        product.put("packaging_type", null);
        product.put("cargo_volume", null);
        product.put("cargo_weight", value(summary.get("Масса брутто груза, кг")));
        product.put("weight_loaded", null);
        product.put("cargo_weight_net", null);
        product.put("cargo_weight_gross", null);
        product.put("length", null);
        product.put("width", null);
        product.put("height", null);
        product.put("marks_stamps_shipper", null);
        product.put("sign_danger", null);
        product.put("packages_count", null);
        product.put("places_count", null);
        product.put("is_dangerous", null);
        product.put("emergency_card_number", null);
        product.put("customs_declaration_number", null);
        product.put("customs_declaration_product_number", null);
        return product;
    }

    private Map<String, Object> container(Map<String, Object> summary) {
        Map<String, Object> container = new LinkedHashMap<>();
        container.put("carriage_number", value(summary.get("Номер вагона")));
        container.put("container_number", value(summary.get("Номер КТК")));
        container.put("lifting_capacity", divideByThousand(summary.get("Грузоподъемность КТК, кг")));
        container.put("weight", divideByHundred(summary.get("Масса тары, кг")));
        container.put("sending_request_number", null);
        container.put("supply_request_number", null);
        container.put("owner", null);
        container.put("container_length", mapContainerLength(summary.get("Тип КТК")));
        container.put("weight_gross", null);
        container.put("weight_net", value(summary.get("Масса нетто  КТК, кг")));
        return container;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> containers(List<Map<String, Object>> etranRows) {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();
        for (Map<String, Object> etranRow : etranRows) {
            Map<String, Object> summary = safeMap((Map<String, Object>) etranRow.get("Свод данных"));
            String containerNumber = stringValue(summary.get("Номер КТК"));
            result.putIfAbsent(nullToEmpty(containerNumber), container(summary));
        }
        return new ArrayList<>(result.values());
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> zpu(List<Map<String, Object>> etranRows) {
        Set<String> zpuNumbers = new LinkedHashSet<>();
        for (Map<String, Object> etranRow : etranRows) {
            Map<String, Object> summary = safeMap((Map<String, Object>) etranRow.get("Свод данных"));
            String rawZpuNumbers = stringValue(summary.get("Номера ЗПУ"));
            String containerNumber = stringValue(summary.get("Номер КТК"));
            if (rawZpuNumbers != null) {
                for (String zpu : splitZpuNumbers(rawZpuNumbers)) {
                    String trimmed = zpu.trim();
                    if (!trimmed.isBlank()) {
                        zpuNumbers.add(nullToEmpty(containerNumber) + "|" + trimmed);
                    }
                }
            }
        }

        if (zpuNumbers.isEmpty()) {
            return List.of(zpuItem(null, null));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (String zpuKey : zpuNumbers) {
            String[] parts = zpuKey.split("\\|", 2);
            result.add(zpuItem(emptyToNull(parts[0]), parts.length > 1 ? parts[1] : null));
        }
        return result;
    }

    private List<String> splitZpuNumbers(String rawZpuNumbers) {
        String normalized = rawZpuNumbers.trim();
        if (normalized.matches("\\d+\\.\\d+")) {
            normalized = normalized.replace('.', ',');
        }
        return List.of(normalized.split(","));
    }

    private Map<String, Object> zpuItem(String containerNumber, String zpu) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("container_number_zpu", containerNumber);
        item.put("zpu", zpu);
        item.put("zpu_type", null);
        return item;
    }

    private Object mapCarriageSort(Object sourceValue) {
        String value = stringValue(sourceValue);
        if (value == null) {
            return null;
        }
        if ("Платформа 80 - футов".equalsIgnoreCase(value)) {
            return "ПЛ для КТК длиной  свыше  25,5";
        }
        if ("Платформа 60 - футов".equalsIgnoreCase(value)) {
            return "ПЛ для КТК длиной  19,62";
        }
        if ("Платформа 40 - футов".equalsIgnoreCase(value)) {
            return "Пл. для КТК, колесн. техн.и конт.-цистер";
        }
        return value;
    }

    private Object mapContainerLength(Object sourceValue) {
        String value = stringValue(sourceValue);
        if (value == null) {
            return null;
        }
        if ("Стандартный 40".equalsIgnoreCase(value)) {
            return "40";
        }
        if ("Стандартный 20".equalsIgnoreCase(value)) {
            return "20";
        }
        return value;
    }

    private Object divideByThousand(Object sourceValue) {
        return divide(sourceValue, 1000);
    }

    private Object divideByHundred(Object sourceValue) {
        return divide(sourceValue, 100);
    }

    private Object divide(Object sourceValue, int divisor) {
        BigDecimal value = decimalValue(sourceValue);
        if (value == null) {
            return null;
        }
        BigDecimal result = value.divide(BigDecimal.valueOf(divisor), 3, RoundingMode.HALF_UP).stripTrailingZeros();
        if (result.scale() <= 0) {
            return result.longValue();
        }
        return result.doubleValue();
    }

    private Object value(Object sourceValue) {
        String value = stringValue(sourceValue);
        return value == null || value.isBlank() ? null : value;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString().trim();
    }

    private String firstString(Object first, Object second) {
        String firstValue = stringValue(first);
        return firstValue == null || firstValue.isBlank() ? stringValue(second) : firstValue;
    }

    private String waybillNumber(Map<String, Object> invoice, Map<String, Object> summary) {
        return firstString(invoice.get("Номер"), summary.get("Номер накладной ЭТРАН"));
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        try {
            return new BigDecimal(value.toString().replace(',', '.').trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Map<String, Object> safeMap(Map<String, Object> value) {
        return value == null ? Map.of() : value;
    }
}
