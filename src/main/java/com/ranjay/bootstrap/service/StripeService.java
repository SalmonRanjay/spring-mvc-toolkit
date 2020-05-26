package com.ranjay.bootstrap.service;

import java.util.HashMap;
import java.util.Map;

import com.stripe.Stripe;
import com.stripe.model.Charge;
import com.stripe.model.Coupon;
import com.stripe.model.Customer;
import com.stripe.model.Subscription;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeService {
  
    @Value("${stripe.keys.secret}")
    private String API_SECRET_KEY;

    public StripeService(){
        // no arg constructor
    }

    public String createCustomer(String email, String token){
        String id = null;

        try{
            Stripe.apiKey = API_SECRET_KEY;
            Map<String,Object> customerParams = new HashMap<>();

            // add customer unique id her to track them in your web app
            customerParams.put("description", "customer for "+ email);
            customerParams.put("email", email);

            customerParams.put("source", token); // ^ Obtained with stripe.js
            // Create a new customer 
            Customer customer = Customer.create(customerParams);
            id = customer.getId();
        } catch( Exception ex){
            ex.printStackTrace();
        }
        return id;
    }

    public String createSubscription(String customerId, String plan, String coupon){
        String id = null;

        try{
            Stripe.apiKey = API_SECRET_KEY;
            Map<String,Object> item = new HashMap<>();
            item.put("plan", plan);

            Map<String,Object> items = new HashMap<>();
            item.put("0", item);

            Map<String,Object> params = new HashMap<>();
            params.put("customer", customerId);
            params.put("items", items);

            // add coupon in available
            if(!coupon.isEmpty()){
                params.put("coupon", coupon);
            }

            Subscription sub = Subscription.create(params);
            id = sub.getId();
        } catch (Exception ex){
            ex.printStackTrace();
        }
        return id;
    }

    public boolean cancelSubscription(String subscriptionId){
        boolean status;

        try {
            Stripe.apiKey = API_SECRET_KEY;
            Subscription sub = Subscription.retrieve(subscriptionId);
            sub.cancel();
            status = true;
        } catch (Exception e) {
            e.printStackTrace();
            status = false;
        }
        return status;
    }

    public Coupon retrieveCoupon(String code){
        try {
            Stripe.apiKey = API_SECRET_KEY;
            return Coupon.retrieve(code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String createCharge(String email, String token, int amount){
        String id = null;

        try {
            Stripe.apiKey = API_SECRET_KEY;
            Map<String,Object> chargeParams = new HashMap<>();
            chargeParams.put("amount", amount);
            chargeParams.put("currency", "usd");
            chargeParams.put("description", "charge For " + email);
            chargeParams.put("source", token);

            // create charge
            Charge charge = Charge.create(chargeParams);
            id = charge.getId();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return id;
    }
}