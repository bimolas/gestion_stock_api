# Inventory Manager API - Complete Testing Guide

## Environment Setup
**Base URL:** `http://localhost:8080` (adjust port if needed)

## Existing Endpoints (Verify Not Broken)

### Articles
```bash
# Get all articles
GET /Api/Article/GetAllArticle

# Get article by ID
GET /Api/Article/GetArticleById/{id}

# Create article
POST /Api/Article/CreateArticle
Content-Type: application/json
{
  "name": "Sample Article",
  "quantity": 50,
  "price": 100.00,
  "categoryId": 1,
  "supplierId": 1
}

# Update article
PUT /Api/Article/UpdateArticle/{id}
Content-Type: application/json
{
  "name": "Updated Article",
  "quantity": 60,
  "price": 120.00,
  "categoryId": 1,
  "supplierId": 1
}
```

### Stock Entry (BEFORE watchdog changes)
```bash
# Get all stock entries
GET /Api/StockEntry/GetAllStockEntries

# Get stock entry by ID
GET /Api/StockEntry/GetStockEntryById/{id}

# Get stock entries by article
GET /Api/StockEntry/GetStockEntriesByArticle/{articleId}

# Create stock entry (NOW INCLUDES traceability message logging)
POST /Api/StockEntry/CreateStockEntry
Content-Type: application/json
{
  "articleId": 1,
  "supplierId": 1,
  "quantity": 25,
  "date": "2024-05-10"
}
```

### Stock Exit (BEFORE watchdog changes)
```bash
# Get all stock exits
GET /Api/StockExit/GetAllStockExit

# Get stock exit by ID
GET /Api/StockExit/GetStockExitById/{id}

# Get stock exits by article
GET /Api/StockExit/GetStockExitByArticle/{id}

# Create stock exit (NOW INCLUDES watchdog check + traceability message logging)
POST /Api/StockExit/CreateStockExit
Content-Type: application/json
{
  "articleId": 1,
  "quantity": 10,
  "date": "2024-05-10",
  "destination": "Warehouse B"
}
```

### Alerts
```bash
# Get open alerts
GET /Api/Alert/GetOpenAlerts

# Get alerts by status (OPEN, ACKNOWLEDGED, RESOLVED)
GET /Api/Alert/GetAlertsByStatus/{status}

# Get alert by ID
GET /Api/Alert/GetAlertById/{id}

# Acknowledge alert
PUT /Api/Alert/AcknowledgeAlert/{id}

# Resolve alert
PUT /Api/Alert/ResolveAlert/{id}
```

### Messages
```bash
# Get all messages
GET /Api/Message/GetAllMessages

# Get all unread messages
GET /Api/Message/CountRead

# Get message by ID
GET /Api/Message/getMessageById/{id}

# Mark message as read
PUT /Api/Message/MarkAsRead/{id}
```

### Dashboard (existing)
```bash
# Get dashboard statistics
GET /Api/Dashboard/GetDashboardStats

# Get stock entry progress (by month)
GET /Api/Dashboard/GetEntriesProgress

# Get stock exit progress (by month)
GET /Api/Dashboard/GetExitProgress
```

---

## NEW ENDPOINTS (After This Implementation)

### Article Movement History (NEW)
```bash
# Get combined stock entry + exit history for an article, sorted by date descending
GET /Api/Dashboard/GetArticleHistory/{articleId}

Example:
GET /Api/Dashboard/GetArticleHistory/1

Response: List of StockEntry and StockExit objects sorted by date (newest first)
[
  {
    "id": 5,
    "article": { "id": 1, ... },
    "quantity": 10,
    "date": "2024-05-10T14:30:00",
    "destination": "Warehouse B",  // Only in StockExit
    "__typename": "StockExit"
  },
  {
    "id": 3,
    "article": { "id": 1, ... },
    "quantity": 25,
    "date": "2024-05-09T10:15:00",
    "supplier": { "id": 1, ... },   // Only in StockEntry
    "__typename": "StockEntry"
  }
]
```

---

## Component Validation Testing

### Test 1: Watchdog Low-Stock Alert Deduplication

**Objective:** Verify that creating multiple stock exits for the same low-stock article creates only ONE OPEN alert

**Steps:**
1. Create an Article with quantity = 50
   ```bash
   POST /Api/Article/CreateArticle
   { "name": "Test Widget", "quantity": 50, ... }
   ```
   Save the Article ID (let's say ID=1)

2. Create first Stock Exit that drops quantity below threshold (10)
   ```bash
   POST /Api/StockExit/CreateStockExit
   { "articleId": 1, "quantity": 45, "date": "2024-05-10", "destination": "Warehouse" }
   ```
   This will:
   - Update Article quantity: 50 - 45 = 5 (BELOW threshold 10)
   - Call StockWatchdogService.checkLevels(1)
   - Create Alert with fingerprint "LOW_STOCK:1"
   - Create Message with title "Stock Movement"

3. Check alerts - should see ONE OPEN LOW_STOCK alert
   ```bash
   GET /Api/Alert/GetOpenAlerts
   ```
   Should return 1 LOW_STOCK alert with fingerprint "LOW_STOCK:1"

4. Create SECOND Stock Exit for same article
   ```bash
   POST /Api/StockExit/CreateStockExit
   { "articleId": 1, "quantity": 3, "date": "2024-05-10", "destination": "Warehouse" }
   ```
   This will:
   - Update Article quantity: 5 - 3 = 2 (still BELOW threshold)
   - Call StockWatchdogService.checkLevels(1)
   - Check if OPEN alert exists for fingerprint "LOW_STOCK:1" → YES exists
   - DO NOT CREATE duplicate alert

5. Verify still only ONE OPEN alert
   ```bash
   GET /Api/Alert/GetOpenAlerts
   ```
   Should still return ONLY 1 LOW_STOCK alert (same one as before)

**Expected Result:** ✓ PASS if exactly 1 OPEN LOW_STOCK alert exists after 2 stock exits

---

### Test 2: Traceability Message Creation

**Objective:** Verify that every stock entry and stock exit creates a Message record

**Steps:**
1. Get current message count
   ```bash
   GET /Api/Message/GetAllMessages
   ```
   Note the count (let's say 10 messages)

2. Create a Stock Entry
   ```bash
   POST /Api/StockEntry/CreateStockEntry
   { "articleId": 1, "supplierId": 1, "quantity": 20, "date": "2024-05-10" }
   ```
   This will create a Message with:
   - title: "Stock Movement"
   - content: "Movement=ENTRY | ArticleId=1 | Quantity=20"

3. Create a Stock Exit
   ```bash
   POST /Api/StockExit/CreateStockExit
   { "articleId": 1, "quantity": 5, "date": "2024-05-10", "destination": "Warehouse" }
   ```
   This will create a Message with:
   - title: "Stock Movement"
   - content: "Movement=EXIT | ArticleId=1 | Quantity=5"

4. Get all messages and verify new ones exist
   ```bash
   GET /Api/Message/GetAllMessages
   ```
   Should now have 12 messages (2 new ones)

5. Verify message content
   ```bash
   GET /Api/Message/getMessageById/{newMessageId}
   ```
   Should contain article ID and quantity in content

**Expected Result:** ✓ PASS if 2 new messages are created with correct content format

---

### Test 3: Article History Timeline (NEW ENDPOINT)

**Objective:** Verify that GetArticleHistory returns combined entries and exits sorted by date

**Steps:**
1. Use Article ID from previous tests (e.g., ID=1)

2. Call the new endpoint
   ```bash
   GET /Api/Dashboard/GetArticleHistory/1
   ```

3. Verify response contains:
   - Both StockEntry and StockExit records for the article
   - Records sorted by date in descending order (newest first)
   - Each record has proper fields (id, quantity, date, etc.)

4. Manually verify sort order is correct:
   ```bash
   # Example response should look like:
   [
     { "id": 5, "date": "2024-05-10T14:30:00", "quantity": 5, "destination": "Warehouse" },    // StockExit
     { "id": 4, "date": "2024-05-10T10:00:00", "quantity": 20, "supplier": {...} },             // StockEntry
     { "id": 3, "date": "2024-05-09T15:00:00", "quantity": 10, "destination": "Warehouse" }     // StockExit (older)
   ]
   ```

**Expected Result:** ✓ PASS if dates are in descending order (newest first)

---

### Test 4: Stock Movement Persistence Even If Alert Fails

**Objective:** Verify that stock quantity is saved even if watchdog/notification fails

**Steps:**
1. Get current Article quantity
   ```bash
   GET /Api/Article/GetArticleById/1
   ```
   Note quantity (e.g., 2)

2. Create Stock Exit
   ```bash
   POST /Api/StockExit/CreateStockExit
   { "articleId": 1, "quantity": 1, "date": "2024-05-10", "destination": "Test" }
   ```

3. Check Article quantity was updated
   ```bash
   GET /Api/Article/GetArticleById/1
   ```
   Should be 1 (2 - 1 = 1), even if alert creation silently failed

4. Check that stock exit record exists
   ```bash
   GET /Api/StockExit/GetStockExitByArticle/1
   ```
   Should contain the new exit record

**Expected Result:** ✓ PASS if stock quantity is reduced and exit record exists, regardless of alert status

---

### Test 5: Low Stock Alert Auto-Resolution

**Objective:** Verify that when stock quantity increases above threshold, open LOW_STOCK alerts are resolved

**Steps:**
1. Verify we have an OPEN LOW_STOCK alert (from Test 1)
   ```bash
   GET /Api/Alert/GetOpenAlerts
   ```

2. Create Stock Entry to bring quantity above threshold (10)
   ```bash
   POST /Api/StockEntry/CreateStockEntry
   { "articleId": 1, "supplierId": 1, "quantity": 15, "date": "2024-05-10" }
   ```
   This increases quantity from (say 2) to 17 (above threshold 10)

3. Get all LOW_STOCK alerts
   ```bash
   GET /Api/Alert/GetAlertsByStatus/RESOLVED
   ```
   Should see previously OPEN LOW_STOCK alert is now RESOLVED

**Expected Result:** ✓ PASS if LOW_STOCK alert status changed from OPEN to RESOLVED

---

## Common Issues & Troubleshooting

### Issue: "Service not found" or "NullPointerException" on Stock Exit/Entry
**Cause:** One of the new services (StockWatchdogService, NotificationService, AnalyticsService) failed to autowire
**Solution:** 
- Check application logs for injection errors
- Verify @Service annotations are on all new service classes
- Verify constructor parameters match in both service and calling class

### Issue: Stock Exit/Entry creates duplicate messages
**Cause:** Message creation is happening twice (old logic + new logic)
**Solution:** Check if MessageService.save() is being called elsewhere. Current implementation should only call NotificationService.logStockMovement() once.

### Issue: Watchdog alert created multiple times for same article
**Cause:** Deduplication check failed
**Solution:**
- Verify AlertRepository.existsByFingerprintAndStatusIn() is working
- Check database for alerts with fingerprint "LOW_STOCK:{articleId}" and status OPEN
- Ensure only OPEN status is checked (not ACKNOWLEDGED)

### Issue: Article history endpoint returns empty list
**Cause:** Article not found or no stock movements for that article
**Solution:**
- Verify article ID exists: GET /Api/Article/GetArticleById/{id}
- Verify stock entries exist: GET /Api/StockEntry/GetStockEntriesByArticle/{id}
- Verify stock exits exist: GET /Api/StockExit/GetStockExitByArticle/{id}

### Issue: Article history endpoint returns both entries and exits but not sorted
**Cause:** Date sorting issue
**Solution:**
- Check StockEntry.date and StockExit.date are not null
- Verify dates are in format accepted by Java Date comparator
- Check if entries/exits have null dates

---

## Quick Validation Checklist

Run through these 5 tests in order:

- [ ] **Test 1:** Create Stock Exit → Article quantity decreases below 10 → LOW_STOCK alert created
- [ ] **Test 2:** Create 2nd Stock Exit for same article → No duplicate alert created (dedup works)
- [ ] **Test 3:** Create Stock Entry → Message created with "Stock Movement" title and "ENTRY" type
- [ ] **Test 4:** Create Stock Exit → Message created with "Stock Movement" title and "EXIT" type
- [ ] **Test 5:** GET /Api/Dashboard/GetArticleHistory/1 → Returns combined list sorted by date descending

**If all 5 pass:** ✓ Implementation is working correctly

---

## Notes for Front-End Integration

1. **New Endpoint Base Path:** `/Api/Dashboard/GetArticleHistory/{articleId}`
   - Returns: `List<Object>` containing mixed StockEntry and StockExit records
   - Frontend will need to check `instanceof` or inspect object structure to differentiate
   - Optional: Consider consuming `__typename` field if backend populates it

2. **Alert Deduplication is Automatic:**
   - No frontend changes needed
   - Watchdog automatically prevents duplicate LOW_STOCK alerts
   - Existing alert UI should work unchanged

3. **Traceability Messages Auto-Created:**
   - No frontend action required
   - Stock Entry/Exit creation automatically logs messages
   - Messages appear in Message list/inbox
   - Existing Message UI works unchanged

4. **Transaction Safety:**
   - Stock movements are atomic (all-or-nothing)
   - Alerts/messages are side-effects (failure doesn't block stock persistence)
   - Frontend can assume stock quantity updates are always persisted
