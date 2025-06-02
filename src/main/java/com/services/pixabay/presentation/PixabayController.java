package com.services.pixabay.presentation;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.services.pixabay.application.PixabayMusicService;
import com.services.pixabay.application.PixabayVideoService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class PixabayController {

    private final PixabayVideoService pixabayVideoService;
    private final PixabayMusicService pixabayMusicService;
    
    @GetMapping("/video")
    public String video(Model model) {
        pixabayVideoService.addRandomElementToModel(model);
        return "video";
    }

    @GetMapping("/music")
    public String musicPage(Model model) {
        pixabayMusicService.addRandomElementToModel(model);
        return "music";
    }
}
