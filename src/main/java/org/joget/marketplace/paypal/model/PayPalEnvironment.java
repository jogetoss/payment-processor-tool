package org.joget.marketplace.paypal.model;

public enum PayPalEnvironment {
    SANDBOX("https://api-m.sandbox.paypal.com"),
    LIVE("https://api-m.paypal.com"); 

    private final String url;

    PayPalEnvironment(String url) {
        this.url = url;
    }

    public static String getUrl(String environment) {
        if (environment.equalsIgnoreCase("SANDBOX")) {
            return SANDBOX.url;
        } else if (environment.equalsIgnoreCase("LIVE")) {
            return LIVE.url;
        }
        return "Invalid environment";
    }

}
