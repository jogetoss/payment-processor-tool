package org.joget.marketplace.razorpay.util;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Map;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;
import static org.joget.marketplace.paypal.util.PaymentUtil.getServerUrl;
import org.json.JSONObject;

public class RazorPayUtil {

    private String apiKey;
    private String apiSecret;

    public RazorPayUtil(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    public String createRazorpayOrder(Map<String, Object> orderDetails) throws RazorpayException, IOException, InterruptedException {
        Order order = null;
        try {
            RazorpayClient client = new RazorpayClient(apiKey, apiSecret);
            JSONObject orderRequest = new JSONObject(orderDetails);
           
            String receipt = orderRequest.optString("receipt");
            if (receipt.length() > 40) {
                
                receipt = receipt.substring(0, 40);
                orderRequest.put("receipt", receipt);
            }
            order = client.Orders.create(orderRequest);
           
        } catch (RazorpayException e) {
            LogUtil.error(getClass().getName(), e, "Failed to create Razorpay client or order: " + e.getMessage());
            throw e;
        }
        return order != null ? order.get("id") : null;
    }

    public String createRazorpayPaymentLink(Map<String, Object> linkDetails, String orderId, String formDefId, String recordId) throws IOException, InterruptedException {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();

        String authHeader = Base64.getEncoder().encodeToString((apiKey + ":" + apiSecret).getBytes());
        // Define the callback URL for post-payment redirection
        String callbackUrl = getServerUrl()
                + "/jw/web/json/app/" + appDef.getAppId() + "/"
                + appDef.getVersion().toString()
                + "/plugin/org.joget.marketplace.PaymentProcessorTool/service?action=success"
                + "&provider=razorpay"
                + "&id=" + recordId
                + "&formDefId=" + formDefId;

        // Add the order ID to the link details
        linkDetails.put("reference_id", orderId);
        linkDetails.put("callback_url", callbackUrl);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.razorpay.com/v1/payment_links"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Basic " + authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(new JSONObject(linkDetails).toString()))
                .build();

        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
       
        JSONObject jsonResponse = new JSONObject(response.body());
        String shortUrl = jsonResponse.optString("short_url");

      
        return shortUrl;
    }

}
