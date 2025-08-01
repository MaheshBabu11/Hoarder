package com.maheshbabu11.hoarder;


import com.maheshbabu11.hoarder.core.HoarderCache;
import com.maheshbabu11.hoarder.entity.PeriodicTable;
import com.maheshbabu11.hoarder.entity.PeriodicTableRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class HoarderApplication {
    private final PeriodicTableRepository repository;

    public HoarderApplication(PeriodicTableRepository repository) {
        this.repository = repository;
    }

    public static void main(String[] args) {
        SpringApplication.run(HoarderApplication.class, args);
    }

    @EventListener
    public void onAppReady(ApplicationReadyEvent event) {
        try {
            // First call (DB + Cache)
            long start1 = System.nanoTime();
            PeriodicTable found1 = repository.findById(1).orElse(null);
            PeriodicTable found2 = repository.findById(2).orElse(null);
            PeriodicTable found3 = repository.findById(3).orElse(null);
            PeriodicTable found4 = repository.findById(4).orElse(null);
            PeriodicTable found5 = repository.findById(5).orElse(null);
            long cacheTime = (System.nanoTime() - start1) / 1_000_000;

            HoarderCache.printCacheStatus();
            HoarderCache.clear();
            // Second call (DB only)
            long start2 = System.nanoTime();
            PeriodicTable found6 = repository.findById(1).orElse(null);
            PeriodicTable found7 = repository.findById(2).orElse(null);
            PeriodicTable found8 = repository.findById(3).orElse(null);
            PeriodicTable found9 = repository.findById(4).orElse(null);
            PeriodicTable found10 = repository.findById(5).orElse(null);
            long dbTime = (System.nanoTime() - start2) / 1_000_000;

            System.out.println("Cache time: " + cacheTime + " ms");
            System.out.println("Db time: " + dbTime + " ms");
            System.out.println("Speed improvement: " + (cacheTime > 0 ? String.format("%.2fx", (double)dbTime / cacheTime) : "N/A"));

        } catch (Exception e) {
            System.err.println("Error accessing database: " + e.getMessage());
        }
    }

}
