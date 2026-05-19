package com.example.demo.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.models.Article;
import com.example.demo.models.StockEntry;
import com.example.demo.models.Supplier;

public interface StockEntryRepository extends JpaRepository<StockEntry, Long> {
    List<StockEntry> findByArticle(Article article);

    List<StockEntry> findBySupplier(Supplier supplier);

    StockEntry findTopByArticleAndSupplierOrderByDateDesc(Article article, Supplier supplier);

    @Query(value = """
            SELECT
                DATE_FORMAT(e.date, '%Y-%m') AS month,
                COALESCE(SUM(e.quantity), 0) AS totalQuantity
            FROM
                stock_entry e
            GROUP BY
                DATE_FORMAT(e.date, '%Y-%m')
            ORDER BY
                month ASC
            """, nativeQuery = true)
    List<Object[]> getStockEntryProgress();
}
