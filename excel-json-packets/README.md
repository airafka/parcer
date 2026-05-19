# Excel to JSON packets

Локальное Java/Spring Boot-приложение читает Excel-файл и формирует JSON-пакеты.

## Формат Excel

- Первая строка листа считается строкой заголовков.
- Каждая следующая непустая строка превращается в JSON-объект.
- Название колонки становится ключом JSON.
- Пустые ячейки пропускаются.

## Запуск в Docker

```powershell
docker compose up --build
```

После запуска откройте:

```text
http://localhost:8081
```

На странице можно загрузить `.xlsx` или `.xls`, указать лист и размер пакета.

## Сборка без Docker

```powershell
mvn package
```

После сборки появится исполняемый jar:

```text
target/excel-json-packets-1.0.0.jar
```

## Запуск

```powershell
java -cp target/excel-json-packets-1.0.0.jar local.parser.Main input.xlsx output
```

Дополнительные параметры:

```powershell
java -cp target/excel-json-packets-1.0.0.jar local.parser.Main input.xlsx output "Лист1" 100
```

Где:

- `input.xlsx` - путь к Excel-файлу.
- `output` - папка для JSON-пакетов.
- `"Лист1"` - необязательное имя листа. Если не указано, берется первый лист.
- `100` - необязательный размер пакета. По умолчанию 100 строк в одном JSON-файле.

## Пример результата

Файл `packet-0001.json`:

```json
{
  "packetNumber": 1,
  "itemsCount": 2,
  "items": [
    {
      "id": 1,
      "name": "Example"
    },
    {
      "id": 2,
      "name": "Another"
    }
  ]
}
```
