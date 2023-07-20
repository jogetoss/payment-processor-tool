package org.joget.marketplace.paypal.model;

public class PluginProperties {

    private String environment;
    private String clientId;
    private String clientSecret;
    private String formDefId;
    private String saveFormDefId;
    private String currency;
    private String totalAmount;
    private String invoiceNo;
    private String description;
    
    private String appId;
    private String appVersion;

    private String redirectUserviewMenu;
    private String redirectUserviewMenuFormID;

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
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

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
