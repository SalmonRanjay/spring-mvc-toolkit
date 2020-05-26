package com.ranjay.bootstrap.web.controller;

import com.ranjay.bootstrap.service.StripeService;
import com.ranjay.bootstrap.web.model.Response;
import com.stripe.model.Coupon;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;



@Controller
public class PaymentController {
    
    @Value("${stripe.keys.public}")
    private String API_PUBLIC_KEY;

    private StripeService stripeService;

    public PaymentController(StripeService stripeService){
        this.stripeService = stripeService;
    }

    @GetMapping(value="/stripe-dash")
    public String homepage() {
        return "views/homepage";
    }

    @GetMapping(value="/subscription")
    public String subscriptionPage(Model model) {
        
        model.addAttribute("stripePublicKey", API_PUBLIC_KEY);
        return "views/subscription";
    }

    @GetMapping(value="/charge")
    public String chargePage(Model model) {
        model.addAttribute("stripePublicKey", API_PUBLIC_KEY);
        return "views/charge";
    }

     /*========== REST APIs for Handling Payments ===================*/

     @PostMapping(value="/create-subscription")
     public @ResponseBody Response createSubscription(String email,String token, String plan, String coupon)     {
         // validate data
         if(token == null || plan.isEmpty()){
             return new Response(false, "Stripe Payment token is missing. Please, try again later.");
         }

         // create customer first
         String customerId = stripeService.createCustomer(email, token);

         if(customerId == null){
             return new Response(false, "An error occured While trying to create customer");
         }

         // create subscription
         String subscriptionId = stripeService.createSubscription(customerId, plan, coupon);
         if(subscriptionId == null){
             return new Response(false, "An error occured while trying to create a subscription.");
         }

         // Ideally you should store customerId and subscriptionId along with customer object here.
         // These values are required to update or cancel the subscription at a later stage.

         return new Response(true, "Success! your Subscription id is " + subscriptionId);
     }
     
    
    @PostMapping("/cancel-subscription")
    @ResponseBody
    public Response cancelSubscription(String subscriptionId){
        boolean status = stripeService.cancelSubscription(subscriptionId);
        if(!status){
            return new Response(false, "Failed to cancel Subscription, Please try Later!.");
        }

        return new Response(true, "Subscription cancelled successfully");
    }

    @PostMapping("/coupon-validator")
    public @ResponseBody
    Response couponValidator(String code) {
        Coupon coupon = stripeService.retrieveCoupon(code);
        if (coupon != null && coupon.getValid()) {
            String details = (coupon.getPercentOff() == null ? "$" + (coupon.getAmountOff() / 100) : coupon.getPercentOff() + "%") +
                    " OFF " + coupon.getDuration();
            return new Response(true, details);
        } else {
            return new Response(false, "This coupon code is not available. This may be because it has expired or has " +
                    "already been applied to your account.");
        }
    }

    @PostMapping("/create-charge")
    public @ResponseBody Response createCharge(String email, String token){
        // validate data
        if(token == null){
            return new Response(false, "Stripe payment token is missing. Please, try again later.");
        }

        // create charge
        String chargeId = stripeService.createCharge(email, token, 999); //$9.99 USD
        if(chargeId == null ){
            return new Response(false, "An error occured while trying to create a charge");
        }

        // you may want to store charge id along with order information

        return new Response(true, "Success! Your charge id is : " + chargeId);
    }
    
}