package com.project.DuAnTotNghiep.controller.admin;

import com.project.DuAnTotNghiep.config.ConfigVNPay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Controller
public class VnPayAdminController {
    private static final Logger logger = LoggerFactory.getLogger(VnPayAdminController.class);

    @GetMapping("/admin/vnpay-check")
    public String checkPage(Model model) {
        // Render a simple page; frontend performs an AJAX call for the actual check
        model.addAttribute("tmnCode", ConfigVNPay.vnp_TmnCode);
        model.addAttribute("payUrl", ConfigVNPay.vnp_PayUrl);
        return "admin/vnpay-check";
    }

    // Internal helper: do a basic GET to a URL and return status & part of content
    public static Map<String, Object> httpGetStatus(String urlStr) {
        Map<String, Object> result = new HashMap<>();
        HttpURLConnection con = null;
        try {
            java.net.URI uri = java.net.URI.create(urlStr);
            URL url = uri.toURL();
            con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setConnectTimeout(8000);
            con.setReadTimeout(8000);
            int status = con.getResponseCode();
            result.put("statusCode", status);
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder content = new StringBuilder();
            String line;
            int linesRead = 0;
            while ((line = in.readLine()) != null && linesRead < 200) {
                content.append(line);
                linesRead++;
            }
            in.close();
            result.put("contentSnippet", content.toString().substring(0, Math.min(300, content.length())));
            result.put("ok", true);
        } catch (IOException e) {
            logger.error("HTTP GET failed: {}", e.getMessage());
            result.put("ok", false);
            result.put("error", e.getMessage());
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return result;
    }
}
