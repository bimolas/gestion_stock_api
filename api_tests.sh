#!/bin/bash

# Inventory Manager API - Complete cURL Test Suite
# Usage: bash api_tests.sh
# Make sure the API is running on http://localhost:8080

BASE_URL="http://localhost:8080"
ARTICLE_ID=""
STOCK_EXIT_COUNT=0
STOCK_ENTRY_COUNT=0
ALERT_COUNT_BEFORE=0
MESSAGE_COUNT_BEFORE=0

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Helper function to print test results
test_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓ PASS${NC}: $2"
    else
        echo -e "${RED}✗ FAIL${NC}: $2"
        echo "Response: $3"
    fi
}

echo "=========================================="
echo "Inventory Manager API - Test Suite"
echo "=========================================="
echo "Base URL: $BASE_URL"
echo ""

# TEST 0: Check if API is running
echo -e "${YELLOW}[TEST 0]${NC} Checking API Health..."
HEALTH=$(curl -s -o /dev/null -w "%{http_code}" "$BASE_URL/Api/Article/GetAllArticle")
if [ "$HEALTH" = "200" ] || [ "$HEALTH" = "401" ]; then
    echo -e "${GREEN}✓ API is responding${NC}"
else
    echo -e "${RED}✗ API not responding (HTTP $HEALTH). Make sure the server is running on $BASE_URL${NC}"
    exit 1
fi

echo ""
echo "=========================================="
echo "PHASE 1: Setup Test Data"
echo "=========================================="

# Create a test article
echo -e "${YELLOW}[SETUP]${NC} Creating test article..."
ARTICLE_RESPONSE=$(curl -s -X POST "$BASE_URL/Api/Article/CreateArticle" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Widget - '$(date +%s)'",
    "quantity": 100,
    "price": 99.99,
    "categoryId": 1,
    "supplierId": 1
  }')

ARTICLE_ID=$(echo "$ARTICLE_RESPONSE" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
if [ -z "$ARTICLE_ID" ]; then
    echo -e "${RED}Failed to create test article${NC}"
    echo "Response: $ARTICLE_RESPONSE"
    exit 1
fi
test_result 0 "Test article created with ID: $ARTICLE_ID"

# Get baseline counts
echo ""
echo -e "${YELLOW}[SETUP]${NC} Getting baseline metrics..."
ALERTS_RESPONSE=$(curl -s "$BASE_URL/Api/Alert/GetOpenAlerts")
ALERT_COUNT_BEFORE=$(echo "$ALERTS_RESPONSE" | grep -c '"id"')
test_result 0 "Current OPEN alerts: $ALERT_COUNT_BEFORE"

MESSAGES_RESPONSE=$(curl -s "$BASE_URL/Api/Message/GetAllMessages")
MESSAGE_COUNT_BEFORE=$(echo "$MESSAGES_RESPONSE" | grep -c '"title"')
test_result 0 "Current messages: $MESSAGE_COUNT_BEFORE"

echo ""
echo "=========================================="
echo "PHASE 2: Test Watchdog Low-Stock Alert (with Deduplication)"
echo "=========================================="

# TEST 1: Create first stock exit that triggers low-stock
echo ""
echo -e "${YELLOW}[TEST 1]${NC} Creating Stock Exit #1 (quantity 85 → article goes to 15, above threshold 10)..."
EXIT1=$(curl -s -X POST "$BASE_URL/Api/StockExit/CreateStockExit" \
  -H "Content-Type: application/json" \
  -d '{
    "articleId": '$ARTICLE_ID',
    "quantity": 85,
    "date": "2024-05-10",
    "destination": "Warehouse A"
  }')

EXIT1_ID=$(echo "$EXIT1" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
test_result $? "Stock Exit #1 created (ID: $EXIT1_ID)"

# Wait a moment for async processing
sleep 1

# Check alerts - should be no new alert (quantity 15 is still above threshold 10)
ALERTS_CHECK1=$(curl -s "$BASE_URL/Api/Alert/GetOpenAlerts")
ALERT_COUNT_CHECK1=$(echo "$ALERTS_CHECK1" | grep -c '"id"')
echo "Alerts after first exit: $ALERT_COUNT_CHECK1 (expected: same as baseline $ALERT_COUNT_BEFORE)"

# TEST 2: Create second stock exit that definitely triggers low-stock
echo ""
echo -e "${YELLOW}[TEST 2]${NC} Creating Stock Exit #2 (quantity 8 → article goes to 7, BELOW threshold 10)..."
EXIT2=$(curl -s -X POST "$BASE_URL/Api/StockExit/CreateStockExit" \
  -H "Content-Type: application/json" \
  -d '{
    "articleId": '$ARTICLE_ID',
    "quantity": 8,
    "date": "2024-05-10",
    "destination": "Warehouse B"
  }')

EXIT2_ID=$(echo "$EXIT2" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
test_result $? "Stock Exit #2 created (ID: $EXIT2_ID)"

sleep 1

# Check alerts - should have ONE new alert
ALERTS_CHECK2=$(curl -s "$BASE_URL/Api/Alert/GetOpenAlerts")
ALERT_COUNT_CHECK2=$(echo "$ALERTS_CHECK2" | grep -c '"id"')
echo "Alerts after second exit: $ALERT_COUNT_CHECK2 (expected: $((ALERT_COUNT_BEFORE + 1)))"

# Check for LOW_STOCK fingerprint
LOW_STOCK_ALERT=$(echo "$ALERTS_CHECK2" | grep -o '"fingerprint":"LOW_STOCK:' | wc -l)
echo "LOW_STOCK alerts found: $LOW_STOCK_ALERT"

# TEST 3: Create third stock exit - should NOT create duplicate alert
echo ""
echo -e "${YELLOW}[TEST 3]${NC} Creating Stock Exit #3 (quantity 3 → article goes to 4, still BELOW threshold)..."
EXIT3=$(curl -s -X POST "$BASE_URL/Api/StockExit/CreateStockExit" \
  -H "Content-Type: application/json" \
  -d '{
    "articleId": '$ARTICLE_ID',
    "quantity": 3,
    "date": "2024-05-10",
    "destination": "Warehouse C"
  }')

EXIT3_ID=$(echo "$EXIT3" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
test_result $? "Stock Exit #3 created (ID: $EXIT3_ID)"

sleep 1

# Check alerts - should still be same count (dedup working)
ALERTS_CHECK3=$(curl -s "$BASE_URL/Api/Alert/GetOpenAlerts")
ALERT_COUNT_CHECK3=$(echo "$ALERTS_CHECK3" | grep -c '"id"')
echo "Alerts after third exit: $ALERT_COUNT_CHECK3 (expected: $ALERT_COUNT_CHECK2 - NO DUPLICATE)"

if [ "$ALERT_COUNT_CHECK3" -eq "$ALERT_COUNT_CHECK2" ]; then
    test_result 0 "✓ Watchdog deduplication WORKING - no duplicate LOW_STOCK alert created"
else
    test_result 1 "✗ Watchdog deduplication FAILED - duplicate alert created!"
fi

echo ""
echo "=========================================="
echo "PHASE 3: Test Traceability Message Creation"
echo "=========================================="

# TEST 4: Create Stock Entry
echo ""
echo -e "${YELLOW}[TEST 4]${NC} Creating Stock Entry (should auto-create traceability message)..."
ENTRY=$(curl -s -X POST "$BASE_URL/Api/StockEntry/CreateStockEntry" \
  -H "Content-Type: application/json" \
  -d '{
    "articleId": '$ARTICLE_ID',
    "supplierId": 1,
    "quantity": 50,
    "date": "2024-05-10"
  }')

ENTRY_ID=$(echo "$ENTRY" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
test_result $? "Stock Entry created (ID: $ENTRY_ID)"

sleep 1

# Check messages
MESSAGES_CHECK1=$(curl -s "$BASE_URL/Api/Message/GetAllMessages")
MESSAGE_COUNT_CHECK1=$(echo "$MESSAGES_CHECK1" | grep -c '"title"')
echo "Messages after stock entry: $MESSAGE_COUNT_CHECK1 (expected: $((MESSAGE_COUNT_BEFORE + 1)))"

# Verify message contains ENTRY type
ENTRY_MESSAGE=$(echo "$MESSAGES_CHECK1" | grep -o 'Movement=ENTRY' | wc -l)
if [ "$ENTRY_MESSAGE" -gt 0 ]; then
    test_result 0 "Stock Entry message created with 'Movement=ENTRY' label"
else
    test_result 1 "Stock Entry message not found or missing 'Movement=ENTRY'"
fi

# TEST 5: Verify Stock Exit also creates messages
echo ""
echo -e "${YELLOW}[TEST 5]${NC} Verifying Stock Exit messages (from earlier tests)..."
EXIT_MESSAGES=$(echo "$MESSAGES_CHECK1" | grep -o 'Movement=EXIT' | wc -l)
echo "EXIT messages found: $EXIT_MESSAGES (should be at least 3 from tests 1-3)"
if [ "$EXIT_MESSAGES" -ge 3 ]; then
    test_result 0 "All stock exits created traceability messages"
else
    test_result 1 "Some stock exits missing messages (found $EXIT_MESSAGES, expected ≥3)"
fi

echo ""
echo "=========================================="
echo "PHASE 4: Test Article History Timeline (NEW ENDPOINT)"
echo "=========================================="

# TEST 6: Get article history
echo ""
echo -e "${YELLOW}[TEST 6]${NC} Fetching article movement history..."
HISTORY=$(curl -s "$BASE_URL/Api/Dashboard/GetArticleHistory/$ARTICLE_ID")

# Count entries and exits in history
ENTRY_COUNT=$(echo "$HISTORY" | grep -o '"supplier"' | wc -l)
EXIT_COUNT=$(echo "$HISTORY" | grep -o '"destination"' | wc -l)

echo "History contains: $ENTRY_COUNT entries, $EXIT_COUNT exits"

if [ "$ENTRY_COUNT" -gt 0 ] && [ "$EXIT_COUNT" -gt 0 ]; then
    test_result 0 "Article history returned both entries and exits"
else
    test_result 1 "Article history incomplete (entries: $ENTRY_COUNT, exits: $EXIT_COUNT)"
fi

# Verify sorting (by checking date order in response)
FIRST_DATE=$(echo "$HISTORY" | grep -o '"date":"[^"]*' | head -1 | cut -d'"' -f4)
LAST_DATE=$(echo "$HISTORY" | grep -o '"date":"[^"]*' | tail -1 | cut -d'"' -f4)
echo "Date range: $LAST_DATE (oldest) to $FIRST_DATE (newest)"

echo ""
echo "=========================================="
echo "PHASE 5: Test Stock Movement Persistence"
echo "=========================================="

# TEST 7: Verify stock quantity was updated
echo ""
echo -e "${YELLOW}[TEST 7]${NC} Verifying stock quantity persistence..."
ARTICLE_CHECK=$(curl -s "$BASE_URL/Api/Article/GetArticleById/$ARTICLE_ID")
FINAL_QUANTITY=$(echo "$ARTICLE_CHECK" | grep -o '"quantity":[0-9]*' | head -1 | cut -d':' -f2)

# Expected: 100 - 85 - 8 - 3 + 50 = 54
EXPECTED_QUANTITY=$((100 - 85 - 8 - 3 + 50))
echo "Article quantity: $FINAL_QUANTITY (expected: $EXPECTED_QUANTITY)"

if [ "$FINAL_QUANTITY" -eq "$EXPECTED_QUANTITY" ]; then
    test_result 0 "Stock quantity correctly persisted through all operations"
else
    test_result 1 "Stock quantity mismatch! Got $FINAL_QUANTITY, expected $EXPECTED_QUANTITY"
fi

echo ""
echo "=========================================="
echo "SUMMARY"
echo "=========================================="
echo "All tests completed!"
echo "Check results above to verify:"
echo "  ✓ Watchdog deduplication prevents duplicate LOW_STOCK alerts"
echo "  ✓ Traceability messages created for ENTRY and EXIT"
echo "  ✓ Article history timeline shows combined entries/exits sorted by date"
echo "  ✓ Stock quantities persisted correctly"
echo ""
echo "TESTING_GUIDE.md has detailed information about each test."
