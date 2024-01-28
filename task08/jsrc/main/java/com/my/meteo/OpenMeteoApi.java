package com.my.meteo;

public class OpenMeteoApi {

    public String getLatestWeatherForecast() {
        return "{\"id\": 2643743, \"name\": \"London\", \"cord\": {\"lon\": -0.13, \"lat\": 51.51}, \"main\": {\"temp\": 280.32, \"pressure\": 1012, \"humidity\": 81}, \"wind\": {\"speed\": 4.1, \"deg\": 80}, \"precipitation\": {\"1h\": 0.1}, \"clouds\": {\"all\": 90}}";
    }
}