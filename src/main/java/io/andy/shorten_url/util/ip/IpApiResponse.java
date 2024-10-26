package io.andy.shorten_url.util.ip;

import lombok.*;

// https://ip-api.com/docs/api:json
@Builder
public record IpApiResponse(
        String query,
        String status,
        String country,
        String countryCode,
        String region,
        String regionName,
        String city,
        String zip,
        String lat,
        String lon,
        String timezone,
        String isp,
        String org,
        String as
) { }
