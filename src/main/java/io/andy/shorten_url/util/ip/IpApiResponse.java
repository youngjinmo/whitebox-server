package io.andy.shorten_url.util.ip;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IpApiResponse {
    // https://ip-api.com/docs/api:json
    private String status;
    private String country;
    private String countryCode;
    private String region;
    private String regionName;
    private String city;
    private String timezone;
    private String isp;
    private String query;
    private String message;
    private String zip;
    private String lat;
    private String lon;
    private String org;
    private String as;
}
