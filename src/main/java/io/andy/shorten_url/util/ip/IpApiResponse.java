package io.andy.shorten_url.util.ip;

public record IpApiResponse (
    // https://ip-api.com/docs/api:json
    String status,
    String country,
    String countryCode,
    String region,
    String regionName,
    String city,
    String timezone,
    String isp,
    String query,
    String message,
    String zip,
    String lat,
    String lon,
    String org,
    String as
) {}
