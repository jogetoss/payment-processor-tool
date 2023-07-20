package org.joget.marketplace.paypal.util;

import com.google.gson.Gson;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentLink;
import com.stripe.model.Price;
import com.stripe.model.Product;
import com.stripe.param.PaymentLinkCreateParams;
import com.stripe.param.PriceCreateParams;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.marketplace.paypal.model.PluginProperties;
import org.joget.marketplace.paypal.model.Amount;
import org.joget.marketplace.paypal.model.ExperienceContext;
import org.joget.marketplace.paypal.model.Order;
import org.joget.marketplace.paypal.model.OrderResponse;
import org.joget.marketplace.paypal.model.PayPalEnvironment;
import org.joget.marketplace.paypal.model.PaymentSource;
import org.joget.marketplace.paypal.model.Paypal;
import org.joget.marketplace.paypal.model.PurchaseUnit;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONObject;

public class PaymentUtil {

    private static final String INTENT = "CAPTURE";

    public Order prepareOrder(String id, PluginProperties properties) {
        Order order = new Order();
        order.setIntent(INTENT);
        PurchaseUnit purchaseUnit = new PurchaseUnit();
        purchaseUnit.setReferenceId(id);
        // purchaseUnit.setInvoiceId(properties.getInvoiceNo());
        // purchaseUnit.setDescription(properties.getDesciption());
        Amount amount = new Amount();
        amount.setCurrencyCode(properties.getCurrency());
        amount.setValue(properties.getTotalAmount());
        purchaseUnit.setAmount(amount);

        List<PurchaseUnit> purchaseUnits = new ArrayList<>();
        purchaseUnits.add(purchaseUnit);
        order.setPurchaseUnits(purchaseUnits);

        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String appId = appDef.getAppId();
        String appVersion = appDef.getVersion().toString();

        String returnUrl = getServerUrl() + WorkflowUtil.getHttpServletRequest().getContextPath()
                + "/web/json/app/" + appId + "/" + appVersion
                + "/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=created&provider=paypal"
                + "&id=" + id + "&formDefId=" + properties.getFormDefId();
        
         String cancelUrl = getServerUrl() + WorkflowUtil.getHttpServletRequest().getContextPath()
                + "/web/json/app/" + appId + "/" + appVersion
                + "/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=cancelled&provider=paypal"
                + "&id=" + id + "&formDefId=" + properties.getFormDefId();

//        String returnUrl = String.format("%s/jw/web/json/app/%s/%s/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=%s&provider=paypal&id=%s&formDefId=%s",
//                getServerUrl(), appId, appVersion, "created", id, properties.getFormDefId());

//        String cancelUrl = String.format("%s/jw/web/json/app/%s/%s/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=%s&id=%s&formDefId=%s",
//                getServerUrl(), appId, appVersion, "cancelled", id, properties.getFormDefId());

        PaymentSource paymentSource = new PaymentSource();
        ExperienceContext ec = new ExperienceContext();
        ec.setPaymentMethodPreference("IMMEDIATE_PAYMENT_REQUIRED");
        ec.setPaymentMethodSelected("PAYPAL");
        ec.setUserAction("PAY_NOW");
        ec.setReturnUrl(returnUrl);
        ec.setCancelUrl(cancelUrl);

        Paypal paypal = new Paypal();
        paypal.setExperienceContext(ec);
        paymentSource.setPaypal(paypal);

        order.setPaymentSource(paymentSource);

        return order;

    }

    public String generateAccessToken(String clientId, String clientSecret, String environment) {
        String bearerToken = null;
        try ( CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String endPoint = PayPalEnvironment.getUrl(environment) + "/v1/oauth2/token";
            HttpPost httpPost = new HttpPost(endPoint);
            String credentials = clientId + ":" + clientSecret;
            String encodedCredentials = java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
            httpPost.setHeader("Authorization", "Basic " + encodedCredentials);
            StringEntity requestBody = new StringEntity("grant_type=client_credentials");
            httpPost.setEntity(requestBody);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            JSONObject jsonResponse = new JSONObject(responseBody);
            //if (!jsonResponse.has("error")) {
            bearerToken = jsonResponse.getString("access_token");
            //}
        } catch (IOException ex) {
            LogUtil.error(this.getClass().getName(), ex, ex.getMessage());
        }
        return bearerToken;
    }

    public String generatePaymentLink(PluginProperties properties, String refId, String environment)  {
        String paymentLink = "";
        PaymentUtil util = new PaymentUtil();
        String accessToken = util.generateAccessToken(properties.getClientId(), properties.getClientSecret(), environment);
        if (accessToken != null && !accessToken.isEmpty()) {

            Order order = util.prepareOrder(refId, properties);
            Gson gson = new Gson();
            String requestBody = gson.toJson(order);

            String url = PayPalEnvironment.getUrl(environment) + "/v2/checkout/orders";

            try ( CloseableHttpClient httpClient = HttpClients.createDefault()) {
                HttpPost httpPost = new HttpPost(url);

                httpPost.setHeader("Content-Type", "application/json");
                httpPost.setHeader("PayPal-Request-Id", refId);
                httpPost.setHeader("Authorization", "Bearer " + accessToken);
                StringEntity entity = new StringEntity(requestBody);
                httpPost.setEntity(entity);
                CloseableHttpResponse response = httpClient.execute(httpPost);
                HttpEntity responseEntity = response.getEntity();
                String responseString = EntityUtils.toString(responseEntity);
                JSONObject res = new JSONObject(responseString);
                JSONArray links = (JSONArray) res.get("links");
                for (int i = 0; i < links.length(); i++) {
                    JSONObject jsonObject = links.getJSONObject(i);
                    String rel = jsonObject.getString("rel");
                    if (rel.equals("payer-action")) {
                        paymentLink = jsonObject.getString("href");
                        break;
                    } else if (rel.equals("information_link")) {
                        LogUtil.info(this.getClass().getName(), "Unable to generate payment link.");
                        break;
                    }
                }
            } catch (IOException ex) {
                LogUtil.error(this.getClass().getName(), ex, ex.getMessage());
            }
        } else {
            // do something
        }
        return paymentLink;
    }

    public String captureOrder(String orderId, String refId, String accessToken, String environment) {
        String url = PayPalEnvironment.getUrl(environment) + "/v2/checkout/orders/" + orderId + "/capture";
        try ( CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("PayPal-Request-Id", refId);
            httpPost.setHeader("Authorization", "Bearer " + accessToken);
            httpPost.setHeader("Content-Type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseString = EntityUtils.toString(responseEntity);
            return responseString;
        } catch (IOException ex) {
            LogUtil.error(this.getClass().getName(), ex, ex.getMessage());
        }
        return null;
    }

    public static String getServerUrl() {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        StringBuffer url = request.getRequestURL();
        URL requestUrl;
        String serverUrl = "";
        try {
            requestUrl = new URL(url.toString());
            serverUrl = requestUrl.getProtocol() + "://" + requestUrl.getHost();
            // Include port if it is present
            int port = requestUrl.getPort();
            if (port != -1) {
                serverUrl += ":" + port;
            }
        } catch (MalformedURLException ex) {
            LogUtil.error("", ex, ex.getMessage());
        }
        return serverUrl;
    }

    public void saveToForm(String primaryKey, String formDefId, OrderResponse orderResponse) {
        AppService appService = (AppService) FormUtil.getApplicationContext().getBean("appService");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String tableName = appService.getFormTableName(appDef, formDefId);
        FormRowSet rows = new FormRowSet();
        FormRow row = new FormRow();
        row.setId(primaryKey);
        row.put("order_id", orderResponse.getOrderId());
        row.put("payment_status", orderResponse.getStatus());
        row.put("update_time", "");
        row.put("payload", orderResponse.getPayload());
        rows.add(row);
        appService.storeFormData(formDefId, tableName, rows, primaryKey);
    }

    public String stripeCreateProducts() throws StripeException {
        Map<String, Object> params = new HashMap<>();
        params.put("name", "Joget Inc.");

        Product product = Product.create(params);

        return product.getId();
    }

    public String stripeCreatePrices(String currency, Long totalAmount, String productId) throws StripeException {
        PriceCreateParams params = PriceCreateParams.builder()
                .setCurrency(currency)
                .setUnitAmount(totalAmount*100)
                .setProduct(productId)
                .build();

        Price price = Price.create(params);

        return price.getId();
    }

    public String stripeCreatePaymentLinks(String priceId, String formDefId, String recordId, String appId, String appVersion) throws StripeException {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        PaymentLinkCreateParams params = PaymentLinkCreateParams.builder()
                .addLineItem(
                        PaymentLinkCreateParams.LineItem.builder()
                                .setPrice(priceId)
                                .setQuantity(1L)
                                .build())
                .setAfterCompletion(
                        PaymentLinkCreateParams.AfterCompletion.builder()
                                .setType(PaymentLinkCreateParams.AfterCompletion.Type.REDIRECT)
                                .setRedirect(
                                        PaymentLinkCreateParams.AfterCompletion.Redirect.builder()
                                                .setUrl(getServerUrl()
                                                        + "/jw/web/json/app/" + appDef.getAppId() + "/"
                                                        + appDef.getVersion().toString()
                                                        + "/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=success&session_id={CHECKOUT_SESSION_ID}&formDefId="
                                                        + formDefId + "&id=" + recordId + "&provider=stripe")
                                                .build())
                                .build())
                .build();

        PaymentLink paymentLink = PaymentLink.create(params);

        return paymentLink.getUrl();
    }

}
