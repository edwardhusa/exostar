package com.husa.exostar.server.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ExostarController {

    //Simple health check endpoint on root
    @GetMapping("/")
    public String index() {
        return "Health Check YES!";
    }

    //POST a new file with information
    @PostMapping("/upload")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        return "Health Check YES!";
    }

}
