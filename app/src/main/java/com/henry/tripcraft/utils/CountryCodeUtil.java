package com.henry.tripcraft.utils;

import java.util.HashMap;
import java.util.Map;

public class CountryCodeUtil {

    private static final Map<String, String> COUNTRY_CODE_MAP = new HashMap<>();
    static {
        COUNTRY_CODE_MAP.put("United States", "US");
        COUNTRY_CODE_MAP.put("United Kingdom", "GB");
        COUNTRY_CODE_MAP.put("Canada", "CA");
        COUNTRY_CODE_MAP.put("Australia", "AU");
        COUNTRY_CODE_MAP.put("Germany", "DE");
        COUNTRY_CODE_MAP.put("France", "FR");
        COUNTRY_CODE_MAP.put("Spain", "ES");
        COUNTRY_CODE_MAP.put("Italy", "IT");
        COUNTRY_CODE_MAP.put("Japan", "JP");
        COUNTRY_CODE_MAP.put("China", "CN");
        COUNTRY_CODE_MAP.put("India", "IN");
        COUNTRY_CODE_MAP.put("Brazil", "BR");
        COUNTRY_CODE_MAP.put("Russia", "RU");
        COUNTRY_CODE_MAP.put("Mexico", "MX");
        COUNTRY_CODE_MAP.put("Netherlands", "NL");
        COUNTRY_CODE_MAP.put("Belgium", "BE");
        COUNTRY_CODE_MAP.put("Switzerland", "CH");
        COUNTRY_CODE_MAP.put("Austria", "AT");
        COUNTRY_CODE_MAP.put("Sweden", "SE");
        COUNTRY_CODE_MAP.put("Norway", "NO");
        COUNTRY_CODE_MAP.put("Denmark", "DK");
        COUNTRY_CODE_MAP.put("Finland", "FI");
        COUNTRY_CODE_MAP.put("Poland", "PL");
        COUNTRY_CODE_MAP.put("Czech Republic", "CZ");
        COUNTRY_CODE_MAP.put("Hungary", "HU");
        COUNTRY_CODE_MAP.put("Portugal", "PT");
        COUNTRY_CODE_MAP.put("Greece", "GR");
        COUNTRY_CODE_MAP.put("Turkey", "TR");
        COUNTRY_CODE_MAP.put("South Korea", "KR");
        COUNTRY_CODE_MAP.put("Thailand", "TH");
        COUNTRY_CODE_MAP.put("Singapore", "SG");
        COUNTRY_CODE_MAP.put("Malaysia", "MY");
        COUNTRY_CODE_MAP.put("Indonesia", "ID");
        COUNTRY_CODE_MAP.put("Philippines", "PH");
        COUNTRY_CODE_MAP.put("Vietnam", "VN");
        COUNTRY_CODE_MAP.put("South Africa", "ZA");
        COUNTRY_CODE_MAP.put("Egypt", "EG");
        COUNTRY_CODE_MAP.put("Argentina", "AR");
        COUNTRY_CODE_MAP.put("Chile", "CL");
        COUNTRY_CODE_MAP.put("Colombia", "CO");
        COUNTRY_CODE_MAP.put("Peru", "PE");
        COUNTRY_CODE_MAP.put("Venezuela", "VE");
        COUNTRY_CODE_MAP.put("Israel", "IL");
        COUNTRY_CODE_MAP.put("Saudi Arabia", "SA");
        COUNTRY_CODE_MAP.put("United Arab Emirates", "AE");
        COUNTRY_CODE_MAP.put("New Zealand", "NZ");
        COUNTRY_CODE_MAP.put("Ireland", "IE");
        COUNTRY_CODE_MAP.put("Iceland", "IS");
        COUNTRY_CODE_MAP.put("Luxembourg", "LU");
        COUNTRY_CODE_MAP.put("Monaco", "MC");
        COUNTRY_CODE_MAP.put("San Marino", "SM");
        COUNTRY_CODE_MAP.put("Vatican City", "VA");
        COUNTRY_CODE_MAP.put("Armenia", "AM");
        COUNTRY_CODE_MAP.put("Georgia", "GE");
        COUNTRY_CODE_MAP.put("Azerbaijan", "AZ");
        COUNTRY_CODE_MAP.put("Kazakhstan", "KZ");
        COUNTRY_CODE_MAP.put("Uzbekistan", "UZ");
        COUNTRY_CODE_MAP.put("Ukraine", "UA");
        COUNTRY_CODE_MAP.put("Belarus", "BY");
        COUNTRY_CODE_MAP.put("Moldova", "MD");
        COUNTRY_CODE_MAP.put("Romania", "RO");
        COUNTRY_CODE_MAP.put("Bulgaria", "BG");
        COUNTRY_CODE_MAP.put("Serbia", "RS");
        COUNTRY_CODE_MAP.put("Croatia", "HR");
        COUNTRY_CODE_MAP.put("Slovenia", "SI");
        COUNTRY_CODE_MAP.put("Bosnia and Herzegovina", "BA");
        COUNTRY_CODE_MAP.put("Montenegro", "ME");
        COUNTRY_CODE_MAP.put("North Macedonia", "MK");
        COUNTRY_CODE_MAP.put("Albania", "AL");
        COUNTRY_CODE_MAP.put("Kosovo", "XK");
    }

    public static String getCountryCode(String countryName) {
        return COUNTRY_CODE_MAP.get(countryName);
    }

}

