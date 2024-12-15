package io.mertkaniscan.automation_engine.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class ElevationService {

    @Value("${open.meteo.elevation.url}")
    private String elevationApiUrl;

    private final RestTemplate restTemplate;

    public ElevationService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ElevationResponse getElevation(double latitude, double longitude) {
        String url = UriComponentsBuilder.fromUriString(elevationApiUrl)
                .queryParam("latitude", latitude)
                .queryParam("longitude", longitude)
                .build()
                .toUriString();

        return restTemplate.getForObject(url, ElevationResponse.class);
    }
}