package org.joget.marketplace.stripe.model;

public class PluginProperties {

    private String apiKey;
    private String currency;
    private String totalAmount;
    private String redirectUserviewMenu;
    private String redirectUserviewMenuFormID;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(String totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getRedirectUserviewMenu() {
        return redirectUserviewMenu;
    }

    public void setRedirectUserviewMenu(String redirectUserviewMenu) {
        this.redirectUserviewMenu = redirectUserviewMenu;
    }

    public String getRedirectUserviewMenuFormID() {
        return redirectUserviewMenuFormID;
    }

    public void setRedirectUserviewMenuFormID(String redirectUserviewMenuFormID) {
        this.redirectUserviewMenuFormID = redirectUserviewMenuFormID;
    }
}
