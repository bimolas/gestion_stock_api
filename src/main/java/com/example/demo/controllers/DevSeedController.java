package com.example.demo.controllers;

import com.example.demo.models.*;
import com.example.demo.repositories.*;
import com.example.demo.services.alert.IAlertService;
import com.example.demo.services.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * Development-only endpoint to seed the database with realistic mock data.
 * Accessible via Swagger at POST /Api/Dev/Seed.
 * Safe to call multiple times — skips seeding if data already exists.
 */
@RestController
@RequestMapping("/Api/Dev/")
@Tag(name = "Dev Tools", description = "Development utilities — seed mock data for testing")
public class DevSeedController {

    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final ArticleRepository articleRepository;
    private final StockEntryRepository stockEntryRepository;
    private final StockExitRepository stockExitRepository;
    private final MessageRepository messageRepository;
    private final IAlertService alertService;
    private final NotificationService notificationService;

    public DevSeedController(
            CategoryRepository categoryRepository,
            SupplierRepository supplierRepository,
            ArticleRepository articleRepository,
            StockEntryRepository stockEntryRepository,
            StockExitRepository stockExitRepository,
            MessageRepository messageRepository,
            IAlertService alertService,
            NotificationService notificationService) {
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.articleRepository = articleRepository;
        this.stockEntryRepository = stockEntryRepository;
        this.stockExitRepository = stockExitRepository;
        this.messageRepository = messageRepository;
        this.alertService = alertService;
        this.notificationService = notificationService;
    }

    @PostMapping("Seed")
    @Transactional
    @Operation(
        summary = "Seed mock data",
        description = "Populates the database with realistic categories, suppliers, articles, " +
                      "stock movements, notifications and triggers alerts for low/zero stock articles. " +
                      "Safe to call multiple times — skips if data already exists."
    )
    public ResponseEntity<Map<String, Object>> seed() {

        // Guard: skip if already seeded
        if (articleRepository.count() > 0) {
            return ResponseEntity.ok(Map.of(
                "status", "skipped",
                "reason", "Database already contains articles. Clear the data first to re-seed."
            ));
        }

        // ── 1. Categories ────────────────────────────────────────────────────
        Category electronics  = save(new Category(null, "Electronics",    "Consumer electronics and accessories"));
        Category office       = save(new Category(null, "Office Supplies", "Stationery, paper, and office equipment"));
        Category furniture    = save(new Category(null, "Furniture",       "Desks, chairs, and storage units"));
        Category networking   = save(new Category(null, "Networking",      "Routers, switches, cables, and network gear"));

        // ── 2. Suppliers ─────────────────────────────────────────────────────
        Supplier techNexus   = save(new Supplier(null, "TechNexus Ltd",      "Sarah Jenkins",  "12 Silicon Ave, San Jose, CA",    "+1 408-555-0192"));
        Supplier officeWorld = save(new Supplier(null, "OfficeWorld Co",     "Marcus Dupont",  "88 Commerce Blvd, Chicago, IL",   "+1 312-555-0847"));
        Supplier furniPro    = save(new Supplier(null, "FurniPro Supplies",  "Amina Osei",     "34 Industrial Rd, Detroit, MI",   "+1 313-555-0374"));
        Supplier netGear     = save(new Supplier(null, "NetGear Wholesale",  "Liam Thornton",  "7 Broadband St, Austin, TX",      "+1 512-555-0621"));

        // ── 3. Articles ──────────────────────────────────────────────────────
        // Healthy stock (quantity > 10)
        Article laptop       = save(new Article(null, "Dell XPS 15 Laptop",       "High-performance 15\" laptop with Intel i7",  45, 1299.99, electronics,  techNexus,   "DXP15-2024"));
        Article monitor      = save(new Article(null, "LG 27\" 4K Monitor",        "Ultra-HD IPS display with USB-C",             30, 449.99,  electronics,  techNexus,   "LG27-4K-01"));
        Article keyboard     = save(new Article(null, "Logitech MX Keys",          "Wireless backlit keyboard",                   60, 109.99,  electronics,  techNexus,   "LGT-MXK-BL"));
        Article deskChair    = save(new Article(null, "Ergonomic Office Chair",    "Lumbar support, adjustable armrests",         18, 349.99,  furniture,    furniPro,    "ERG-CHR-001"));
        Article standingDesk = save(new Article(null, "Height-Adjustable Desk",    "Electric sit-stand desk, 140x70cm",           12, 599.99,  furniture,    furniPro,    "HGT-DSK-140"));
        Article printer      = save(new Article(null, "HP LaserJet Pro M404",      "Monochrome laser printer, 40ppm",             22, 279.99,  office,       officeWorld, "HP-LJP-M404"));

        // Low stock (quantity <= 10, > 0) → triggers HIGH alert
        Article mouse        = save(new Article(null, "Logitech MX Master 3",      "Advanced wireless mouse",                      8, 99.99,   electronics,  techNexus,   "LGT-MXM3-BK"));
        Article hdmiCable    = save(new Article(null, "HDMI 2.1 Cable 2m",         "8K certified HDMI cable",                      5, 19.99,   networking,   netGear,     "HDMI21-2M-BK"));
        Article paperRim     = save(new Article(null, "A4 Paper 500 Sheets",       "80gsm white copy paper, 1 ream",               3, 8.99,    office,       officeWorld, "A4-PPR-500"));

        // Zero stock → triggers CRITICAL alert
        Article webcam       = save(new Article(null, "Logitech C920 Webcam",      "Full HD 1080p webcam with stereo mic",         0, 79.99,   electronics,  techNexus,   "LGT-C920-HD"));
        Article switch8port  = save(new Article(null, "TP-Link 8-Port Switch",     "Gigabit unmanaged network switch",             0, 39.99,   networking,   netGear,     "TPL-SW8-GBT"));
        Article toner        = save(new Article(null, "HP 26A Toner Cartridge",    "Black toner for LaserJet Pro M402/M426",       0, 64.99,   office,       officeWorld, "HP-26A-TNR"));

        // ── 4. Stock Entries (spread over last 5 months) ─────────────────────
        Date m0 = daysAgo(5);   // ~5 months ago
        Date m1 = daysAgo(120);
        Date m2 = daysAgo(90);
        Date m3 = daysAgo(60);
        Date m4 = daysAgo(30);
        Date m5 = daysAgo(10);

        // Laptop entries
        createEntry(laptop,       techNexus,   50, m0);
        createEntry(laptop,       techNexus,   20, m2);

        // Monitor entries
        createEntry(monitor,      techNexus,   40, m1);
        createEntry(monitor,      techNexus,   10, m3);

        // Keyboard entries
        createEntry(keyboard,     techNexus,   80, m0);
        createEntry(keyboard,     techNexus,   30, m3);

        // Chair & desk entries
        createEntry(deskChair,    furniPro,    20, m1);
        createEntry(standingDesk, furniPro,    15, m2);

        // Printer entries
        createEntry(printer,      officeWorld, 30, m0);
        createEntry(printer,      officeWorld, 10, m4);

        // Mouse — low stock, last entry was 45 days ago (supply chain delay)
        createEntry(mouse,        techNexus,   20, daysAgo(45));

        // HDMI cable — low stock, last entry was 50 days ago
        createEntry(hdmiCable,    netGear,     30, daysAgo(50));

        // Paper — low stock, last entry was 35 days ago
        createEntry(paperRim,     officeWorld, 50, daysAgo(35));

        // Webcam — zero stock, last entry was 60 days ago (supply chain delay)
        createEntry(webcam,       techNexus,   25, daysAgo(60));

        // Switch — zero stock, no recent entry (will trigger supply chain alert)
        createEntry(switch8port,  netGear,     20, daysAgo(90));

        // Toner — zero stock, last entry was 45 days ago
        createEntry(toner,        officeWorld, 30, daysAgo(45));

        // ── 5. Stock Exits (spread over last 5 months) ───────────────────────
        createExit(laptop,       45,  m1, "Retail Store A - Downtown");
        createExit(laptop,       20,  m3, "Corporate Client - Acme Corp");
        createExit(monitor,      20,  m2, "Retail Store B - Westside");
        createExit(monitor,      10,  m4, "Online Order #ORD-4821");
        createExit(keyboard,     50,  m1, "Retail Store A - Downtown");
        createExit(keyboard,     20,  m4, "Corporate Client - TechStart Inc");
        createExit(deskChair,     2,  m3, "Office Renovation - Floor 3");
        createExit(standingDesk,  3,  m4, "Office Renovation - Floor 3");
        createExit(printer,      18,  m2, "Retail Store C - Northgate");
        createExit(mouse,        12,  m3, "Online Order #ORD-5103");
        createExit(hdmiCable,    25,  m3, "Retail Store A - Downtown");
        createExit(paperRim,     47,  m4, "Office Supplies - Internal Use");
        createExit(webcam,       25,  m4, "Corporate Client - MediaHouse");
        createExit(switch8port,  20,  m3, "IT Department - Network Upgrade");
        createExit(toner,        30,  m4, "Retail Store B - Westside");

        // ── 6. Trigger alert evaluation for all articles ─────────────────────
        // The alert service uses the existing rules (LowStockRule, SupplyChainDelayRule)
        // Only articles at or below threshold will generate alerts
        List<Article> allArticles = articleRepository.findAll();
        for (Article article : allArticles) {
            try {
                alertService.evaluateAndCreateForArticle(article);
            } catch (Exception ignored) {}
        }

        // ── 7. Summary message ───────────────────────────────────────────────
        Message summary = new Message();
        summary.setTitle("Database Seeded");
        summary.setContent("Mock data loaded: 4 categories, 4 suppliers, 12 articles, " +
                           "stock entries & exits across 5 months. " +
                           "Alerts triggered for low/zero stock articles.");
        summary.setRead(false);
        messageRepository.save(summary);

        return ResponseEntity.ok(Map.of(
            "status",     "success",
            "categories", 4,
            "suppliers",  4,
            "articles",   12,
            "entries",    stockEntryRepository.count(),
            "exits",      stockExitRepository.count(),
            "alerts",     "evaluated — check /Api/Alert/GetOpenAlerts",
            "messages",   messageRepository.count()
        ));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Category save(Category c) { return categoryRepository.save(c); }
    private Supplier save(Supplier s) { return supplierRepository.save(s); }
    private Article  save(Article a)  { return articleRepository.save(a); }

    private void createEntry(Article article, Supplier supplier, int qty, Date date) {
        StockEntry entry = new StockEntry();
        entry.setArticle(article);
        entry.setSupplier(supplier);
        entry.setQuantity(qty);
        entry.setDate(date);
        stockEntryRepository.save(entry);
        notificationService.logStockMovement(article.getId(), qty, "ENTRY");
    }

    private void createExit(Article article, int qty, Date date, String destination) {
        StockExit exit = new StockExit();
        exit.setArticle(article);
        exit.setQuantity(qty);
        exit.setDate(date);
        exit.setDestination(destination);
        stockExitRepository.save(exit);
        notificationService.logStockMovement(article.getId(), qty, "EXIT");
    }

    private Date daysAgo(int days) {
        return Date.from(
            LocalDate.now().minusDays(days)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
        );
    }
}
