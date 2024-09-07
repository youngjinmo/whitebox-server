package io.andy.shorten_url.util.random;

public class MockRandom implements RandomObject<String>{
    private final String mockData;

    public MockRandom(String mockData) {
        this.mockData = mockData;
    }

    @Override
    public String generate(int length) {
        return mockData;
    }
}
