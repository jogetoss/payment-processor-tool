package org.joget.marketplace;

import com.google.gson.Gson;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import java.io.IOException;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.marketplace.paypal.util.PaymentUtil;
import org.joget.marketplace.stripe.model.PluginProperties;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

public class StripePaymentProcessor {

    public void generatePaymentLink(Map properties, AppDefinition appDef, String recordId) {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");

        String formDefId = (String) properties.get("formDefId");
        String apiKey = (String) properties.get("apiKey");
        String productName = (String) properties.get("productName");
        String currency = (String) properties.get("currency");
        String unitAmount = (String) properties.get("unitAmount");
        String quantity = (String) properties.get("quantity");
        String redirectUserviewMenu = (String) properties.get("redirectUserviewMenu");
        String redirectUserviewMenuFormID = (String) properties.get("redirectUserviewMenuFormID");
        String serverUrl = PaymentUtil.getServerUrl();
        String tempPaymentUrl = serverUrl + WorkflowUtil.getHttpServletRequest().getContextPath()
                + "/web/json/app/" + appDef.getAppId() + "/" + appDef.getVersion()
                + "/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=generateLink&provider=stripe";
        tempPaymentUrl = tempPaymentUrl + "&id=" + recordId + "&formDefId=" + formDefId;

        org.joget.marketplace.stripe.model.PluginProperties pp = new org.joget.marketplace.stripe.model.PluginProperties();
        pp.setApiKey(apiKey);
        pp.setProductName(productName);
        pp.setCurrency(currency);
        pp.setUnitAmount(unitAmount);
        pp.setQuantity(quantity);
        pp.setRedirectUserviewMenu(redirectUserviewMenu);
        pp.setRedirectUserviewMenuFormID(redirectUserviewMenuFormID);

        Gson gson = new Gson();
        String jsonPp = gson.toJson(pp);

        // save to db
        FormRowSet set = new FormRowSet();
        FormRow formRow = new FormRow();
        formRow.put("temp_payment_url", tempPaymentUrl);
        formRow.put("plugin_properties", jsonPp);
        set.add(formRow);
        set.add(0, formRow);
        appService.storeFormData(appDef.getId(), appDef.getVersion().toString(), formDefId, set, recordId);
    }

    public void createStripeLink(HttpServletRequest request, HttpServletResponse response, String id, String formDefId) throws IOException {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appVersion = String.valueOf(appDef.getVersion());
        String appId = appDef.getAppId();
        PaymentUtil util = new PaymentUtil();

        FormRow row = new FormRow();
        FormRowSet rowSet = appService.loadFormData(appId, appVersion, formDefId, id);
        if (rowSet != null && !rowSet.isEmpty()) {
            row = rowSet.get(0);
            Gson gson = new Gson();
            PluginProperties pp = gson.fromJson(row.getProperty("plugin_properties"), PluginProperties.class);
            Stripe.apiKey = pp.getApiKey();
            String redirectUserviewMenu = pp.getRedirectUserviewMenu();
            String redirectUserviewMenuFormID = pp.getRedirectUserviewMenuFormID();
            String redirectURL = PaymentUtil.getServerUrl() + "/jw/web/userview/" + appId + "/" + redirectUserviewMenu + "/_/" + redirectUserviewMenuFormID + "?id=" + id;

            String paymentIntentId = row.getProperty("payment_intent_id");
            String status = "";

            if (!(paymentIntentId == null || paymentIntentId.isEmpty())) {
                try {
                    PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                    status = paymentIntent.getStatus();
                } catch (StripeException ex) {
                    LogUtil.error(getClassName(), ex, ex.getMessage());
                    response.sendRedirect(redirectURL + "&src=error");
                    return;
                }
            }

            if ((status == null || status.isEmpty()) && status != "succeeded" || (paymentIntentId == null || paymentIntentId.isEmpty())) {
                String productName = pp.getProductName();
                String currency = pp.getCurrency();
                String unitAmount = pp.getUnitAmount();
                String quantity = pp.getQuantity();

                try {
                    String productId = util.stripeCreateProducts(productName);
                    String priceId = util.stripeCreatePrices(currency, Long.valueOf(unitAmount), productId);
                    String paymentUrl = util.stripeCreatePaymentLinks(priceId, Long.valueOf(quantity), formDefId, id, appId, appVersion);
                    response.sendRedirect(paymentUrl);

                } catch (StripeException ex) {
                    LogUtil.error(getClassName(), ex, ex.getMessage());
                    response.sendRedirect(redirectURL + "&src=error");
                    return;
                }
            } else {
                response.sendRedirect(redirectURL + "&src=stored");
            }

        }
    }

    public void processPayment(HttpServletRequest request, HttpServletResponse response, String id, String formDefId) throws IOException {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        String sessionId = request.getParameter("session_id");

        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appVersion = String.valueOf(appDef.getVersion());
        String appId = appDef.getAppId();

        FormRow row = new FormRow();
        FormRowSet rowSet = appService.loadFormData(appId, appVersion, formDefId, id);

        if (rowSet != null && !rowSet.isEmpty()) {
            row = rowSet.get(0);
            Gson gson = new Gson();
            PluginProperties pp = gson.fromJson(row.getProperty("plugin_properties"), PluginProperties.class);
            
            Stripe.apiKey = pp.getApiKey();
            
            String redirectUserviewMenu = pp.getRedirectUserviewMenu();
            String redirectUserviewMenuFormID = pp.getRedirectUserviewMenuFormID();
            String redirectURL = PaymentUtil.getServerUrl() + "/jw/web/userview/" + appId + "/" + redirectUserviewMenu + "/_/" + redirectUserviewMenuFormID + "?id=" + id;

            try {
                Session session = Session.retrieve(sessionId);
                String paymentIntentId = session.getPaymentIntent();
                PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
                String status = paymentIntent.getStatus();

                String payload = gson.toJson(paymentIntent);
                
                // save to db
                FormRowSet set = new FormRowSet();
                FormRow r1 = new FormRow();
                r1.put("payment_intent_id", paymentIntentId);
                r1.put("payment_intent", payload);
                r1.put("status", status);
                set.add(r1);
                set.add(0, r1);
                appService.storeFormData(appId, appVersion, formDefId, set, id);

                response.sendRedirect(redirectURL + "&src=gateway");
            } catch (StripeException ex) {
                LogUtil.error(getClassName(), ex, ex.getMessage());
                response.sendRedirect(redirectURL + "&src=error");
                return;
            }
        }
    }

    public String getClassName() {
        return this.getClass().getName();
    }

}
