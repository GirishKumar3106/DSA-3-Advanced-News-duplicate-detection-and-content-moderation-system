package com.advancednews.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.advancednews.model.NewsApiResponse;
import com.advancednews.service.NewsApiService;
import com.advancednews.service.FactCheckService;
import com.advancednews.model.VerificationResult;
import com.advancednews.service.VerificationService;

import org.springframework.web.bind.annotation.CrossOrigin;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsApiService newsApiService;
    private final FactCheckService factCheckService;
    private final VerificationService verificationService;
public NewsController(
        NewsApiService newsApiService,
        FactCheckService factCheckService,
        VerificationService verificationService) {

    this.newsApiService = newsApiService;
    this.factCheckService = factCheckService;
    this.verificationService = verificationService;
}

    @GetMapping("/test")
    public String test() {

        return "Advanced News Intelligence Backend is running!";
    }

    @GetMapping("/search")
    public NewsApiResponse searchNews(
            @RequestParam String q)
            throws IOException, InterruptedException {

        return newsApiService.searchNews(q);
    }

    @GetMapping("/factcheck")
public String factCheck(
        @RequestParam String q)
        throws IOException, InterruptedException {

    return factCheckService.searchFactChecks(q);
}

@GetMapping("/verify")
public VerificationResult verifyNews(
        @RequestParam String q)
        throws Exception {

    return verificationService.verify(q);
}
}