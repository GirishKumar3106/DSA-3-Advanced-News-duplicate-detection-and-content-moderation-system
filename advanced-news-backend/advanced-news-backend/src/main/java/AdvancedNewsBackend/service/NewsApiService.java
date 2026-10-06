package com.advancednews.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.advancednews.model.NewsApiResponse;

import tools.jackson.databind.json.JsonMapper;

@Service
public class NewsApiService {

    @Value("${newsapi.key}")
    private String apiKey;

    @Value("${newsapi.base-url}")
    private String baseUrl;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    private final JsonMapper jsonMapper =
            JsonMapper.builder().build();

    public NewsApiResponse searchNews(String query)
            throws IOException, InterruptedException {

        String encodedQuery =
                URLEncoder.encode(
                        query,
                        StandardCharsets.UTF_8);

        String url =
                baseUrl
                + "/everything?q="
                + encodedQuery
                + "&language=en"
                + "&sortBy=relevancy"
                + "&pageSize=10";

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("X-Api-Key", apiKey)
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {

            throw new RuntimeException(
                    "NewsAPI request failed. HTTP status: "
                    + response.statusCode()
                    + "\nResponse: "
                    + response.body());
        }

        return jsonMapper.readValue(
                response.body(),
                NewsApiResponse.class);
    }
}