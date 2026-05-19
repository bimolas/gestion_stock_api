package com.example.demo.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.demo.models.Article;
import com.example.demo.models.StockExit;

public interface StockExitRepository extends JpaRepository<StockExit, Long> {
    List<StockExit> findByArticle(Article article);

    @Query(value = """
            SELECT
                DATE_FORMAT(e.date, '%Y-%m') AS month,
                COALESCE(SUM(e.quantity), 0) AS totalQuantity
            FROM
                stock_exit e
            GROUP BY
                DATE_FORMAT(e.date, '%Y-%m')
            ORDER BY
                month ASC
            """, nativeQuery = true)
    List<Object[]> getStockExitProgress();
}
