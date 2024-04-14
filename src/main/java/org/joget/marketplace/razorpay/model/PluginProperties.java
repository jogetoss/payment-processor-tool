package org.joget.marketplace.razorpay.model;

public class PluginProperties {

    private String apiKey;
    private String productName;
    public  String apiSecret;
    private String currency;
    private String totalAmount;
    private String redirectUserviewMenu;
    private String redirectUserviewMenuFormID;
    private String formDefId;
    private String saveFormDefId;
    private String appId;
    private String appVersion;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiSecret() {
        return apiSecret;
    }

    public void setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
    }

    public String getFormDefId() {
        return formDefId;
    }

    public void setFormDefId(String formDefId) {
        this.formDefId = formDefId;
    }

    public String getSaveFormDefId() {
        return saveFormDefId;
    }

    public void setSaveFormDefId(String saveFormDefId) {
        this.saveFormDefId = saveFormDefId;
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
