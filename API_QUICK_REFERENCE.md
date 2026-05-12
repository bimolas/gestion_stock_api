# Quick API Reference - All Endpoints

## NEW ENDPOINTS (Added with Watchdog/Traceability Implementation)

### 1. Article Movement History (Timeline)
```
GET /Api/Dashboard/GetArticleHistory/{articleId}
```
**Purpose:** Get combined stock entry + exit history for an article, sorted by date (newest first)
**Response:** `List<Object>` with mixed StockEntry and StockExit records
**Example:** `GET /Api/Dashboard/GetArticleHistory/1`

---

## MODIFIED ENDPOINTS (Now include automatic side effects)

### 2. Create Stock Exit
```
POST /Api/StockExit/CreateStockExit
```
**Body:**
```json
{
  "articleId": 1,
  "quantity": 10,
  "date": "2024-05-10",
  "destination": "Warehouse B"
}
```
**Changes:** Now automatically:
- ✓ Calls `StockWatchdogService.checkLevels()` after save
- ✓ Creates LOW_STOCK alert if quantity < 10 (and no OPEN alert exists)
- ✓ Logs "Stock Movement" Message with type "EXIT"

---

### 3. Create Stock Entry
```
POST /Api/StockEntry/CreateStockEntry
```
**Body:**
```json
{
  "articleId": 1,
  "supplierId": 1,
  "quantity": 20,
  "date": "2024-05-10"
}
```
**Changes:** Now automatically:
- ✓ Logs "Stock Movement" Message with type "ENTRY"
- ✓ Still calls existing alert evaluation

---

## UNCHANGED EXISTING ENDPOINTS

### Articles
- `GET /Api/Article/GetAllArticle`
- `GET /Api/Article/GetArticleById/{id}`
- `POST /Api/Article/CreateArticle`
- `PUT /Api/Article/UpdateArticle/{id}`

### Stock Entry (Read Operations)
- `GET /Api/StockEntry/GetAllStockEntries`
- `GET /Api/StockEntry/GetStockEntryById/{id}`
- `GET /Api/StockEntry/GetStockEntriesByArticle/{articleId}`
- `PUT /Api/StockEntry/UpdateStockEntry/{id}` (⚠️ still has old alert call only)

### Stock Exit (Read Operations)
- `GET /Api/StockExit/GetAllStockExit`
- `GET /Api/StockExit/GetStockExitById/{id}`
- `GET /Api/StockExit/GetStockExitByArticle/{id}`
- `PUT /Api/StockExit/UpdateStockExit/{id}` (⚠️ still has old alert call only)

### Alerts
- `GET /Api/Alert/GetOpenAlerts`
- `GET /Api/Alert/GetAlertsByStatus/{status}` (OPEN, ACKNOWLEDGED, RESOLVED)
- `GET /Api/Alert/GetAlertById/{id}`
- `PUT /Api/Alert/AcknowledgeAlert/{id}`
- `PUT /Api/Alert/ResolveAlert/{id}`

### Messages
- `GET /Api/Message/GetAllMessages`
- `GET /Api/Message/getAllMessages`
- `GET /Api/Message/getMessageById/{id}`
- `PUT /Api/Message/MarkAsRead/{id}`
- `GET /Api/Message/CountRead`

### Dashboard (Existing)
- `GET /Api/Dashboard/GetDashboardStats`
- `GET /Api/Dashboard/GetEntriesProgress`
- `GET /Api/Dashboard/GetExitProgress`

---

## Key Implementation Details

### Watchdog Service (`StockWatchdogService`)
- **When called:** After every `StockExit.createStockExit()` save
- **What it does:** Checks if article quantity ≤ 10, creates LOW_STOCK alert if no OPEN alert exists
- **Deduplication:** Uses fingerprint "LOW_STOCK:{articleId}" + checks only OPEN status
- **Failure handling:** Exceptions are caught and logged; stock movement is NOT rolled back

### Notification Service (`NotificationService`)
- **When called:** After `StockEntry.createStockEntry()` AND `StockExit.createStockExit()` save
- **What it does:** Creates a Message with title="Stock Movement" and content containing movement type + article id + quantity
- **Format:** `Movement=ENTRY|EXIT | ArticleId={id} | Quantity={qty}`
- **Failure handling:** Exceptions are caught and logged; stock movement is NOT rolled back

### Analytics Service (`AnalyticsService`)
- **When called:** When `GET /Api/Dashboard/GetArticleHistory/{id}` is invoked
- **What it does:** Fetches StockEntry and StockExit records for article, merges them, sorts by date (descending)
- **Returns:** Raw list containing both types of objects (caller must check type/properties)
- **Readonly:** Does not modify any data

---

## Response Examples

### Stock Exit Response
```json
{
  "id": 5,
  "article": {
    "id": 1,
    "name": "Test Widget",
    "quantity": 7,
    "price": 99.99,
    "category": { "id": 1, "name": "Hardware" },
    "supplier": { "id": 1, "name": "Supplier A" }
  },
  "quantity": 3,
  "date": "2024-05-10T14:30:00",
  "destination": "Warehouse C"
}
```

### Article History Response (Sorted by Date, Newest First)
```json
[
  {
    "id": 5,
    "article": { "id": 1, ... },
    "quantity": 3,
    "date": "2024-05-10T14:30:00",
    "destination": "Warehouse C"  // <-- StockExit field
  },
  {
    "id": 4,
    "article": { "id": 1, ... },
    "quantity": 50,
    "date": "2024-05-10T13:00:00",
    "supplier": { "id": 1, ... }  // <-- StockEntry field
  },
  {
    "id": 3,
    "article": { "id": 1, ... },
    "quantity": 8,
    "date": "2024-05-10T10:00:00",
    "destination": "Warehouse B"  // <-- StockExit field
  }
]
```

### Alert Response (LOW_STOCK)
```json
{
  "id": 42,
  "type": "LOW_STOCK",
  "severity": "HIGH",
  "status": "OPEN",
  "title": "Low stock alert",
  "content": "Article 1 is low on stock. Current quantity: 7",
  "articleId": 1,
  "fingerprint": "LOW_STOCK:1",
  "createdAt": "2024-05-10T14:30:01"
}
```

### Message Response (Stock Movement)
```json
{
  "id": 123,
  "title": "Stock Movement",
  "content": "Movement=EXIT | ArticleId=1 | Quantity=3",
  "isRead": false,
  "createdAt": "2024-05-10T14:30:00"
}
```

---

## Testing Quick Commands

```bash
# Test 1: Create stock exit below threshold
curl -X POST http://localhost:8080/Api/StockExit/CreateStockExit \
  -H "Content-Type: application/json" \
  -d '{"articleId":1,"quantity":90,"date":"2024-05-10","destination":"Test"}'

# Test 2: Check for LOW_STOCK alert
curl http://localhost:8080/Api/Alert/GetOpenAlerts | grep LOW_STOCK

# Test 3: Check messages (should contain Movement=EXIT)
curl http://localhost:8080/Api/Message/GetAllMessages | grep "Stock Movement"

# Test 4: Get article history (NEW)
curl http://localhost:8080/Api/Dashboard/GetArticleHistory/1

# Test 5: Verify stock quantity was updated
curl http://localhost:8080/Api/Article/GetArticleById/1 | grep quantity
```

---

## Troubleshooting

**Problem:** Stock exits work but no LOW_STOCK alerts created
- Check if quantity is actually below 10
- Verify an OPEN LOW_STOCK alert doesn't already exist
- Check logs for watchdog errors

**Problem:** Messages not created
- Check if MessageRepository is working (GET /Api/Message/GetAllMessages)
- Verify NotificationService is injected properly
- Check logs for notification errors

**Problem:** Article history returns empty
- Verify article ID exists
- Verify stock entries/exits exist for that article
- Try: GET /Api/StockEntry/GetStockEntriesByArticle/{id}

**Problem:** Article history not sorted correctly
- Verify StockEntry.date and StockExit.date fields are populated
- Check if any records have null dates

---

## Frontend Integration Notes

1. **Article History List:**
   - Response contains both StockEntry and StockExit objects
   - Use `typeof` or `__proto__.constructor.name` to differentiate
   - StockExit has `destination`, StockEntry has `supplier`
   - All have `date`, `quantity`, `article` fields

2. **Auto-Alerts:**
   - No frontend action needed for watchdog
   - Existing alert list UI will show LOW_STOCK alerts automatically
   - Consider filtering/styling LOW_STOCK alerts differently

3. **Messages:**
   - Traceability messages appear in message list automatically
   - Format: "Movement=ENTRY|EXIT | ArticleId=X | Quantity=Y"
   - Consider showing these in a "Stock Activity" timeline

4. **Backward Compatibility:**
   - All existing endpoints work unchanged
   - New functionality is additive
   - Old code paths still work (just with added side effects)
