package com.maheshbabu11.hoarder;


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
            PeriodicTable found = repository.findById(1).orElse(null);
            System.out.println("Found element: " + (found != null ? found.getElement() : "Not found"));
        } catch (Exception e) {
            System.err.println("Error accessing database: " + e.getMessage());
        }
    }

}
