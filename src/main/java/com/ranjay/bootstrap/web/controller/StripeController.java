package com.ranjay.bootstrap.web.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.ranjay.bootstrap.model.CartItem;
import com.ranjay.bootstrap.model.Product;
import com.ranjay.bootstrap.repository.CartItemRepository;
import com.ranjay.bootstrap.repository.ProductRepository;
import com.ranjay.bootstrap.repository.ShoppingCartRepositor;
import com.ranjay.bootstrap.repository.UserRepository;
import com.ranjay.bootstrap.service.StripeService;
import com.stripe.exception.StripeException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/stripe")
public class StripeController {

    @Value("${stripe.keys.public}")
    private String API_PUBLIC_KEY;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ShoppingCartRepositor shoppingCartRepositor;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StripeService stripeService;

    private static final String REDIRECTSTRIPE = "redirect:/stripe/";

    @RequestMapping(value = "/", method = RequestMethod.GET)
    public String stripe(Model model) throws StripeException {
        List<Product> productList = (List<Product>) productRepository.findAll();
        List<CartItem> cartItemList = (List<CartItem>) cartItemRepository.findAll();

        // Calculating total
        BigDecimal total = new BigDecimal(0);
        for (CartItem cartItem : cartItemList) {
            total = total.add(cartItem.getSubtotal());
        }

        // Creating Model
        // model.addAttribute("client_secret", intent.getClientSecret());
        model.addAttribute("client_secret", API_PUBLIC_KEY);
        model.addAttribute("cartItemList", cartItemList);
        model.addAttribute("productList", productList);
        model.addAttribute("total", total.abs());
        return "views/stripe-cart";
    }

    @RequestMapping(value = "/addToCart", method = RequestMethod.GET)
    public String addToCart(@RequestParam Long id) {
        Product product = productRepository.findById(id).get();

        CartItem cartItem = new CartItem();
        cartItem.setProduct(product);
        cartItem.setQty(1);

        cartItem.setSubtotal(new BigDecimal(product.getPrice()).setScale(2, RoundingMode.HALF_DOWN));
        cartItemRepository.save(cartItem);

        return REDIRECTSTRIPE;
    }

    @RequestMapping(value = "/remove", method = RequestMethod.GET)
    public String remove(@RequestParam Long id) {
        cartItemRepository.deleteById(id);

        return REDIRECTSTRIPE;
    }

    @RequestMapping(value = "/updateCart", method = RequestMethod.POST)
    public String updateCart(HttpServletRequest request) {
        Long id = Long.parseLong(request.getParameter("id"));
        int qty = Integer.parseInt(request.getParameter("qty"));

        CartItem cartItem = cartItemRepository.findById(id).get();
        cartItem.setQty(qty);
        cartItem.setSubtotal(new BigDecimal(cartItem.getProduct().getPrice() * qty).setScale(2, RoundingMode.HALF_UP));

        cartItemRepository.save(cartItem);

        return REDIRECTSTRIPE;
    }

    // @RequestMapping(value = "/checkout", method = RequestMethod.POST)
    @PostMapping("/checkout")
    public String checkoutPay(String email, String token){
        // validate data
        
        // create charge
        String chargeId = stripeService.createCharge(email, token, 999); //$9.99 USD
        // model.addAttribute("checkoutPaySuccess", true);

        return "forward:/stripe/";
    }
}