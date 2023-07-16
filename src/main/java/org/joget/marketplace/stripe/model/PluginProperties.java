package org.joget.marketplace.stripe.model;

public class PluginProperties {

    private String apiKey;
    private String productName;
    private String currency;
    private String unitAmount;
    private String quantity;
    private String redirectUserviewMenu;
    private String redirectUserviewMenuFormID;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getUnitAmount() {
        return unitAmount;
    }

    public void setUnitAmount(String unitAmount) {
        this.unitAmount = unitAmount;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
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
