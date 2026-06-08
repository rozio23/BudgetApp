package pk.mr.pasir_rozek_mateusz.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class InfoController {

    @GetMapping("/api/info")
    public Map<String, String> getInfo() {
        Map<String, String> response = new HashMap<>();
        response.put("appName", "Aplikacja Budżetowa");
        response.put("version", "1.0");
        response.put("message", "Witaj w aplikacji budżetowej stworzonej ze Spring Boot!");

        return response;
    }
}
