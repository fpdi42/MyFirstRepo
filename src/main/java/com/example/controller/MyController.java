package com.example.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MyController {

    @PostMapping("/api/data")
    public String receiveData(@RequestBody Data data) {
        // Process the data, for example, print it or save it
        System.out.println("Received data: key=" + data.getKey() + ", value=" + data.getValue());
        return "Data received successfully";
    }
}