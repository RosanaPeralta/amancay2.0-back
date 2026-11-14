package com.amancay.infrastructure.adapter.out.shipping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.amancay.application.port.out.ShippingQuotePort;
import com.amancay.domain.exception.ShippingQuoteUnavailableException;
import com.amancay.domain.model.ShippingAddress;
import com.amancay.domain.model.ShippingQuote;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// Servicios gratuitos y sin API key de OpenStreetMap:
// - Nominatim convierte la direccion en coordenadas.
// - OSRM calcula la distancia en auto desde el deposito hasta esas coordenadas.
// Costo = base + km * costo por km.
@Component
class OpenStreetMapShippingQuoteAdapter implements ShippingQuotePort {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    // Nominatim exige un User-Agent que identifique a la aplicacion.
    private static final String USER_AGENT = "amancay-backend (UADE)";

    private final RestClient nominatim;
    private final RestClient osrm;
    private final double originLat;
    private final double originLon;
    private final BigDecimal baseCost;
    private final BigDecimal costPerKm;
    // Sin este filtro Nominatim busca en todo el mundo: "test, test" terminaba en Inglaterra.
    private final String countryCodes;

    OpenStreetMapShippingQuoteAdapter(
            @Value("${amancay.shipping.nominatim-url}") String nominatimUrl,
            @Value("${amancay.shipping.osrm-url}") String osrmUrl,
            @Value("${amancay.shipping.origin.lat}") double originLat,
            @Value("${amancay.shipping.origin.lon}") double originLon,
            @Value("${amancay.shipping.base-cost}") BigDecimal baseCost,
            @Value("${amancay.shipping.cost-per-km}") BigDecimal costPerKm,
            @Value("${amancay.shipping.country-codes}") String countryCodes) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT);
        requestFactory.setReadTimeout(TIMEOUT);
        this.nominatim = RestClient.builder().baseUrl(nominatimUrl).requestFactory(requestFactory)
                .defaultHeader("User-Agent", USER_AGENT).build();
        this.osrm = RestClient.builder().baseUrl(osrmUrl).requestFactory(requestFactory).build();
        this.originLat = originLat;
        this.originLon = originLon;
        this.baseCost = baseCost;
        this.costPerKm = costPerKm;
        this.countryCodes = countryCodes;
    }

    @Override
    public ShippingQuote quote(ShippingAddress destination) {
        try {
            Coordinates to = geocode(destination);
            BigDecimal distanceKm = routeDistanceKm(to);
            BigDecimal cost = baseCost.add(costPerKm.multiply(distanceKm)).setScale(2, RoundingMode.HALF_UP);
            return new ShippingQuote(distanceKm.setScale(1, RoundingMode.HALF_UP), cost);
        } catch (RestClientException exception) {
            throw new ShippingQuoteUnavailableException("Shipping service is unavailable, try again later",
                    exception);
        }
    }

    // Primero busca la direccion exacta; si no la encuentra, se conforma con la localidad.
    private Coordinates geocode(ShippingAddress address) {
        String street = join(" ", address.street(), address.number());
        String locality = join(", ", address.city(), address.province(), address.country());
        return search(join(", ", street, locality))
                .or(() -> search(locality))
                .orElseThrow(() -> new ShippingQuoteUnavailableException(
                        "Could not locate the shipping address: " + join(", ", street, locality)));
    }

    private Optional<Coordinates> search(String query) {
        List<Coordinates> results = nominatim.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("q", query)
                        .queryParam("format", "jsonv2")
                        .queryParam("limit", 1)
                        .queryParam("countrycodes", countryCodes)
                        .build())
                .retrieve()
                .body(new ParameterizedTypeReference<List<Coordinates>>() {
                });
        return results == null ? Optional.empty() : results.stream().findFirst();
    }

    private BigDecimal routeDistanceKm(Coordinates to) {
        // OSRM recibe las coordenadas como lon,lat.
        String path = "/route/v1/driving/" + originLon + "," + originLat + ";" + to.lon() + "," + to.lat();
        OsrmResponse response = osrm.get()
                .uri(uri -> uri.path(path).queryParam("overview", "false").build())
                .retrieve()
                .body(OsrmResponse.class);
        if (response == null || !"Ok".equals(response.code()) || response.routes() == null
                || response.routes().isEmpty()) {
            throw new ShippingQuoteUnavailableException("No route found to the shipping address");
        }
        return BigDecimal.valueOf(response.routes().getFirst().distance())
                .divide(BigDecimal.valueOf(1000), 3, RoundingMode.HALF_UP);
    }

    private static String join(String separator, String... parts) {
        return Stream.of(parts).filter(Objects::nonNull).filter(part -> !part.isBlank())
                .collect(Collectors.joining(separator));
    }

    // Nominatim devuelve lat/lon como texto.
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Coordinates(String lat, String lon) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record OsrmResponse(String code, List<Route> routes) {

        // distance viene en metros.
        @JsonIgnoreProperties(ignoreUnknown = true)
        record Route(double distance) {
        }
    }
}
