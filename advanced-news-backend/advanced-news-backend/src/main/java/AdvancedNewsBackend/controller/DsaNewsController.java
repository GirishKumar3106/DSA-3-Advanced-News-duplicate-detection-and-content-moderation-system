package com.advancednews.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.advancednews.model.DsaArticleRequest;
import com.advancednews.model.ManagedNewsArticle;
import com.advancednews.service.DsaNewsService;

@RestController
@RequestMapping("/api/dsa")
public class DsaNewsController {

    private final DsaNewsService dsaNewsService;

    public DsaNewsController(DsaNewsService dsaNewsService) {
        this.dsaNewsService = dsaNewsService;
    }

    @PostMapping("/news/add")
    public Map<String, Object> addNews(
            @RequestBody DsaArticleRequest request) {

        return dsaNewsService.addArticle(request);
    }

    @GetMapping("/news")
    public List<ManagedNewsArticle> getAllNews() {
        return dsaNewsService.getAllArticles();
    }

    @PostMapping("/duplicate")
    public Map<String, Object> checkDuplicate(
            @RequestBody DsaArticleRequest request) {

        return dsaNewsService.checkDuplicate(
                request.getTitle(),
                request.getContent());
    }

    @PostMapping("/moderate/{id}")
    public Map<String, Object> moderate(
            @PathVariable int id) {

        return dsaNewsService.moderateArticle(id);
    }

    @PostMapping("/categorize/{id}")
    public Map<String, Object> categorize(
            @PathVariable int id) {

        return dsaNewsService.categorizeArticle(id);
    }

    @GetMapping("/trending")
    public List<Map<String, Object>> trending() {
        return dsaNewsService.trendingKeywords();
    }

    @GetMapping("/related/{id}")
    public Map<String, Object> related(
            @PathVariable int id) {

        return dsaNewsService.relatedNews(id);
    }
}
