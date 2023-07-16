package org.joget.marketplace;

import com.google.gson.Gson;
import java.io.IOException;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.marketplace.paypal.model.OrderResponse;
import org.joget.marketplace.paypal.model.PluginProperties;
import org.joget.marketplace.paypal.util.PaymentUtil;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONObject;

public class PayPalPaymentProcessor {

    public void generatePaymentLink(Map properties, AppDefinition appDef, String recordId) {

        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

        String environment = (String) properties.get("environment");
        String clientId = (String) properties.get("clientId");
        String clientSecret = (String) properties.get("clientSecret");
        String formDefId = (String) properties.get("pmtFormDefId");
        //String saveFormDefId = (String) properties.get("saveFormDefId");
        String acknowledge = (String) properties.get("pmtAcknowledge");
        String currency = (String) properties.get("pmtCurrency");
        String amount = (String) properties.get("pmtAmount");
        String invoiceNo = (String) properties.get("pmtInvoiceNo");
        String desciption = (String) properties.get("pmtDesciption");
        String serverUrl = PaymentUtil.getServerUrl();

        String paymentLink = serverUrl + WorkflowUtil.getHttpServletRequest().getContextPath()
                + "/web/json/app/" + appDef.getAppId() + "/" + appDef.getVersion()
                + "/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=generateLink&provider=paypal"
                + "&id=" + recordId + "&formDefId=" + formDefId;

        PluginProperties pluginProperties = new PluginProperties();
        pluginProperties.setEnvironment(environment);
        pluginProperties.setClientId(clientId);
        pluginProperties.setClientSecret(clientSecret);
        pluginProperties.setFormDefId(formDefId);
        pluginProperties.setAcknowledge(acknowledge);
        pluginProperties.setCurrency(currency);
        pluginProperties.setAmount(amount);
        pluginProperties.setInvoiceNo(invoiceNo);
        pluginProperties.setDesciption(desciption);
        pluginProperties.setAppId(appDef.getAppId());
        pluginProperties.setAppVersion(String.valueOf(appDef.getVersion()));

        String pp = generatePluginProperties(pluginProperties);

        FormRowSet rows = new FormRowSet();
        FormRow row = new FormRow();
        row.setId(recordId);
        row.put("payment_link", paymentLink);
        row.put("plugin_properties", pp);
        row.put("status", "PAYER_ACTION_REQUIRED");
        rows.add(row);

        String tableName = appService.getFormTableName(appDef, formDefId);
        appService.storeFormData(formDefId, tableName, rows, recordId);
    }

    public void createPayPalLink(HttpServletRequest request, HttpServletResponse response, String id, String formDefId) throws IOException {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

        PaymentUtil util = new PaymentUtil();
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appVersion = String.valueOf(appDef.getVersion());
        String appId = appDef.getAppId();

        FormRowSet frs = appService.loadFormData(appId, appVersion, formDefId, id);
        FormRow formRow = frs.get(0);
        String pp = (String) formRow.get("plugin_properties");
        Gson gson = new Gson();
        PluginProperties props = gson.fromJson(pp, PluginProperties.class);

        if (id != null && !id.isEmpty()) {
            String status = (String) formRow.get("status");
            if (status != null && !status.isEmpty() && "COMPLETED".equalsIgnoreCase(status)) {
                response.sendRedirect(props.getAcknowledge() + "?id=" + id + "&src=stored");
            } else {
                props.setAppId(appId);
                props.setAppVersion(appVersion);
                String paymentLink = util.generatePaymentLink(props, id, props.getEnvironment());
                response.sendRedirect(paymentLink);
            }
        }
    }

    public void captureOrder(HttpServletResponse response, String id, String formDefId, String token) throws IOException {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        PaymentUtil util = new PaymentUtil();
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appVersion = String.valueOf(appDef.getVersion());
        String appId = appDef.getAppId();
        FormRowSet frs = appService.loadFormData(appId, appVersion, formDefId, id);
        FormRow formRow = frs.get(0);
        String pp = (String) formRow.get("plugin_properties");
        Gson gson = new Gson();
        PluginProperties props = gson.fromJson(pp, PluginProperties.class);
        String clientId = props.getClientId();
        String clientSecret = props.getClientSecret();
        String environment = props.getEnvironment();
        String acknowledgeUrl = props.getAcknowledge();

        String bearerToken = util.generateAccessToken(clientId, clientSecret, environment);
        String responseString = util.captureOrder(token, id, bearerToken, environment);
        JSONObject order = new JSONObject(responseString);

        if (order.has("status")) {
            OrderResponse orderResponse = new OrderResponse();
            orderResponse.setId(id);
            orderResponse.setOrderId(order.getString("id"));
            orderResponse.setStatus(order.getString("status"));
            orderResponse.setPayload(responseString);
            orderResponse.setToken(order.getString("id"));
            util.saveToForm(id, formDefId, orderResponse);
            response.sendRedirect(acknowledgeUrl + "?id=" + id + "&src=gateway");
        }
    }

    public void processCancel(HttpServletResponse response, String id, String formDefId) throws IOException {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        PaymentUtil util = new PaymentUtil();
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appVersion = String.valueOf(appDef.getVersion());
        String appId = appDef.getAppId();
        FormRowSet frs = appService.loadFormData(appId, appVersion, formDefId, id);
        FormRow formRow = frs.get(0);
        String pp = (String) formRow.get("plugin_properties");
        Gson gson = new Gson();
        PluginProperties props = gson.fromJson(pp, PluginProperties.class);
        response.sendRedirect(props.getAcknowledge() + "?id=" + id + "&src=cancelled");
    }

    private String generatePluginProperties(PluginProperties pluginProperties) {
        Gson gson = new Gson();
        String jsonString = gson.toJson(pluginProperties);
        return jsonString;
    }
    
}
