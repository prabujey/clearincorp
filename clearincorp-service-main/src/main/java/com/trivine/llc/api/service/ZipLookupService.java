package com.trivine.llc.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.stripe.model.Address;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import com.trivine.llc.api.dto.response.LocationDTO;
import com.trivine.llc.api.dto.response.ZippopotZipResponseDTO;
import com.trivine.llc.api.dto.PlaceDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@Service
public class ZipLookupService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Gson gson = new Gson();


    public LocationDTO findLocationByZip(String zipCode) {
        // Use UriComponentsBuilder without fromHttpUrl
        URI uri = UriComponentsBuilder
                .newInstance()  // Initialize a new builder instance
                .scheme("https")
                .host("api.zippopotam.us")
                .pathSegment("us", zipCode)
                .build()
                .toUri();

        try {
            ZippopotZipResponseDTO body = restTemplate.getForObject(uri, ZippopotZipResponseDTO.class);
            if (body == null || body.getPlaces() == null || body.getPlaces().isEmpty()) {
                throw new IllegalArgumentException("No location found for the given ZIP code.");
            }
            PlaceDTO place = body.getPlaces().get(0);
            return new LocationDTO(place.getPlaceName(), place.getState(),"USA",place.getPostalCode(), place.getLatitude(), place.getLongitude());
        } catch (HttpClientErrorException.NotFound e) {
            throw new IllegalArgumentException("No location found for the given ZIP code.");
        }
    }

    public LocationDTO getLiveLocation(double latitude, double longitude) {
        // Build URI
        URI uri = UriComponentsBuilder
                .newInstance()
                .scheme("https")
                .host("nominatim.openstreetmap.org")
                .path("/reverse")
                .queryParam("lat", latitude)
                .queryParam("lon", longitude)
                .queryParam("format", "jsonv2")      // cleaner fields
                .queryParam("addressdetails", "1")
                .build()
                .toUri();

        // Headers (Nominatim requires a real UA with contact)
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/json");
        headers.set(HttpHeaders.USER_AGENT, "ClearInCorp/1.0 (support@clearincorp.com)");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> resp =
                    restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

            String body = resp.getBody();
            if (isBlank(body)) {
                throw new IllegalArgumentException("Empty response from geocoder.");
            }

            JsonNode root = objectMapper.readTree(body);

            // Nominatim error payloads often include "error"
            if (root.hasNonNull("error")) {
                throw new IllegalArgumentException("Geocoder error: " + root.path("error").asText());
            }

            JsonNode addr = root.path("address");
            if (addr.isMissingNode() || addr.isNull()) {
                throw new IllegalArgumentException("No address in geocoder response.");
            }

            // Build a JSON that matches Stripe Address fields
            JsonObject stripeAddr = new JsonObject();
            stripeAddr.addProperty("city", firstNonBlank(
                    addr.path("city").asText(null),
                    addr.path("town").asText(null),
                    addr.path("village").asText(null),
                    addr.path("hamlet").asText(null),
                    addr.path("municipality").asText(null),
                    addr.path("county").asText(null)
            ));
            stripeAddr.addProperty("state",       blankToNull(addr.path("state").asText(null)));
            stripeAddr.addProperty("country",     blankToNull(addr.path("country").asText(null)));
            stripeAddr.addProperty("postal_code", blankToNull(addr.path("postcode").asText(null)));

            // Convert to Stripe Address using Gson (Stripe models are Gson-based)
            Address address = gson.fromJson(stripeAddr, Address.class);

            String city     = defaultIfBlank(address.getCity(),       "Unknown City");
            String state    = defaultIfBlank(address.getState(),      "Unknown State");
            String country  = defaultIfBlank(address.getCountry(),    "Unknown Country");
            String zip      = defaultIfBlank(address.getPostalCode(), "Unknown ZipCode");

            // Your 6-field LocationDTO ctor
            return new LocationDTO(
                    city, state, country, zip,
                    String.valueOf(latitude), String.valueOf(longitude)
            );

        } catch (HttpClientErrorException e) {
            log.warn("Nominatim HTTP error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalArgumentException("Error retrieving live location data.");
        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            log.error("Failed to parse/map Nominatim response", e);
            throw new IllegalArgumentException("Failed to parse or map Nominatim response.");
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.trim().isEmpty();
    }

    private static String defaultIfBlank(String v, String def) {
        return isBlank(v) ? def : v;
    }

    private static String blankToNull(String v) {
        return isBlank(v) ? null : v;
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) return v;
        }
        return null;
    }
}