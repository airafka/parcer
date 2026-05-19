const form = document.querySelector("#uploadForm");
const fileInput = document.querySelector("#fileInput");
const dropZone = document.querySelector("#dropZone");
const fileName = document.querySelector("#fileName");
const statusNode = document.querySelector("#status");
const submitButton = document.querySelector("#submitButton");
const summaryGrid = document.querySelector("#summaryGrid");
const waybillsCount = document.querySelector("#waybillsCount");
const containersCount = document.querySelector("#containersCount");
const carriagesCount = document.querySelector("#carriagesCount");
const productsCount = document.querySelector("#productsCount");
const waybillPanel = document.querySelector("#waybillPanel");
const waybillList = document.querySelector("#waybillList");
const resultPanel = document.querySelector("#resultPanel");
const packetList = document.querySelector("#packetList");
const preview = document.querySelector("#preview");
const downloadSelectedButton = document.querySelector("#downloadSelectedButton");
const downloadAllButton = document.querySelector("#downloadAllButton");

let currentPackets = [];
let selectedPacketIndex = 0;

fileInput.addEventListener("change", () => {
    updateFileLabel(fileInput.files[0]);
});

dropZone.addEventListener("dragover", (event) => {
    event.preventDefault();
    dropZone.classList.add("is-dragover");
});

dropZone.addEventListener("dragleave", () => {
    dropZone.classList.remove("is-dragover");
});

dropZone.addEventListener("drop", (event) => {
    event.preventDefault();
    dropZone.classList.remove("is-dragover");
    if (event.dataTransfer.files.length > 0) {
        fileInput.files = event.dataTransfer.files;
        updateFileLabel(fileInput.files[0]);
    }
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!fileInput.files[0]) {
        setStatus("Выберите файл", "is-error");
        return;
    }

    setBusy(true);
    setStatus("Обработка", "is-busy");

    try {
        const formData = new FormData(form);
        const response = await fetch("/api/convert", {
            method: "POST",
            body: formData
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: "Ошибка загрузки файла" }));
            throw new Error(error.message || "Ошибка загрузки файла");
        }

        const result = await response.json();
        renderResult(result);
        setStatus("Готово", "is-done");
    } catch (error) {
        setStatus(error.message, "is-error");
    } finally {
        setBusy(false);
    }
});

downloadAllButton.addEventListener("click", () => {
    currentPackets.forEach((packet) => {
        downloadJson(packet.fileName, packet.content);
    });
});

downloadSelectedButton.addEventListener("click", () => {
    const packet = currentPackets[selectedPacketIndex];
    if (packet) {
        downloadJson(packet.fileName, packet.content);
    }
});

function updateFileLabel(file) {
    fileName.textContent = file ? file.name : "Выберите Excel-файл";
}

function renderResult(result) {
    currentPackets = result.packets || [];
    selectedPacketIndex = 0;
    const summary = calculateSummary(currentPackets);
    waybillsCount.textContent = summary.waybills;
    containersCount.textContent = summary.containers;
    carriagesCount.textContent = summary.carriages;
    productsCount.textContent = summary.products;
    summaryGrid.hidden = false;
    waybillPanel.hidden = false;
    resultPanel.hidden = false;
    renderWaybillList(currentPackets);
    packetList.innerHTML = "";

    currentPackets.forEach((packet, index) => {
        const item = document.createElement("div");
        item.className = "packet-item";

        const previewButton = document.createElement("button");
        previewButton.type = "button";
        previewButton.textContent = packet.fileName;
        previewButton.addEventListener("click", () => {
            showPacket(index);
        });

        const downloadButton = document.createElement("button");
        downloadButton.type = "button";
        downloadButton.className = "packet-download";
        downloadButton.textContent = "Скачать";
        downloadButton.addEventListener("click", () => {
            downloadJson(packet.fileName, packet.content);
        });

        item.append(previewButton, downloadButton);
        packetList.append(item);
    });

    showPacket(0);
}

function renderWaybillList(packets) {
    waybillList.innerHTML = "";
    if (packets.length === 0) {
        const emptyRow = document.createElement("div");
        emptyRow.className = "waybill-row";
        emptyRow.innerHTML = "<span>Нет данных</span><span>0</span><span>0</span><span>0</span>";
        waybillList.append(emptyRow);
        return;
    }

    packets.forEach((packet, index) => {
        const waybill = packet.content?.waybill || {};
        const row = document.createElement("button");
        row.type = "button";
        row.className = "waybill-row";
        row.innerHTML = `
            <span>${escapeHtml(waybill.waybill_number || packet.fileName)}</span>
            <span>${arrayLength(waybill.waybill_railway_carriage)}</span>
            <span>${arrayLength(waybill.waybill_container)}</span>
            <span>${arrayLength(waybill.waybill_product)}</span>
        `;
        row.addEventListener("click", () => {
            showPacket(index);
        });
        waybillList.append(row);
    });
}

function calculateSummary(packets) {
    return packets.reduce((summary, packet) => {
        const waybill = packet.content?.waybill || {};
        summary.waybills += 1;
        summary.containers += arrayLength(waybill.waybill_container);
        summary.carriages += arrayLength(waybill.waybill_railway_carriage);
        summary.products += arrayLength(waybill.waybill_product);
        return summary;
    }, {
        waybills: 0,
        containers: 0,
        carriages: 0,
        products: 0
    });
}

function arrayLength(value) {
    return Array.isArray(value) ? value.length : 0;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}

function showPacket(index) {
    const packet = currentPackets[index];
    if (!packet) {
        preview.textContent = "";
        downloadSelectedButton.disabled = true;
        downloadAllButton.disabled = true;
        return;
    }

    selectedPacketIndex = index;
    downloadSelectedButton.disabled = false;
    downloadAllButton.disabled = currentPackets.length === 0;

    packetList.querySelectorAll(".packet-item > button:first-child").forEach((button, buttonIndex) => {
        button.classList.toggle("is-active", buttonIndex === index);
    });
    waybillList.querySelectorAll(".waybill-row").forEach((row, rowIndex) => {
        row.classList.toggle("is-active", rowIndex === index);
    });
    preview.textContent = JSON.stringify(packet.content, null, 2);
}

function downloadJson(fileName, content) {
    const blob = new Blob([JSON.stringify(content, null, 2)], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    document.body.append(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
}

function setBusy(isBusy) {
    submitButton.disabled = isBusy;
    submitButton.textContent = isBusy ? "Обработка..." : "Сформировать JSON";
}

function setStatus(text, className) {
    statusNode.textContent = text;
    statusNode.className = `status-pill ${className}`;
}
