package com.services.pixabay.presentation;

import com.services.common.presentation.dto.BaseResponse;
import com.services.pixabay.application.PixabayMusicService;
import com.services.pixabay.application.PixabayVideoService;
import com.services.pixabay.application.dto.result.PixabayMusicResult;
import com.services.pixabay.application.dto.result.PixabayVideoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PixabayController {

    private final PixabayVideoService pixabayVideoService;
    private final PixabayMusicService pixabayMusicService;

    @GetMapping("")
    public ResponseEntity<BaseResponse<String>> index() {
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, "online"));
    }

    @GetMapping("/video")
    public ResponseEntity<BaseResponse<PixabayVideoResult>> video() {
        PixabayVideoResult data = pixabayVideoService.getRandomElement();
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, data));
    }

    @GetMapping("/music")
    public ResponseEntity<BaseResponse<PixabayMusicResult>> music() {
        PixabayMusicResult data = pixabayMusicService.getRandomElement();
        return ResponseEntity.ok(BaseResponse.of(HttpStatus.OK, data));
    }
}
