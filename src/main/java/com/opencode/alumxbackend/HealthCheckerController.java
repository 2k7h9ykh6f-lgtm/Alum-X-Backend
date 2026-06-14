package com.opencode.alumxbackend;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckerController {

    @GetMapping("/")
    public String healtcheck(){
        return "The backend is runnintg";
    }
}
