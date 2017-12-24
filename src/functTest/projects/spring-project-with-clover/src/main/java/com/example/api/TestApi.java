package com.example.api;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/test", produces = MediaType.APPLICATION_JSON_VALUE)
public class TestApi {

    @GetMapping("/{id}/hello")
    @PreAuthorize("#id+'' == principal.username")
    public String get(@PathVariable("id") Long id) {
        return "Hello World!";
    }

}
