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

@Service
public class FactCheckService {

    @Value("${factcheck.api-key}")
    private String apiKey;

    private final HttpClient httpClient =
            HttpClient.newHttpClient();

    public String searchFactChecks(String query)
            throws IOException, InterruptedException {

        String encodedQuery =
                URLEncoder.encode(
                        query,
                        StandardCharsets.UTF_8);

        String url =
                "https://factchecktools.googleapis.com/v1alpha1/claims:search"
                + "?query=" + encodedQuery
                + "&languageCode=en"
                + "&pageSize=5"
                + "&key=" + apiKey;

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

        HttpResponse<String> response =
                httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {

            throw new RuntimeException(
                    "Fact Check API request failed. HTTP status: "
                    + response.statusCode()
                    + "\nResponse: "
                    + response.body());
        }

        return response.body();
    }
}