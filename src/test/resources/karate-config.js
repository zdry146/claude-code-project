function fn() {
    var config = {
        baseUrl: karate.properties['baseUrl'] || 'http://localhost:8083'
    };
    return config;
}
